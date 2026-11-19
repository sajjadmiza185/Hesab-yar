package com.example.data

import androidx.room.withTransaction

import com.example.model.Customer
import com.example.model.CustomerAccountSummary
import com.example.model.CustomerGroup
import com.example.model.DashboardStatistics
import com.example.model.MonthFinancialSummary
import com.example.model.MonthlyCharge
import com.example.model.PaymentTransaction
import com.example.model.PendingSmsNotification
import com.example.model.SmsHistory
import com.example.model.SmsSendBatch
import com.example.model.Subscription
import com.example.model.TransactionType
import com.example.util.BackupPreference
import com.example.util.JalaliCalendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BarbargRepository(private val db: AppDatabase) {

    private val customerGroupDao = db.customerGroupDao()
    private val customerDao = db.customerDao()
    private val monthlyChargeDao = db.monthlyChargeDao()
    private val transactionDao = db.transactionDao()
    private val smsHistoryDao = db.smsHistoryDao()
    private val smsSendBatchDao = db.smsSendBatchDao()
    private val subscriptionDao = db.subscriptionDao()
    fun pendingSmsDao(): PendingSmsDao = db.pendingSmsDao()

    // Groups
    val allGroups: Flow<List<CustomerGroup>> = customerGroupDao.getAllGroups()
    suspend fun getAllGroupsList(): List<CustomerGroup> = customerGroupDao.getAllGroupsList()
    suspend fun getGroupById(id: Long): CustomerGroup? = customerGroupDao.getGroupById(id)
    suspend fun insertGroup(group: CustomerGroup): Long = customerGroupDao.insertGroup(group)
    suspend fun updateGroup(group: CustomerGroup) = customerGroupDao.updateGroup(group)
    suspend fun deleteGroup(group: CustomerGroup) = customerGroupDao.deleteGroup(group)

    // Customers
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    suspend fun getAllCustomersList(): List<Customer> = customerDao.getAllCustomersList()
    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerByIdFlow(id)
    suspend fun getCustomerByIdOnce(id: Long): Customer? = customerDao.getCustomerById(id)
    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    suspend fun getCustomersCountOnce(): Int = customerDao.getCustomersCountOnce()
    fun getSubscription() = subscriptionDao.getSubscription()
    suspend fun saveSubscription(subscription: Subscription) = subscriptionDao.saveSubscription(subscription)
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer)
        monthlyChargeDao.deleteByCustomerId(customer.id)
        transactionDao.deleteTransactionsByCustomerId(customer.id)
    }

    // Monthly Charges
    fun getChargesForYear(year: Int): Flow<List<MonthlyCharge>> = monthlyChargeDao.getChargesForYear(year)
    fun getCustomerChargesForYear(customerId: Long, year: Int): Flow<List<MonthlyCharge>> =
        monthlyChargeDao.getCustomerChargesForYear(customerId, year)
    fun getAllCustomerCharges(customerId: Long): Flow<List<MonthlyCharge>> =
        monthlyChargeDao.getAllCustomerCharges(customerId)

    suspend fun getMonthlyCharge(customerId: Long, year: Int, month: Int): MonthlyCharge? =
        monthlyChargeDao.getCharge(customerId, year, month)

    suspend fun setMonthlyCharge(customerId: Long, year: Int, month: Int, amount: Long, title: String = "") {
        val existing = monthlyChargeDao.getCharge(customerId, year, month)
        val charge = if (existing != null) {
            existing.copy(amount = amount, title = title, updatedAt = System.currentTimeMillis())
        } else {
            MonthlyCharge(
                customerId = customerId,
                year = year,
                month = month,
                amount = amount,
                title = title,
                updatedAt = System.currentTimeMillis()
            )
        }
        monthlyChargeDao.insertOrUpdate(charge)
    }

    suspend fun batchSetMonthlyCharges(charges: List<MonthlyCharge>) {
        monthlyChargeDao.insertOrUpdateAll(charges)
    }

    suspend fun deleteMonthlyCharge(id: Long) = monthlyChargeDao.deleteById(id)

    // Transactions
    val allTransactions: Flow<List<PaymentTransaction>> = transactionDao.getAllTransactions()
    fun getCustomerTransactions(customerId: Long): Flow<List<PaymentTransaction>> =
        transactionDao.getTransactionsForCustomer(customerId)

    suspend fun insertTransaction(transaction: PaymentTransaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun batchInsertTransactions(transactions: List<PaymentTransaction>) =
        transactionDao.insertAll(transactions)

    suspend fun updateTransaction(transaction: PaymentTransaction) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: PaymentTransaction) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteTransactionById(id)

    /**
     * Combines all data for a specific year to provide full financial account summaries per customer.
     */
    fun getCustomerAccountSummaries(year: Int, currentMonth: Int): Flow<List<CustomerAccountSummary>> {
        return combine(
            customerDao.getAllCustomers(),
            customerGroupDao.getAllGroups(),
            monthlyChargeDao.getChargesForYear(year),
            transactionDao.getAllTransactions()
        ) { customers, groups, charges, transactions ->
            val groupMap = groups.associateBy { it.id }
            val chargesByCustomer = charges.groupBy { it.customerId }
            val transactionsByCustomer = transactions.groupBy { it.customerId }

            customers.map { customer ->
                val customerCharges = chargesByCustomer[customer.id] ?: emptyList()
                val customerTx = transactionsByCustomer[customer.id] ?: emptyList()

                val totalCharges = customerCharges.sumOf { it.amount }

                var totalPayments = 0L
                var totalDiscounts = 0L

                val monthlyPaymentsMap = mutableMapOf<Int, Long>()
                val monthlyDiscountsMap = mutableMapOf<Int, Long>()

                for (tx in customerTx) {
                    when (tx.type) {
                        TransactionType.PAYMENT.name -> {
                            totalPayments += tx.amount
                            if (tx.forYear == null || tx.forYear == year) {
                                tx.forMonth?.let { m ->
                                    monthlyPaymentsMap[m] = (monthlyPaymentsMap[m] ?: 0L) + tx.amount
                                }
                            }
                        }
                        TransactionType.CHARGE.name -> {
                            // Any ad-hoc charges not in monthly_charges
                        }
                        TransactionType.DISCOUNT.name -> {
                            totalDiscounts += tx.amount
                            if (tx.forYear == null || tx.forYear == year) {
                                tx.forMonth?.let { m ->
                                    monthlyDiscountsMap[m] = (monthlyDiscountsMap[m] ?: 0L) + tx.amount
                                }
                            }
                        }
                    }
                }

                val monthlyChargesMap = customerCharges.associate { it.month to it.amount }
                // وضعیت بدهکاری فقط برای ماه انتخاب‌شده محاسبه می‌شود؛
                // مبالغ ماه‌های آینده نباید مشتری را از همین حالا بدهکار نشان دهند.
                val balance = (monthlyChargesMap[currentMonth] ?: 0L) -
                    (monthlyPaymentsMap[currentMonth] ?: 0L) -
                    (monthlyDiscountsMap[currentMonth] ?: 0L)

                CustomerAccountSummary(
                    customer = customer,
                    group = customer.groupId?.let { groupMap[it] },
                    totalCharges = totalCharges,
                    totalPayments = totalPayments,
                    totalDiscounts = totalDiscounts,
                    balance = balance,
                    monthlyCharges = monthlyChargesMap,
                    monthlyPayments = monthlyPaymentsMap
                )
            }
        }
    }

    /**
     * Dashboard financial statistics for a selected year and current month.
     */
    fun getDashboardStatistics(year: Int, currentMonth: Int): Flow<DashboardStatistics> {
        return combine(
            customerDao.getAllCustomers(),
            monthlyChargeDao.getChargesForYear(year),
            transactionDao.getAllTransactions()
        ) { customers, charges, transactions ->
            val totalCustomers = customers.size
            val chargesByCustomer = charges.groupBy { it.customerId }
            val txByCustomer = transactions.groupBy { it.customerId }

            var currentMonthCharges = 0L
            for (charge in charges) {
                if (charge.month == currentMonth) currentMonthCharges += charge.amount
            }

            var totalPayments = 0L
            var currentMonthPayments = 0L
            for (tx in transactions) {
                if (tx.type == TransactionType.PAYMENT.name) {
                    totalPayments += tx.amount
                    if ((tx.forYear == null || tx.forYear == year) && tx.forMonth == currentMonth) {
                        currentMonthPayments += tx.amount
                    }
                }
            }

            // بدهی کل و تعداد بدهکاران فقط از مانده همین ماه محاسبه می‌شود.
            // اعتبار یک مشتری نباید بدهی مشتری دیگری را خنثی کند.
            var currentMonthOutstandingDebt = 0L
            var activeDebtors = 0
            for (customer in customers) {
                val monthCharge = (chargesByCustomer[customer.id] ?: emptyList())
                    .filter { it.month == currentMonth }
                    .sumOf { it.amount }
                val customerTx = txByCustomer[customer.id] ?: emptyList()
                val monthPaid = customerTx.filter {
                    it.type == TransactionType.PAYMENT.name &&
                        (it.forYear == null || it.forYear == year) && it.forMonth == currentMonth
                }.sumOf { it.amount }
                val monthDiscount = customerTx.filter {
                    it.type == TransactionType.DISCOUNT.name &&
                        (it.forYear == null || it.forYear == year) && it.forMonth == currentMonth
                }.sumOf { it.amount }
                val balance = monthCharge - monthPaid - monthDiscount
                if (balance > 0L) {
                    currentMonthOutstandingDebt += balance
                    activeDebtors++
                }
            }

            DashboardStatistics(
                totalCustomersCount = totalCustomers,
                activeDebtorsCount = activeDebtors,
                totalChargesAmount = currentMonthCharges,
                totalPaymentsAmount = totalPayments,
                totalOutstandingDebt = currentMonthOutstandingDebt,
                currentMonthCharges = currentMonthCharges,
                currentMonthPayments = currentMonthPayments
            )
        }
    }

    /**
     * Monthly 12-month breakdown for the annual report and charts.
     */
    fun getMonthlyFinancialBreakdown(year: Int): Flow<List<MonthFinancialSummary>> {
        return combine(
            monthlyChargeDao.getChargesForYear(year),
            transactionDao.getTransactionsForYear(year)
        ) { charges, transactions ->
            val chargesByMonth = charges.groupBy { it.month }
            val paymentsByMonth = transactions
                .filter { it.type == TransactionType.PAYMENT.name && it.forMonth != null }
                .groupBy { it.forMonth!! }

            (1..12).map { month ->
                val monthCharges = (chargesByMonth[month] ?: emptyList()).sumOf { it.amount }
                val monthPayments = (paymentsByMonth[month] ?: emptyList()).sumOf { it.amount }
                MonthFinancialSummary(
                    month = month,
                    monthName = JalaliCalendar.getMonthName(month),
                    totalCharges = monthCharges,
                    totalPayments = monthPayments,
                    balance = monthCharges - monthPayments
                )
            }
        }
    }



    // SMS send batch archive
    fun observeSmsSendBatches(): Flow<List<SmsSendBatch>> =
        smsSendBatchDao.observeAll()

    fun observeAllSmsHistory(): Flow<List<SmsHistory>> =
        smsHistoryDao.observeAll()

    suspend fun createSmsSendBatch(title: String = ""): Long =
        smsSendBatchDao.insert(SmsSendBatch(title = title))

    suspend fun deleteSmsSendBatch(batchId: Long): Int {
        smsHistoryDao.deleteByBatchId(batchId)
        return smsSendBatchDao.deleteById(batchId)
    }

    suspend fun deleteSmsSendBatches(batchIds: List<Long>): Int {
        val ids = batchIds.distinct()
        if (ids.isEmpty()) return 0
        ids.forEach { smsHistoryDao.deleteByBatchId(it) }
        return smsSendBatchDao.deleteMany(ids)
    }

    // Outgoing SMS history
    fun observeSmsHistory(): Flow<List<SmsHistory>> =
        smsHistoryDao.observeAll()

    fun observeCustomerSmsHistory(customerId: Long): Flow<List<SmsHistory>> =
        smsHistoryDao.observeByCustomer(customerId)

    suspend fun insertSmsHistory(item: SmsHistory): Long =
        smsHistoryDao.insert(item)

    suspend fun updateSmsSentResult(
        id: Long,
        completed: Int,
        failed: Int,
        status: String,
        errorReason: String
    ): Int = smsHistoryDao.updateSentResult(id, completed, failed, status, errorReason)

    suspend fun updateSmsDeliveryResult(
        id: Long,
        completed: Int,
        failed: Int
    ): Int = smsHistoryDao.updateDeliveryResult(id, completed, failed)

    suspend fun deleteSmsHistory(id: Long): Int =
        smsHistoryDao.deleteById(id)

    suspend fun deleteSmsHistoryMany(ids: List<Long>): Int =
        if (ids.isEmpty()) 0 else smsHistoryDao.deleteMany(ids)

    suspend fun deleteSmsHistoryInDateRange(start: Long, end: Long): Int =
        smsHistoryDao.deleteInDateRange(start, end)

    suspend fun deleteAllSmsHistory(): Int =
        smsHistoryDao.deleteAll()
    suspend fun recordSmsSentPart(
        id: Long,
        succeeded: Int,
        failed: Int,
        errorReason: String
    ): Int = smsHistoryDao.recordSentPart(id, succeeded, failed, errorReason)

    suspend fun recordSmsDeliveryPart(
        id: Long,
        succeeded: Int,
        failed: Int
    ): Int = smsHistoryDao.recordDeliveryPart(id, succeeded, failed)
    // Export every internal table for Backup. Preferences are added by the ViewModel.
    suspend fun getAllDataForBackup(): BackupContainer {
        return BackupContainer(
            groups = customerGroupDao.getAllGroupsList(),
            customers = customerDao.getAllCustomersList(),
            monthlyCharges = monthlyChargeDao.getAllChargesList(),
            transactions = transactionDao.getAllTransactionsList(),
            smsSendBatches = db.smsSendBatchDao().getAllList(),
            smsHistory = db.smsHistoryDao().getAllList(),
            pendingSmsNotifications = db.pendingSmsDao().getAllList(),
            subscription = db.subscriptionDao().getSubscriptionOnce()
        )
    }

    // Restore is additive: existing rows are never deleted or overwritten.
    // Subscription is only replaced when the backup grants a higher customer limit.
    suspend fun restoreFromBackup(backup: BackupContainer) {
        fun <T> unique(items: List<T>, id: (T) -> Long, label: String) {
            require(items.map(id).toSet().size == items.size) { "شناسه تکراری در $label" }
        }
        unique(backup.groups, { it.id }, "گروه‌ها")
        unique(backup.customers, { it.id }, "مشتریان")
        unique(backup.monthlyCharges, { it.id }, "مبالغ")
        unique(backup.transactions, { it.id }, "تراکنش‌ها")
        unique(backup.smsSendBatches, { it.id }, "نوبت‌های پیامک")
        unique(backup.smsHistory, { it.id }, "سابقه پیامک")
        val groupIds = backup.groups.map { it.id }.toSet()
        val customerIds = backup.customers.map { it.id }.toSet()
        require(backup.customers.all { it.groupId == null || it.groupId in groupIds })
        require(backup.groups.all { it.leaderCustomerId == null || it.leaderCustomerId in customerIds })
        require(backup.monthlyCharges.all { it.customerId in customerIds })
        require(backup.transactions.all { it.customerId in customerIds })
        val keys = backup.monthlyCharges.map { Triple(it.customerId, it.year, it.month) }
        require(keys.toSet().size == keys.size) { "ماه تکراری برای مشتری در فایل پشتیبان" }

        db.withTransaction {
            val groupMap = mutableMapOf<Long, Long>()
            backup.groups.forEach { old ->
                groupMap[old.id] = customerGroupDao.insertGroup(old.copy(id = 0L, leaderCustomerId = null))
            }
            val customerMap = mutableMapOf<Long, Long>()
            backup.customers.forEach { old ->
                customerMap[old.id] = customerDao.insertCustomer(old.copy(id = 0L, groupId = old.groupId?.let(groupMap::getValue)))
            }
            backup.groups.forEach { old ->
                val leader = old.leaderCustomerId?.let(customerMap::getValue)
                if (leader != null) {
                    val newGroup = customerGroupDao.getGroupById(groupMap.getValue(old.id)) ?: error("گروه پیدا نشد")
                    customerGroupDao.updateGroup(newGroup.copy(leaderCustomerId = leader))
                }
            }
            backup.monthlyCharges.forEach { old ->
                monthlyChargeDao.insertOrUpdate(old.copy(id = 0L, customerId = customerMap.getValue(old.customerId)))
            }
            backup.transactions.forEach { old ->
                transactionDao.insertTransaction(old.copy(id = 0L, customerId = customerMap.getValue(old.customerId)))
            }

            // SMS records: if a customer no longer exists, the original id is kept so the record is not lost.
            val batchMap = mutableMapOf<Long, Long>()
            backup.smsSendBatches.forEach { old ->
                batchMap[old.id] = db.smsSendBatchDao().insert(old.copy(id = 0L))
            }
            backup.smsHistory.forEach { old ->
                db.smsHistoryDao().insert(
                    old.copy(
                        id = 0L,
                        customerId = customerMap[old.customerId] ?: old.customerId,
                        batchId = old.batchId?.let { batchMap[it] }
                    )
                )
            }
            // Duplicate pending notifications are skipped by the unique dedupeKey index.
            backup.pendingSmsNotifications.forEach { old ->
                db.pendingSmsDao().insert(
                    old.copy(id = 0L, customerId = customerMap[old.customerId] ?: old.customerId)
                )
            }

            backup.subscription?.let { incoming ->
                val current = db.subscriptionDao().getSubscriptionOnce()
                if (current == null || incoming.customerLimit > current.customerLimit) {
                    db.subscriptionDao().saveSubscription(incoming.copy(id = 1))
                }
            }
        }
    }
}

data class BackupContainer(
    val groups: List<CustomerGroup> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val monthlyCharges: List<MonthlyCharge> = emptyList(),
    val transactions: List<PaymentTransaction> = emptyList(),
    val smsSendBatches: List<SmsSendBatch> = emptyList(),
    val smsHistory: List<SmsHistory> = emptyList(),
    val pendingSmsNotifications: List<PendingSmsNotification> = emptyList(),
    val subscription: Subscription? = null,
    val preferences: List<BackupPreference> = emptyList()
)
