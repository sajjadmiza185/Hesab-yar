package com.example

import com.example.data.BackupContainer
import com.example.model.Customer
import com.example.model.CustomerAccountSummary
import com.example.model.CustomerGroup
import com.example.model.MonthlyCharge
import com.example.model.PaymentTransaction
import com.example.util.BackupManager
import com.example.util.CsvExporter
import com.example.util.CurrencyFormatter
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

class ExampleUnitTest {

    @Test
    fun testJalaliDateConversion() {
        // Test Gregorian to Jalali conversion: 2024-03-20 is 1403/01/01 (Nowruz)
        val jDate = JalaliCalendar.gregorianToJalali(2024, 3, 20)
        assertEquals(1403, jDate.year)
        assertEquals(1, jDate.month)
        assertEquals(1, jDate.day)

        // Test Jalali to Gregorian conversion back
        val (gy, gm, gd) = JalaliCalendar.jalaliToGregorian(1403, 1, 1)
        assertEquals(2024, gy)
        assertEquals(3, gm)
        assertEquals(20, gd)

        // Month names test
        assertEquals("فروردین", JalaliCalendar.getMonthName(1))
        assertEquals("اسفند", JalaliCalendar.getMonthName(12))
        assertEquals(31, JalaliCalendar.getDaysInMonth(1403, 1))
        assertEquals(30, JalaliCalendar.getDaysInMonth(1403, 7))
    }

    @Test
    fun testCurrencyFormatter() {
        val amount = 12500000L
        val formatted = CurrencyFormatter.formatAmount(amount, "تومان", false)
        assertEquals("12,500,000 تومان", formatted)

        val persianDigits = CurrencyFormatter.toPersianDigits("12,500,000")
        assertEquals("۱۲,۵۰۰,۰۰۰", persianDigits)

        val words = CurrencyFormatter.amountToWordsPersian(250000L, "تومان")
        assertTrue(words.contains("دویست و پنجاه هزار تومان"))
    }

    @Test
    fun testCsvExporterWithBom() {
        val summaries = listOf(
            CustomerAccountSummary(
                customer = Customer(id = 1, name = "فروشگاه نمونه", phone = "09123456789"),
                group = CustomerGroup(id = 1, name = "مسیر ۱"),
                totalCharges = 500000L,
                totalPayments = 200000L,
                totalDiscounts = 0L,
                balance = 300000L,
                monthlyCharges = mapOf(1 to 500000L),
                monthlyPayments = mapOf(1 to 200000L)
            )
        )

        val csv = CsvExporter.generateMonthlyReportCsv(1403, 1, summaries, "تومان")
        assertTrue(csv.startsWith("\uFEFF")) // UTF-8 BOM check for Excel
        assertTrue(csv.contains("فروشگاه نمونه"))
        assertTrue(csv.contains("300000"))
    }

    @Test
    fun testBackupRoundtrip() {
        val original = BackupContainer(
            groups = listOf(CustomerGroup(id = 10, name = "گروه تست")),
            customers = listOf(Customer(id = 100, name = "مشتری آزمایشی", phone = "09120000000")),
            monthlyCharges = listOf(MonthlyCharge(id = 1, customerId = 100, year = 1403, month = 5, amount = 150000)),
            transactions = listOf(PaymentTransaction(id = 1, customerId = 100, amount = 100000, shamsiDate = "1403/05/10"))
        )

        val json = BackupManager.exportToJson(original)
        assertTrue(json.contains("مشتری آزمایشی"))

        val restored = BackupManager.parseFromJson(json)
        assertEquals(1, restored.groups.size)
        assertEquals(1, restored.customers.size)
        assertEquals("مشتری آزمایشی", restored.customers[0].name)
        assertEquals(150000L, restored.monthlyCharges[0].amount)
        assertEquals(100000L, restored.transactions[0].amount)
    }
}
