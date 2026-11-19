package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

/**
 * ذخیره‌سازی پشتیبان با Storage Access Framework.
 * پوشه انتخاب‌شده توسط کاربر به‌صورت پایدار نگهداری می‌شود و فایل قبلی بازنویسی نمی‌شود.
 */
object BackupStorage {
    private const val PREFS = "barbarg_backup_storage"
    private const val KEY_TREE_URI = "backup_tree_uri"

    fun rememberBackupFolder(context: Context, treeUri: Uri, resultFlags: Int) {
        val flags = resultFlags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        context.contentResolver.takePersistableUriPermission(treeUri, flags)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TREE_URI, treeUri.toString()).apply()
    }

    fun savedFolder(context: Context): Uri? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TREE_URI, null)?.let(Uri::parse)

    fun ensureBackupsDirectory(resolver: ContentResolver, selectedTree: Uri): Uri {
        val rootId = DocumentsContract.getTreeDocumentId(selectedTree)
        val root = DocumentsContract.buildDocumentUriUsingTree(selectedTree, rootId)
        val rootName = resolver.query(
            root, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null
        )?.use { if (it.moveToFirst()) it.getString(0) else null }

        if (rootName.equals("Backups", ignoreCase = true)) return root
        val barbarg = findOrCreateDirectory(resolver, root, "Barbarg")
        return findOrCreateDirectory(resolver, barbarg, "Backups")
    }

    private fun findOrCreateDirectory(resolver: ContentResolver, parent: Uri, name: String): Uri {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            parent, DocumentsContract.getDocumentId(parent)
        )
        resolver.query(
            children,
            arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE),
            null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameCol).equals(name, true) &&
                    cursor.getString(mimeCol) == Document.MIME_TYPE_DIR
                ) {
                    return DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(idCol))
                }
            }
        }
        return DocumentsContract.createDocument(
            resolver, parent, Document.MIME_TYPE_DIR, name
        ) ?: error("ساخت پوشه $name ناموفق بود.")
    }

    fun saveJson(context: Context, json: String, requestedFileName: String): Uri {
        val tree = savedFolder(context) ?: error("ابتدا پوشهٔ پشتیبان را انتخاب کنید.")
        val directory = ensureBackupsDirectory(context.contentResolver, tree)
        val name = uniqueName(context.contentResolver, directory, requestedFileName)
        val fileUri = DocumentsContract.createDocument(
            context.contentResolver, directory, "application/json", name
        ) ?: error("ساخت فایل پشتیبان ناموفق بود.")

        try {
            context.contentResolver.openOutputStream(fileUri, "w")!!.use {
                it.write(json.toByteArray(Charsets.UTF_8))
                it.flush()
            }
        } catch (t: Throwable) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, fileUri) }
            throw t
        }
        return fileUri
    }

    private fun uniqueName(resolver: ContentResolver, directory: Uri, requested: String): String {
        val base = requested.substringBeforeLast('.', requested).ifBlank { "barberg_backup" }
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            directory, DocumentsContract.getDocumentId(directory)
        )
        val existing = mutableSetOf<String>()
        resolver.query(children, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { c -> while (c.moveToNext()) existing += c.getString(0) }

        if ("$base.json" !in existing) return "$base.json"
        var index = 1
        while ("${base}_$index.json" in existing) index++
        return "${base}_$index.json"
    }
}
