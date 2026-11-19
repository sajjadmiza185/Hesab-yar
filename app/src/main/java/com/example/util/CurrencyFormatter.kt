package com.example.util

import java.text.DecimalFormat

object CurrencyFormatter {

    private val PERSIAN_DIGITS = charArrayOf('\u06F0', '\u06F1', '\u06F2', '\u06F3', '\u06F4', '\u06F5', '\u06F6', '\u06F7', '\u06F8', '\u06F9')
    private val ARABIC_DIGITS = charArrayOf('\u0660', '\u0661', '\u0662', '\u0663', '\u0664', '\u0665', '\u0666', '\u0667', '\u0668', '\u0669')

    fun toPersianDigits(input: String): String {
        val sb = java.lang.StringBuilder()
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(PERSIAN_DIGITS[ch - '0'])
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = java.lang.StringBuilder()
        for (ch in input) {
            val pIndex = PERSIAN_DIGITS.indexOf(ch)
            if (pIndex != -1) {
                sb.append(('0'.code + pIndex).toChar())
                continue
            }
            val aIndex = ARABIC_DIGITS.indexOf(ch)
            if (aIndex != -1) {
                sb.append(('0'.code + aIndex).toChar())
                continue
            }
            sb.append(ch)
        }
        return sb.toString()
    }

    fun parseAmount(input: String): Long {
        val digitsOnly = toEnglishDigits(input).filter { it.isDigit() }
        return digitsOnly.toLongOrNull() ?: 0L
    }

    fun formatNumber(number: Long, persianDigits: Boolean = true): String {
        val df = DecimalFormat("#,###")
        val formatted = df.format(number)
        return if (persianDigits) toPersianDigits(formatted) else formatted
    }

    fun formatAmount(amount: Long, unit: String = "\u062A\u0648\u0645\u0627\u0646", persianDigits: Boolean = true): String {
        val formattedNumber = formatNumber(amount, persianDigits)
        return "$formattedNumber $unit"
    }

    /**
     * Converts a number to Persian words.
     */
    fun amountToWordsPersian(number: Long, unit: String = "\u062A\u0648\u0645\u0627\u0646"): String {
        if (number == 0L) return "\u0635\u0641\u0631 $unit"
        val isNegative = number < 0
        val n = Math.abs(number)
        val text = convertToWords(n).trim()
        val prefix = if (isNegative) "\u0645\u0646\u0641\u06CC " else ""
        return "$prefix$text $unit"
    }

    private val YEKAN = arrayOf(
        "", "\u06CC\u06A9", "\u062F\u0648", "\u0633\u0647", "\u0686\u0647\u0627\u0631",
        "\u067E\u0646\u062C", "\u0634\u0634", "\u0647\u0641\u062A", "\u0647\u0634\u062A", "\u0646\u0647"
    )

    private val DAHGAN_10 = arrayOf(
        "\u062F\u0647", "\u06CC\u0627\u0632\u062F\u0647", "\u062F\u0648\u0627\u0632\u062F\u0647",
        "\u0633\u06CC\u0632\u062F\u0647", "\u0686\u0647\u0627\u0631\u062F\u0647", "\u067E\u0627\u0646\u0632\u062F\u0647",
        "\u0634\u0627\u0646\u0632\u062F\u0647", "\u0647\u0641\u062F\u0647", "\u0647\u062C\u062F\u0647", "\u0646\u0648\u0632\u062F\u0647"
    )

    private val DAHGAN = arrayOf(
        "", "", "\u0628\u06CC\u0633\u062A", "\u0633\u06CC", "\u0686\u0647\u0644",
        "\u067E\u0646\u062C\u0627\u0647", "\u0634\u0635\u062A", "\u0647\u0641\u062A\u0627\u062F",
        "\u0647\u0634\u062A\u0627\u062F", "\u0646\u0648\u062F"
    )

    private val SADGAN = arrayOf(
        "", "\u06CC\u06A9\u0635\u062F", "\u062F\u0648\u06CC\u0633\u062A", "\u0633\u06CC\u0635\u062F",
        "\u0686\u0647\u0627\u0631\u0635\u062F", "\u067E\u0627\u0646\u0635\u062F", "\u0634\u0634\u0635\u062F",
        "\u0647\u0641\u062A\u0635\u062F", "\u0647\u0634\u062A\u0635\u062F", "\u0646\u0647\u0635\u062F"
    )

    private val SCALE = arrayOf(
        "", "\u0647\u0632\u0627\u0631", "\u0645\u06CC\u0644\u06CC\u0648\u0646",
        "\u0645\u06CC\u0644\u06CC\u0627\u0631\u062F", "\u062A\u0631\u06CC\u0644\u06CC\u0648\u0646"
    )

    private fun convertToWords(number: Long): String {
        if (number == 0L) return ""
        var num = number
        val parts = mutableListOf<String>()
        var scaleIndex = 0

        while (num > 0 && scaleIndex < SCALE.size) {
            val chunk = (num % 1000).toInt()
            if (chunk != 0) {
                val chunkText = convertThreeDigits(chunk)
                val scale = SCALE[scaleIndex]
                val part = if (scale.isNotEmpty()) "$chunkText $scale" else chunkText
                parts.add(0, part)
            }
            num /= 1000
            scaleIndex++
        }

        return parts.joinToString(" \u0648 ")
    }

    private fun convertThreeDigits(num: Int): String {
        val words = mutableListOf<String>()
        val hundreds = num / 100
        val rem = num % 100

        if (hundreds > 0) {
            words.add(SADGAN[hundreds])
        }

        if (rem in 10..19) {
            words.add(DAHGAN_10[rem - 10])
        } else {
            val tens = rem / 10
            val ones = rem % 10
            if (tens > 0) {
                words.add(DAHGAN[tens])
            }
            if (ones > 0) {
                words.add(YEKAN[ones])
            }
        }

        return words.joinToString(" \u0648 ")
    }
}
