package com.example.util

import java.security.MessageDigest

object IncomingPaymentSmsParser {
    data class Parsed(
        val amountRial: Long,
        val shamsiDate: String,
        val year: Int,
        val month: Int,
        val tracking: String,
        val dedupeKey: String
    )

    private val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun normalizeDigits(text: String): String = buildString {
        text.forEach { c ->
            append(
                when (c) {
                    in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar()
                    in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar()
                    else -> c
                }
            )
        }
    }

    private fun normalizePersian(text: String): String =
        normalizeDigits(text)
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace('\u200c', ' ')
            .replace('\u200f', ' ')
            .replace('\u200e', ' ')
            .replace('\u202a', ' ')
            .replace('\u202c', ' ')

    fun normalizePhone(input: String): String {
        var s = normalizeDigits(input).filter { it.isDigit() }
        if (s.startsWith("0098")) s = s.drop(4)
        else if (s.startsWith("98") && s.length == 12) s = s.drop(2)
        if (s.length == 10 && s.startsWith("9")) s = "0$s"
        return if (s.length == 11 && s.startsWith("09")) s else ""
    }

    /**
     * Called only after the SMS sender has exactly matched an active customer's phone.
     * Bank names and card-holder names are deliberately not used as identity signals.
     */
    fun isPaymentIntent(body: String): Boolean {
        val text = normalizePersian(body).lowercase()
            .replace(Regex("[\\t\\r\\n]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (text.isBlank()) return false

        // Explicit payment/deposit/transfer language, including informal customer wording.
        val explicitPayment = listOf(
            "واریز", "واريز", "واریز کردم", "واریز شد", "واریز کردم",
            "پرداخت", "پرداخت کردم", "پرداخت شد", "پول زدم", "پول واریز",
            "پول نشست", "برات زدم", "برات واریز", "انتقال وجه",
            "انتقال دادم", "کارت به کارت", "کارت‌به‌کارت",
            "رسید انتقال", "رسید کارت", "رسید پرداخت", "رسید تراکنش",
            "تراکنش موفق", "انتقال موفق", "با موفقیت انتقال",
            "با موفقیت انجام شد", "درخواست موفق", "خدمات شتاب",
            "انتقال وجه شتابی", "مبلغ انتقال", "واریز گردید"
        )
        if (explicitPayment.any(text::contains)) return true

        // Flexible receipt structure: do not depend on bank, app, person or fixed template.
        val hasAmount = Regex("""(?:مبلغ|مبلغ\s+انتقال)\s*[:：]?\s*[0-9][0-9,٬،.]*""")
            .containsMatchIn(text)
        val hasTransferContext = listOf(
            "مبدا", "مبدأ", "مقصد", "کارت", "حساب", "شماره ارجاع",
            "شماره مرجع", "کد پیگیری", "شماره پیگیری", "شماره بازیابی",
            "شماره تراکنش", "انتقال", "واریز", "پرداخت", "شتاب",
            "ریال", "تومان", "موفق"
        ).count(text::contains) >= 2
        if (hasAmount && hasTransferContext) return true

        // A reference identifier alone is not enough; require money/transfer context too.
        val hasReference = listOf(
            "شماره ارجاع", "شماره مرجع", "کد پیگیری",
            "شماره پیگیری", "شماره بازیابی", "شماره تراکنش"
        ).any(text::contains)
        return hasReference && hasAmount
    }

    fun parse(body: String): Parsed? {
        val text = normalizePersian(body)
            .replace(Regex("[\\t\\r\\n]+"), " ")
            .replace(Regex("\\s+"), " ")

        // Accept "مبلغ", "مبلغ انتقال", Persian/Arabic/Latin digits and masked-free values.
        val amountMatch = Regex(
            """(?:مبلغ(?:\s+انتقال)?)\s*[:：]?\s*([0-9][0-9,٬،.]*)\s*(ریال|تومان)?""",
            RegexOption.IGNORE_CASE
        ).find(text) ?: return null

        val rawAmount = amountMatch.groupValues[1].filter(Char::isDigit)
        var amount = rawAmount.toLongOrNull()?.takeIf { it > 0 } ?: return null
        if (amountMatch.groupValues[2] == "تومان") {
            amount = amount * 10L
        }

        var year: Int? = null
        var month: Int? = null
        var day: Int? = null
        var time = ""

        val weekday = """(?:(?:شنبه|یکشنبه|دوشنبه|سه\s*شنبه|چهارشنبه|پنجشنبه|جمعه)[،،\- ]*)?"""
        val namedDate = Regex(
            """$weekday([0-9]{1,2})\s+(فروردین|اردیبهشت|خرداد|تیر|مرداد|شهریور|مهر|آبان|آذر|دی|بهمن|اسفند)\s+([0-9]{4})"""
        ).find(text)
        if (namedDate != null) {
            day = namedDate.groupValues[1].toIntOrNull()
            month = monthNames.indexOf(namedDate.groupValues[2]) + 1
            year = namedDate.groupValues[3].toIntOrNull()
        } else {
            val slashDate = Regex("""(?:تاریخ\s*[:：]?\s*)?([0-9]{4})[/-]([0-9]{1,2})[/-]([0-9]{1,2})""")
                .find(text)
            if (slashDate != null) {
                year = slashDate.groupValues[1].toIntOrNull()
                month = slashDate.groupValues[2].toIntOrNull()
                day = slashDate.groupValues[3].toIntOrNull()
            }
        }

        // Time may occur before or after the date (e.g. ۱۲:۵۰ جمعه، ۱۰ مهر ۱۴۰۵).
        time = Regex("""(?<![0-9])([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)(?![0-9])""")
            .find(text)?.groupValues?.get(1).orEmpty()

        if (year == null || month == null || day == null ||
            month !in 1..12 || day !in 1..31
        ) return null

        // Tracking/reference values can be numeric OR alphanumeric (e.g. e920b7d1d0de).
        val referenceLabel = """(?:شماره\s*(?:پیگیری|تراکنش|ارجاع|مرجع|بازیابی)|کد\s*پیگیری)"""
        val tracking = Regex(
            """$referenceLabel\s*[:：]?\s*([a-zA-Z0-9]+)""",
            RegexOption.IGNORE_CASE
        ).find(text)?.groupValues?.get(1).orEmpty()

        val canonicalDate = "%04d/%02d/%02d".format(year, month, day)
        val sourceKey = "$tracking|$amount|$canonicalDate|$time|${text.hashCode()}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sourceKey.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        return Parsed(amount, canonicalDate, year, month, tracking, digest)
    }
}
