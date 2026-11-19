package com.example.util

import java.util.Calendar
import java.util.Date

/**
 * Jalali (Solar Hijri / Shamsi) calendar conversion and utilities.
 * Handles precise day-of-year calculations, leap years, and Persian formatting.
 */
data class JalaliDate(
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int    // 1 to 31
) : Comparable<JalaliDate> {

    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    override fun toString(): String {
        return String.format("%04d/%02d/%02d", year, month, day)
    }
}

object JalaliCalendar {

    val MONTH_NAMES = listOf(
        "فروردین",
        "اردیبهشت",
        "خرداد",
        "تیر",
        "مرداد",
        "شهریور",
        "مهر",
        "آبان",
        "آذر",
        "دی",
        "بهمن",
        "اسفند"
    )

    val WEEK_DAYS = listOf(
        "شنبه",
        "یکشنبه",
        "دوشنبه",
        "سه‌شنبه",
        "چهارشنبه",
        "پنج‌شنبه",
        "جمعه"
    )

    fun getMonthName(month: Int): String {
        return if (month in 1..12) MONTH_NAMES[month - 1] else ""
    }

    fun isLeapYear(year: Int): Boolean {
        val rem = (year - 474) % 2820
        val mod = (if (rem >= 0) rem else rem + 2820) + 474
        val leapYears = listOf(1, 5, 9, 13, 17, 22, 26, 30)
        val cycle = mod % 33
        return leapYears.contains(cycle)
    }

    fun getDaysInMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    /**
     * Converts current system time to JalaliDate.
     */
    fun today(): JalaliDate {
        val calendar = Calendar.getInstance()
        return gregorianToJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Converts Gregorian date to Jalali date.
     */
    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy = gYear - 1600
        val gm = gMonth - 1
        val gd = gDay - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i + 1]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++ // leap and after Feb
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        var jd = 0
        for (i in 0 until 11) {
            val days = if (i < 6) 31 else 30
            if (jDayNo < days) {
                jm = i + 1
                jd = jDayNo + 1
                break
            }
            jDayNo -= days
        }
        if (jm == 0) {
            jm = 12
            jd = jDayNo + 1
        }

        return JalaliDate(jy, jm, jd)
    }

    /**
     * Converts Jalali date to Gregorian date.
     */
    fun jalaliToGregorian(jYear: Int, jMonth: Int, jDay: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        val jy = jYear - 979
        val jm = jMonth - 1
        val jd = jDay - 1

        var jDayNo = 365 * jy + (jy / 33) * 8 + ((jy % 33 + 3) / 4)
        for (i in 0 until jm) {
            jDayNo += if (i < 6) 31 else 30
        }
        jDayNo += jd

        var gDayNo = jDayNo + 79
        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524
            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        var gm = 0
        var gd = 0
        var i = 0
        while (true) {
            val days = gDaysInMonth[i + 1] + if (i == 1 && leap) 1 else 0
            if (gDayNo < days) {
                gm = i + 1
                gd = gDayNo + 1
                break
            }
            gDayNo -= days
            i++
            if (i >= 12) break
        }

        return Triple(gy, gm, gd)
    }

    /**
     * Converts Jalali date to Unix timestamp (millis).
     */
    fun jalaliToTimestamp(date: JalaliDate): Long {
        val (gy, gm, gd) = jalaliToGregorian(date.year, date.month, date.day)
        val cal = Calendar.getInstance()
        cal.set(gy, gm - 1, gd, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Converts timestamp to JalaliDate.
     */
    fun timestampToJalali(timestamp: Long): JalaliDate {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Parses standard string "YYYY/MM/DD".
     */
    fun parse(str: String): JalaliDate? {
        val clean = CurrencyFormatter.toEnglishDigits(str).trim()
        val parts = clean.split("/", "-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            if (m in 1..12 && d in 1..31) {
                return JalaliDate(y, m, d)
            }
        }
        return null
    }

    /**
     * Formats nicely as e.g. "۱۴۰۳/۰۸/۱۵"
     */
    fun formatShort(date: JalaliDate, persianDigits: Boolean = true): String {
        val formatted = String.format("%04d/%02d/%02d", date.year, date.month, date.day)
        return if (persianDigits) CurrencyFormatter.toPersianDigits(formatted) else formatted
    }

    /**
     * Formats nicely as e.g. "۱۵ آبان ۱۴۰۳"
     */
    fun formatMedium(date: JalaliDate, persianDigits: Boolean = true): String {
        val monthName = getMonthName(date.month)
        val text = "${date.day} $monthName ${date.year}"
        return if (persianDigits) CurrencyFormatter.toPersianDigits(text) else text
    }

    /**
     * Formats with Persian day of week: e.g. "چهارشنبه، ۱۵ آبان ۱۴۰۳"
     */
    fun formatWithDayOfWeek(date: JalaliDate, persianDigits: Boolean = true): String {
        val (gy, gm, gd) = jalaliToGregorian(date.year, date.month, date.day)
        val cal = Calendar.getInstance()
        cal.set(gy, gm - 1, gd)
        // Calendar.DAY_OF_WEEK: Sunday = 1, Saturday = 7
        val dayOfWeekIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
        val dayName = WEEK_DAYS[dayOfWeekIndex]
        val formattedDate = formatMedium(date, persianDigits)
        return "$dayName، $formattedDate"
    }
}
