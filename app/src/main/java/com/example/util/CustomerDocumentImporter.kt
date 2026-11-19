package com.example.util

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Parsed customer row shown for confirmation before any database insert. */
data class ImportedCustomer(
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = ""
)

/** Lightweight XLSX/DOCX reader using only Android/JDK APIs (no network or external libraries). */
object CustomerDocumentImporter {
    fun getDisplayName(context: Context, uri: Uri): String {
        var name = uri.lastPathSegment ?: "customers"
        runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0) ?: name
            }
        }
        return name
    }

    fun parse(input: InputStream, fileName: String): List<ImportedCustomer> {
        val entries = linkedMapOf<String, String>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && (entry.name == "xl/sharedStrings.xml" || entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml") || entry.name == "word/document.xml")) {
                    entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
            }
        }
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".xlsx") || entries.keys.any { it.startsWith("xl/worksheets/") } -> parseXlsx(entries)
            lower.endsWith(".docx") || entries.containsKey("word/document.xml") -> parseDocx(entries["word/document.xml"].orEmpty())
            lower.endsWith(".xls") || lower.endsWith(".doc") -> throw IllegalArgumentException("قالب قدیمی XLS و DOC پشتیبانی نمی‌شود؛ فایل را به XLSX یا DOCX تبدیل کن")
            else -> throw IllegalArgumentException("فقط فایل‌های XLSX و DOCX پشتیبانی می‌شوند")
        }
    }

    private fun parseXlsx(entries: Map<String, String>): List<ImportedCustomer> {
        val shared = entries["xl/sharedStrings.xml"].orEmpty()
            .let { Regex("<si\\b[^>]*>(.*?)</si>", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(it).map { m -> xmlText(m.groupValues[1]) }.toList() }
        val sheet = entries.entries.firstOrNull { it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml") }?.value
            ?: throw IllegalArgumentException("برگه‌ای در فایل Excel پیدا نشد")
        val rows = Regex("<row\\b[^>]*>(.*?)</row>", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(sheet).map { rowMatch ->
            Regex("<c\\b([^>]*)>(.*?)</c>", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(rowMatch.groupValues[1]).associate { cell ->
                val attrs = cell.groupValues[1]
                val ref = Regex("\\br=\"([A-Z]+)\\d+\"").find(attrs)?.groupValues?.get(1) ?: "A"
                val type = Regex("\\bt=\"([^\"]+)\"").find(attrs)?.groupValues?.get(1).orEmpty()
                val body = cell.groupValues[2]
                val raw = if (type == "s") {
                    val index = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.get(1)?.toIntOrNull()
                    index?.let { shared.getOrNull(it) }.orEmpty()
                } else if (type == "inlineStr") xmlText(body) else Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL).find(body)?.groupValues?.get(1)?.let(::decodeXml).orEmpty()
                ref to raw.trim()
            }
        }.toList()
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().mapValues { normalizeHeader(it.value) }
        val nameCol = header.entries.firstOrNull { it.value in setOf("name", "customer", "customername", "نام", "ناممشتری", "مشتری") }?.key
            ?: header.entries.firstOrNull { it.value.contains("نام") }?.key
            ?: "A"
        val phoneCol = header.entries.firstOrNull { it.value in setOf("phone", "mobile", "telephone", "شمارهتلفن", "تلفن", "موبایل", "شمارهتماس") }?.key
        val addressCol = header.entries.firstOrNull { it.value in setOf("address", "آدرس", "نشانی") }?.key
        val notesCol = header.entries.firstOrNull { it.value in setOf("notes", "note", "توضیحات", "شرح") }?.key
        return rows.drop(1).mapNotNull { row ->
            val name = row[nameCol].orEmpty().trim()
            if (name.isBlank()) null else ImportedCustomer(name, row[phoneCol].orEmpty(), row[addressCol].orEmpty(), row[notesCol].orEmpty())
        }
    }

    private fun parseDocx(xml: String): List<ImportedCustomer> {
        if (xml.isBlank()) throw IllegalArgumentException("متن سند Word خوانده نشد")
        val tableRows = Regex("<w:tr\\b[^>]*>(.*?)</w:tr>", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(xml).map { row ->
            Regex("<w:tc\\b[^>]*>(.*?)</w:tc>", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(row.groupValues[1]).map { cell -> xmlText(cell.groupValues[1]) }.toList()
        }.filter { it.isNotEmpty() }.toList()
        if (tableRows.size >= 2) {
            val headers = tableRows.first().map(::normalizeHeader)
            val nameIndex = headers.indexOfFirst { it in setOf("name", "customer", "customername", "نام", "ناممشتری", "مشتری") }.let { if (it >= 0) it else headers.indexOfFirst { h -> h.contains("نام") } }
            val phoneIndex = headers.indexOfFirst { it in setOf("phone", "mobile", "telephone", "شمارهتلفن", "تلفن", "موبایل", "شمارهتماس") }
            val addressIndex = headers.indexOfFirst { it in setOf("address", "آدرس", "نشانی") }
            val notesIndex = headers.indexOfFirst { it in setOf("notes", "note", "توضیحات", "شرح") }
            if (nameIndex >= 0) return tableRows.drop(1).mapNotNull { cells ->
                val name = cells.getOrNull(nameIndex).orEmpty().trim()
                if (name.isBlank()) null else ImportedCustomer(name, cells.getOrNull(phoneIndex).orEmpty(), cells.getOrNull(addressIndex).orEmpty(), cells.getOrNull(notesIndex).orEmpty())
            }
        }
        // For plain paragraphs, each non-empty line is treated as a customer name.
        return Regex("<w:p\\b[^>]*>(.*?)</w:p>", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(xml)
            .map { xmlText(it.groupValues[1]).trim() }.filter { it.isNotBlank() }
            .map { ImportedCustomer(name = it) }.toList()
    }

    private fun normalizeHeader(value: String): String = value.lowercase()
        .replace(" ", "").replace("_", "").replace("-", "")
        .replace("ي", "ی").replace("ك", "ک").replace(Regex("[\\u200c\\t\\r\\n]"), "")

    private fun xmlText(xml: String): String = Regex("<w?:?t\\b[^>]*>(.*?)</w?:?t>", setOf(RegexOption.DOT_MATCHES_ALL))
        .findAll(xml).joinToString("") { decodeXml(it.groupValues[1]) }

    private fun decodeXml(s: String): String = s.replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
}
