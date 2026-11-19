package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.CustomerAccountSummary
import com.example.model.PaymentTransaction
import java.io.File
import java.io.FileOutputStream

object CsvExporter {

    private const val UTF8_BOM = "\uFEFF"

    /**
     * Exports monthly report for a specific year and month.
     */
    fun generateMonthlyReportCsv(
        year: Int,
        month: Int,
        summaries: List<CustomerAccountSummary>,
        currencyUnit: String
    ): String {
        val sb = StringBuilder()
        sb.append(UTF8_BOM)
        val monthName = JalaliCalendar.getMonthName(month)
        sb.append("گزارش ماهانه باربرگ - $monthName سال $year\n")
        sb.append("ردیف,نام مشتری,گروه,تلفن,مبلغ باربرگ ($currencyUnit),پرداختی ماه ($currencyUnit),مانده ماه ($currencyUnit),وضعیت\n")

        summaries.forEachIndexed { index, item ->
            val charge = item.monthlyCharges[month] ?: 0L
            val paid = item.monthlyPayments[month] ?: 0L
            val balance = charge - paid
            val status = when {
                balance > 0 -> "بدهکار"
                balance < 0 -> "بستانکار"
                charge > 0 && balance == 0L -> "تسویه شده"
                else -> "بدون صورتحساب"
            }
            sb.append("${index + 1},")
            sb.append("\"${escape(item.customer.name)}\",")
            sb.append("\"${escape(item.group?.name ?: "عمومی")}\",")
            sb.append("\"${escape(item.customer.phone)}\",")
            sb.append("$charge,")
            sb.append("$paid,")
            sb.append("$balance,")
            sb.append("\"$status\"\n")
        }

        val totalCharge = summaries.sumOf { it.monthlyCharges[month] ?: 0L }
        val totalPaid = summaries.sumOf { it.monthlyPayments[month] ?: 0L }
        val totalBalance = totalCharge - totalPaid
        sb.append("\n,,مجموع کل,$totalCharge,$totalPaid,$totalBalance,\n")

        return sb.toString()
    }

    /**
     * Exports full annual 12-month ledger matrix (فروردین تا اسفند).
     */
    fun generateAnnualMatrixCsv(
        year: Int,
        summaries: List<CustomerAccountSummary>,
        currencyUnit: String
    ): String {
        val sb = StringBuilder()
        sb.append(UTF8_BOM)
        sb.append("ماتریس سالانه صورتحساب باربرگ - سال $year (مبالغ به $currencyUnit)\n")
        sb.append("ردیف,نام مشتری,گروه,فروردین,اردیبهشت,خرداد,تیر,مرداد,شهریور,مهر,آبان,آذر,دی,بهمن,اسفند,مجموع مبالغ,مجموع پرداختی,مانده کل,وضعیت\n")

        summaries.forEachIndexed { index, item ->
            sb.append("${index + 1},")
            sb.append("\"${escape(item.customer.name)}\",")
            sb.append("\"${escape(item.group?.name ?: "عمومی")}\",")
            for (m in 1..12) {
                sb.append("${item.monthlyCharges[m] ?: 0L},")
            }
            val status = if (item.balance > 0) "بدهکار" else if (item.balance < 0) "بستانکار" else "تسویه"
            sb.append("${item.totalCharges},${item.totalPayments},${item.balance},\"$status\"\n")
        }

        // Totals row
        sb.append(",مجموع کل,,")
        for (m in 1..12) {
            val mSum = summaries.sumOf { it.monthlyCharges[m] ?: 0L }
            sb.append("$mSum,")
        }
        val grandCharges = summaries.sumOf { it.totalCharges }
        val grandPayments = summaries.sumOf { it.totalPayments }
        val grandBalance = grandCharges - grandPayments
        sb.append("$grandCharges,$grandPayments,$grandBalance,\n")

        return sb.toString()
    }

    /**
     * Exports list of debtors.
     */
    fun generateDebtorsCsv(
        summaries: List<CustomerAccountSummary>,
        currencyUnit: String
    ): String {
        val sb = StringBuilder()
        sb.append(UTF8_BOM)
        sb.append("گزارش مشتریان بدهکار باربرگ (واحد: $currencyUnit)\n")
        sb.append("ردیف,نام مشتری,گروه,شماره تماس,کل مبالغ,کل پرداختی‌ها,مبلغ بدهی,آدرس\n")

        val debtors = summaries.filter { it.balance > 0 }.sortedByDescending { it.balance }
        debtors.forEachIndexed { index, item ->
            sb.append("${index + 1},")
            sb.append("\"${escape(item.customer.name)}\",")
            sb.append("\"${escape(item.group?.name ?: "عمومی")}\",")
            sb.append("\"${escape(item.customer.phone)}\",")
            sb.append("${item.totalCharges},")
            sb.append("${item.totalPayments},")
            sb.append("${item.balance},")
            sb.append("\"${escape(item.customer.address)}\"\n")
        }

        val totalDebt = debtors.sumOf { it.balance }
        sb.append("\n,,مجموع کل بدهی,,,$totalDebt,\n")
        return sb.toString()
    }

    /**
     * Exports transactions ledger.
     */
    fun generateTransactionsCsv(
        transactions: List<PaymentTransaction>,
        customerNames: Map<Long, String>,
        currencyUnit: String
    ): String {
        val sb = StringBuilder()
        sb.append(UTF8_BOM)
        sb.append("گزارش تاریخچه تراکنش‌ها و پرداخت‌ها (واحد: $currencyUnit)\n")
        sb.append("ردیف,تاریخ شمسی,نام مشتری,نوع تراکنش,مبلغ ($currencyUnit),روش پرداخت,شماره پیگیری,ماه مربوطه,توضیحات\n")

        transactions.forEachIndexed { index, tx ->
            val custName = customerNames[tx.customerId] ?: "مشتری نامشخص"
            val typeTitle = when (tx.type) {
                "PAYMENT" -> "پرداخت"
                "CHARGE" -> "باربرگ"
                "DISCOUNT" -> "تخفیف"
                else -> tx.type
            }
            val forMonthName = tx.forMonth?.let { JalaliCalendar.getMonthName(it) } ?: "-"
            sb.append("${index + 1},")
            sb.append("\"${escape(tx.shamsiDate)}\",")
            sb.append("\"${escape(custName)}\",")
            sb.append("\"$typeTitle\",")
            sb.append("${tx.amount},")
            sb.append("\"${escape(tx.paymentMethod)}\",")
            sb.append("\"${escape(tx.referenceNumber)}\",")
            sb.append("\"$forMonthName\",")
            sb.append("\"${escape(tx.notes)}\"\n")
        }

        return sb.toString()
    }

    private fun escape(text: String): String {
        return text.replace("\"", "\"\"").replace("\n", " ")
    }

    /**
     * Saves CSV string to app cache directory and triggers an Android Share / Open intent.
     */
    fun shareCsv(context: Context, csvContent: String, fileName: String) {
        try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری گزارش باربرگ")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
