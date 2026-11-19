package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.room.withTransaction
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BarbargRepository
import com.example.model.Customer
import com.example.util.ImportedCustomer
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
import com.example.billing.BazaarBillingManager
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import com.example.model.TransactionType
import com.example.util.BackupManager
import com.example.util.CsvExporter
import com.example.util.JalaliCalendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

enum class AppScreen(val persianTitle: String) {
    DASHBOARD("داشبورد"),
    CUSTOMERS("مشتریان"),
    MONTHLY_CHARGES("مبالغ ماهانه"),
    TRANSACTIONS("تراکنش‌ها"),
    REPORTS("گزارش‌ها"),
    SETTINGS("تنظیمات و پشتیبان")
}

data class UiState(
    val currentScreen: AppScreen = AppScreen.DASHBOARD,
    val selectedCustomerDetailId: Long? = null,
    val selectedYear: Int = JalaliCalendar.today().year,
    val selectedMonth: Int = JalaliCalendar.today().month,
    val currencyUnit: String = "\u062A\u0648\u0645\u0627\u0646",
    val usePersianDigits: Boolean = true,
    val themeMode: String = "SYSTEM",
    val customerSearchQuery: String = "",
    val selectedGroupFilterId: Long? = null,
    val debtorOnlyFilter: Boolean = false,
    val transactionTypeFilter: String = "ALL",
    val transactionSearchQuery: String = "",
    val isLoading: Boolean = false,
    val isManager: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class BarbargViewModel(application: Application) : AndroidViewModel(application) {

    private val repository =
        BarbargRepository(AppDatabase.getDatabase(application))

    fun observeSubscription() = repository.getSubscription()

    /**
     * Persist only a Poolakey purchase for a known product.
     * The UI cannot supply or override the purchased customer capacity.
     */
    fun saveVerifiedPurchase(purchase: PurchaseInfo): Boolean {
        val limit = BazaarBillingManager.PRODUCT_LIMITS[purchase.productId] ?: return false
        if (purchase.purchaseToken.isBlank() || purchase.purchaseTime <= 0L) return false

        val subscription = Subscription(
            planCode = purchase.productId,
            customerLimit = limit,
            purchaseToken = purchase.purchaseToken,
            productId = purchase.productId,
            purchasedAt = purchase.purchaseTime,
            isVerified = true
        )
        viewModelScope.launch { repository.saveSubscription(subscription) }
        return true
    }


    fun observeSmsSendBatches() = repository.observeSmsSendBatches()

    fun observeAllSmsHistory() = repository.observeAllSmsHistory()

    suspend fun createSmsSendBatchAndReturn(title: String = ""): Long =
        repository.createSmsSendBatch(title)

    fun createSmsSendBatch(title: String = "", onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            onCreated(repository.createSmsSendBatch(title))
        }
    }

    fun deleteSmsSendBatch(batchId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteSmsSendBatch(batchId)
            onDeleted()
        }
    }

    fun deleteSmsSendBatches(batchIds: List<Long>, onDeleted: () -> Unit = {}) {
        val ids = batchIds.distinct()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteSmsSendBatches(ids)
            onDeleted()
        }
    }

    fun observeCustomerSmsHistory(customerId: Long) =
        repository.observeCustomerSmsHistory(customerId)

    fun deleteSmsHistory(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteSmsHistoryMany(ids.distinct())
        }
    }

    fun insertSmsHistory(item: SmsHistory, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertSmsHistory(item)
            onSaved(id)
        }
    }

    suspend fun insertSmsHistoryAndReturn(item: SmsHistory): Long {
        return repository.insertSmsHistory(item)
    }

    fun updateSmsSentResult(
        id: Long,
        completed: Int,
        failed: Int,
        status: String,
        errorReason: String
    ) {
        viewModelScope.launch {
            repository.updateSmsSentResult(id, completed, failed, status, errorReason)
        }
    }

    fun updateSmsDeliveryResult(id: Long, completed: Int, failed: Int) {
        viewModelScope.launch {
            repository.updateSmsDeliveryResult(id, completed, failed)
        }
    }
    fun recordSmsSentPart(
        id: Long,
        succeeded: Int,
        failed: Int,
        errorReason: String = ""
    ) {
        viewModelScope.launch {
            repository.recordSmsSentPart(id, succeeded, failed, errorReason)
        }
    }

    fun recordSmsDeliveryPart(
        id: Long,
        succeeded: Int,
        failed: Int
    ) {
        viewModelScope.launch {
            repository.recordSmsDeliveryPart(id, succeeded, failed)
        }
    }
    private val preferences = application.getSharedPreferences("barberg_preferences", 0)
    // One-time security migration: invalidate the old user-created manager PIN.
    // The owner credential is verified against a salted PBKDF2 hash compiled into this build.
    private val _managerSecurityMigration = if (!preferences.getBoolean("manager_auth_v2_initialized", false)) {
        preferences.edit()
            .remove("manager_pin_hash")
            .remove("manager_pin_salt")
            .putBoolean("manager_access_granted", false)
            .putBoolean("manager_auth_v2_initialized", true)
            .apply()
        true
    } else false
    private val _initialTheme = if (!preferences.getBoolean("midnight_3d_default_applied", false)) {
        preferences.edit().putString("theme_mode", "MIDNIGHT_HD_3D").putBoolean("midnight_3d_default_applied", true).apply()
        "MIDNIGHT_HD_3D"
    } else {
        preferences.getString("theme_mode", "MIDNIGHT_HD_3D") ?: "MIDNIGHT_HD_3D"
    }
    private val _safeInitialTheme = if (_initialTheme == "BLUE_GLOSS_HD_3D" || _initialTheme == "BLUE_GLOSS_HD") {
        preferences.edit().putString("theme_mode", "MIDNIGHT_HD_3D").apply()
        "MIDNIGHT_HD_3D"
    } else _initialTheme

    private val _uiState = MutableStateFlow(UiState(themeMode = _safeInitialTheme, isManager = preferences.getBoolean("manager_access_granted", false)))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _snackBarMessage = MutableSharedFlow<String>()
    val snackBarMessage: SharedFlow<String> = _snackBarMessage.asSharedFlow()

    // Data streams
    val groups: StateFlow<List<CustomerGroup>> =
        repository.allGroups.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customers: StateFlow<List<Customer>> =
        repository.allCustomers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingSmsNotifications: StateFlow<List<PendingSmsNotification>> = repository.pendingSmsDao().pending().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pendingSmsCount: StateFlow<Int> = repository.pendingSmsDao().pendingCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun rejectSmsNotifications(ids: List<Long>) {
        viewModelScope.launch {
            ids.distinct().forEach { id -> repository.pendingSmsDao().setStatus(id, "REJECTED") }
        }
    }

    fun rejectSmsNotification(id: Long) {
        viewModelScope.launch { repository.pendingSmsDao().setStatus(id, "REJECTED") }
    }

    fun approveSmsNotification(id: Long) {
        viewModelScope.launch {
            val item = repository.pendingSmsDao().byId(id) ?: return@launch
            if (item.status != "PENDING") return@launch
            val amount = if (item.amountRial > 0L) {
                if (_uiState.value.currencyUnit == "\u062A\u0648\u0645\u0627\u0646") item.amountRial / 10L else item.amountRial
            } else {
                // A customer text message has no verified amount; suggest the configured tariff for that month.
                repository.getMonthlyCharge(item.customerId, item.forYear, item.forMonth)?.amount ?: 0L
            }
            if (amount <= 0L) {
                _snackBarMessage.emit("\u0628\u0631\u0627\u06CC \u0627\u06CC\u0646 \u0645\u0627\u0647 \u0645\u0628\u0644\u063A \u0634\u0627\u0631\u0698 \u062B\u0628\u062A \u0646\u0634\u062F\u0647 \u0627\u0633\u062A\u061B \u0627\u0628\u062A\u062F\u0627 \u0634\u0627\u0631\u0698 \u0645\u0627\u0647\u0627\u0646\u0647 \u0631\u0627 \u062A\u0639\u0631\u06CC\u0641 \u06A9\u0646\u06CC\u062F.")
                return@launch
            }
            val transaction = PaymentTransaction(
                customerId = item.customerId,
                type = TransactionType.PAYMENT.name,
                amount = amount,
                shamsiDate = item.shamsiDate,
                paymentMethod = "کارت به کارت (پیامک تأییدشده)",
                referenceNumber = item.trackingNumber,
                forMonth = item.forMonth,
                forYear = item.forYear,
                notes = "ثبت پس از تأیید دستی پیامک: ${item.senderNumber}"
            )
            val inserted = AppDatabase.getDatabase(getApplication()).withTransaction {
                val current = repository.pendingSmsDao().byId(id) ?: return@withTransaction false
                if (current.status != "PENDING") return@withTransaction false
                repository.insertTransaction(transaction)
                repository.pendingSmsDao().setStatus(id, "APPROVED")
                true
            }
            if (inserted) _snackBarMessage.emit("\u067E\u0631\u062F\u0627\u062E\u062A \u067E\u06CC\u0627\u0645\u06A9 \u067E\u0633 \u0627\u0632 \u062A\u0627\u06CC\u06CC\u062F \u062B\u0628\u062A \u0634\u062F.")
        }
    }


    fun approveGroupSmsNotification(id: Long, selectedCustomerIds: List<Long>) {
        viewModelScope.launch {
            val item = repository.pendingSmsDao().byId(id) ?: return@launch
            if (item.status != "PENDING") return@launch

            val ids = selectedCustomerIds.distinct()
            if (ids.isEmpty()) {
                _snackBarMessage.emit("Select at least one group member.")
                return@launch
            }

            val allCustomers = repository.getAllCustomersList()
            val leader = allCustomers.firstOrNull { it.id == item.customerId }
            val groupId = leader?.groupId
            if (groupId == null) {
                _snackBarMessage.emit("The SMS recipient is not assigned to a group.")
                return@launch
            }

            val validMembers = allCustomers.filter {
                it.isActive && it.groupId == groupId && it.id in ids
            }
            if (validMembers.size != ids.size) {
                _snackBarMessage.emit("Some selected members are inactive or belong to another group.")
                return@launch
            }

            val sortedMembers = validMembers.sortedBy { it.id }
            val amountsByCustomerId: Map<Long, Long>

            if (item.amountRial > 0L) {
                val total = if (_uiState.value.currencyUnit == "\u062A\u0648\u0645\u0627\u0646") {
                    item.amountRial / 10L
                } else {
                    item.amountRial
                }

                if (total < sortedMembers.size.toLong()) {
                    _snackBarMessage.emit("The SMS amount is too small to divide among the selected members.")
                    return@launch
                }

                val baseAmount = total / sortedMembers.size
                val remainder = (total % sortedMembers.size).toInt()

                amountsByCustomerId = sortedMembers.mapIndexed { index, customer ->
                    customer.id to (baseAmount + if (index < remainder) 1L else 0L)
                }.toMap()
            } else {
                val tariffs = sortedMembers.associate { customer ->
                    customer.id to (
                        repository.getMonthlyCharge(
                            customer.id,
                            item.forYear,
                            item.forMonth
                        )?.amount ?: 0L
                    )
                }

                if (tariffs.values.any { it <= 0L }) {
                    _snackBarMessage.emit(
                        "A monthly charge is missing for one or more selected members. No payments were recorded."
                    )
                    return@launch
                }

                amountsByCustomerId = tariffs
            }

            val transactionsToInsert = sortedMembers.map { customer ->
                PaymentTransaction(
                    customerId = customer.id,
                    type = TransactionType.PAYMENT.name,
                    amount = amountsByCustomerId.getValue(customer.id),
                    shamsiDate = item.shamsiDate,
                    paymentMethod = "کارت به کارت (پیامک تأییدشده گروهی)",
                    referenceNumber = item.trackingNumber,
                    forMonth = item.forMonth,
                    forYear = item.forYear,
                    notes = if (item.amountRial > 0L) {
                        "Group SMS manually approved; sender: ${item.senderNumber}"
                    } else {
                        "Group SMS manually approved using individual monthly charges; sender: ${item.senderNumber}"
                    }
                )
            }

            val inserted = AppDatabase.getDatabase(getApplication()).withTransaction {
                val current = repository.pendingSmsDao().byId(id)
                    ?: return@withTransaction false
                if (current.status != "PENDING") return@withTransaction false

                repository.batchInsertTransactions(transactionsToInsert)
                repository.pendingSmsDao().setStatus(id, "APPROVED")
                true
            }

            if (inserted) {
                _snackBarMessage.emit("Group payment approved for ${sortedMembers.size} members.")
            }
        }
    }
    val transactions: StateFlow<List<PaymentTransaction>> =
        repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedYearCharges: StateFlow<List<MonthlyCharge>> = _uiState
        .flatMapLatest { state ->
            repository.getChargesForYear(state.selectedYear)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customerAccountSummaries: StateFlow<List<CustomerAccountSummary>> =
        _uiState
            .flatMapLatest { state ->
                repository.getCustomerAccountSummaries(state.selectedYear, state.selectedMonth)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val dashboardStats: StateFlow<DashboardStatistics> = _uiState
        .flatMapLatest { state ->
            repository.getDashboardStatistics(
                state.selectedYear,
                state.selectedMonth
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStatistics()
        )

    val monthlyBreakdown: StateFlow<List<MonthFinancialSummary>> =
        _uiState
            .flatMapLatest { state ->
                repository.getMonthlyFinancialBreakdown(state.selectedYear)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Navigation and Filters
    fun setScreen(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(
            currentScreen = screen,
            selectedCustomerDetailId = null
        )
    }

    fun openCustomerDetail(customerId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedCustomerDetailId = customerId
        )
    }

    fun closeCustomerDetail() {
        _uiState.value = _uiState.value.copy(
            selectedCustomerDetailId = null
        )
    }

    fun setSelectedYear(year: Int) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
    }

    fun setSelectedMonth(month: Int) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
    }

    fun setCustomerSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            customerSearchQuery = query
        )
    }

    fun setGroupFilter(groupId: Long?) {
        _uiState.value = _uiState.value.copy(
            selectedGroupFilterId = groupId
        )
    }

    fun setDebtorOnlyFilter(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            debtorOnlyFilter = enabled
        )
    }

    fun toggleDebtorOnlyFilter() {
        _uiState.value = _uiState.value.copy(
            debtorOnlyFilter = !_uiState.value.debtorOnlyFilter
        )
    }

    fun setTransactionTypeFilter(type: String) {
        _uiState.value = _uiState.value.copy(
            transactionTypeFilter = type
        )
    }

    fun setTransactionSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            transactionSearchQuery = query
        )
    }

    fun setCurrencyUnit(unit: String) {
        _uiState.value = _uiState.value.copy(
            currencyUnit = unit
        )
    }

    fun setUsePersianDigits(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            usePersianDigits = enabled
        )
    }

    fun setThemeMode(mode: String) {
        preferences.edit().putString("theme_mode", mode).apply()
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    // The password itself is never stored in preferences. Only a salted verifier is in the APK.
    fun unlockManager(password: String): Boolean {
        val saltBytes = Base64.decode(MANAGER_PASSWORD_SALT, Base64.NO_WRAP)
        val actual = managerPasswordHash(password, saltBytes)
        val valid = MessageDigest.isEqual(actual.toByteArray(Charsets.UTF_8), MANAGER_PASSWORD_HASH.toByteArray(Charsets.UTF_8))
        if (valid) {
            preferences.edit().putBoolean("manager_access_granted", true).apply()
            _uiState.value = _uiState.value.copy(isManager = true)
        }
        return valid
    }

    fun revokeManagerAccess() {
        preferences.edit().putBoolean("manager_access_granted", false).apply()
        _uiState.value = _uiState.value.copy(isManager = false)
    }

    private companion object {
        // Owner-only credential verifier. The raw password is not embedded in the app.
        const val MANAGER_PASSWORD_SALT = "kZpBlERIHzA9+Z1+I8cMOQ=="
        const val MANAGER_PASSWORD_HASH = "QRpc3QTE14neBSpNVhggGuC+Vf0+nOMiReLOBDfEZo8="
    }

    private fun managerPasswordHash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally { spec.clearPassword() }
    }

    // Customer operations
    fun saveCustomer(
        id: Long = 0,
        name: String,
        phone: String,
        groupId: Long?,
        address: String,
        notes: String,
        username: String? = null,
        passwordEncrypted: String? = null,
        monthlyAmount: Long = 0L,
        monthlyYear: Int = _uiState.value.selectedYear,
        monthlyMonth: Int = _uiState.value.selectedMonth
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _snackBarMessage.emit("لطفاً نام مشتری را وارد کنید.")
                return@launch
            }

            if (id == 0L) {
                val subscription = repository.getSubscription().first()
                val limit = subscription?.customerLimit ?: 5
                if (!_uiState.value.isManager && repository.getCustomersCountOnce() >= limit) {
                    _snackBarMessage.emit("\u0638\u0631\u0641\u06cc\u062a \u0628\u0633\u062a\u0647 \u0634\u0645\u0627 \u062a\u06a9\u0645\u06cc\u0644 \u0634\u062f\u0647 \u0627\u0633\u062a. \u0628\u0631\u0627\u06cc \u062b\u0628\u062a \u0645\u0634\u062a\u0631\u06cc \u062c\u062f\u06cc\u062f \u0627\u0634\u062a\u0631\u0627\u06a9 \u0631\u0627 \u0627\u0631\u062a\u0642\u0627 \u062f\u0647\u06cc\u062f.")
                    return@launch
                }
            }

            val existingCustomer = if (id != 0L) repository.getCustomerByIdOnce(id) else null

            val customer = Customer(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                groupId = groupId,
                address = address.trim(),
                notes = notes.trim(),
                username = username ?: existingCustomer?.username.orEmpty(),
                passwordEncrypted = passwordEncrypted ?: existingCustomer?.passwordEncrypted.orEmpty()
            )

            if (id == 0L) {
                val insertedId = repository.insertCustomer(customer)
                if (monthlyAmount > 0L) {
                    for (month in monthlyMonth..12) {
                        repository.setMonthlyCharge(
                            customerId = insertedId,
                            year = monthlyYear,
                            month = month,
                            amount = monthlyAmount
                        )
                    }
                }
                _snackBarMessage.emit(
                    if (monthlyAmount > 0L) "مشتری و مبلغ ماهانه با موفقیت ثبت شدند." else "مشتری جدید با موفقیت اضافه شد."
                )
            } else {
                repository.updateCustomer(customer)
                if (monthlyAmount > 0L) {
                    // Preserve historical months. Apply the new rate only from the selected month forward.
                    for (month in monthlyMonth..12) {
                        repository.setMonthlyCharge(
                            customerId = id,
                            year = monthlyYear,
                            month = month,
                            amount = monthlyAmount
                        )
                    }
                }
                _snackBarMessage.emit("اطلاعات مشتری و مبلغ ماهانه از ماه انتخاب‌شده به بعد به‌روزرسانی شد.")
            }
        }
    }

    fun importCustomers(imported: List<ImportedCustomer>) {
        if (imported.isEmpty()) return
        viewModelScope.launch {
            val subscription = repository.getSubscription().first()
            val limit = subscription?.customerLimit ?: 5
            val currentCount = repository.getCustomersCountOnce()
            var inserted = 0
            var skipped = 0
            val remaining = if (_uiState.value.isManager) Int.MAX_VALUE else (limit - currentCount).coerceAtLeast(0)

            for (row in imported) {
                val name = row.name.trim()
                if (name.isBlank()) continue
                if (inserted >= remaining) {
                    skipped++
                    continue
                }
                repository.insertCustomer(
                    Customer(
                        name = name,
                        phone = row.phone.trim(),
                        address = row.address.trim(),
                        notes = row.notes.trim()
                    )
                )
                inserted++
            }
            _snackBarMessage.emit("$inserted مشتری اضافه شد؛ $skipped مشتری به‌دلیل تکمیل ظرفیت وارد نشد.")
        }
    }
    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)

            if (_uiState.value.selectedCustomerDetailId == customer.id) {
                closeCustomerDetail()
            }

            _snackBarMessage.emit(
                "مشتری «${customer.name}» و سوابق مرتبط حذف شد."
            )
        }
    }

    fun deleteCustomers(customerIds: List<Long>) {
        viewModelScope.launch {
            val all = repository.getAllCustomersList().associateBy { it.id }
            val targets = customerIds.distinct().mapNotNull { all[it] }
            targets.forEach { customer ->
                repository.deleteCustomer(customer)
                if (_uiState.value.selectedCustomerDetailId == customer.id) {
                    closeCustomerDetail()
                }
            }
            _snackBarMessage.emit("${targets.size} مشتری و سوابق مرتبط با موفقیت حذف شدند.")
        }
    }

    // Group operations
    fun saveGroup(
        id: Long = 0,
        name: String,
        description: String,
        colorHex: String,
        leaderCustomerId: Long? = null
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _snackBarMessage.emit("لطفاً نام گروه را وارد کنید.")
                return@launch
            }

            val group = CustomerGroup(
                id = id,
                name = name.trim(),
                description = description.trim(),
                colorHex = colorHex,
                leaderCustomerId = leaderCustomerId
            )

            if (id == 0L) {
                repository.insertGroup(group)
                _snackBarMessage.emit("گروه جدید با موفقیت ایجاد شد.")
            } else {
                repository.updateGroup(group)
                _snackBarMessage.emit("اطلاعات گروه با موفقیت ویرایش شد.")
            }
        }
    }

    fun deleteGroup(group: CustomerGroup) {
        viewModelScope.launch {
            repository.deleteGroup(group)
            _snackBarMessage.emit("گروه «${group.name}» حذف شد.")
        }
    }

    /**
     * تنظیم اعضای یک گروه:
     * - مشتریان انتخاب‌شده به گروه مقصد منتقل می‌شوند.
     * - مشتریانی که قبلاً در همین گروه بودند ولی دیگر انتخاب نشده‌اند،
     *   بدون گروه می‌شوند.
     * - سایر مشتریان دست‌نخورده می‌مانند.
     */
    fun setGroupMembers(
        groupId: Long,
        selectedCustomerIds: List<Long>
    ) {
        viewModelScope.launch {
            val selectedIds = selectedCustomerIds.toSet()

            if (groupId <= 0L) {
                _snackBarMessage.emit("گروه انتخاب‌شده معتبر نیست")
                return@launch
            }

            val currentCustomers = customers.value

            val customersToUpdate = currentCustomers.filter { customer ->
                customer.groupId == groupId ||
                    customer.id in selectedIds
            }

            var updatedCount = 0

            customersToUpdate.forEach { customer ->
                val newGroupId = if (customer.id in selectedIds) {
                    groupId
                } else {
                    null
                }

                if (customer.groupId != newGroupId) {
                    repository.updateCustomer(
                        customer.copy(groupId = newGroupId)
                    )
                    updatedCount++
                }
            }

            val group = repository.getGroupById(groupId)
            if (group?.leaderCustomerId != null && group.leaderCustomerId !in selectedIds) {
                repository.updateGroup(group.copy(leaderCustomerId = null))
                _snackBarMessage.emit("سرگروه قبلی از اعضای گروه خارج شد؛ لطفاً سرگروه جدید را انتخاب کنید")
            }

            _snackBarMessage.emit(
                "اعضای گروه ذخیره شدند؛ $updatedCount مشتری به‌روزرسانی شد"
            )
        }
    }

    // Monthly Charge operations
    fun setMonthlyCharge(
        customerId: Long,
        year: Int,
        month: Int,
        amount: Long,
        title: String = ""
    ) {
        viewModelScope.launch {
            val today = JalaliCalendar.today()
            if (year != today.year || month !in 1..12) {
                _snackBarMessage.emit("تنظیم مبلغ فقط برای ماه‌های سال جاری امکان‌پذیر است")
                return@launch
            }

            val startMonth = maxOf(month, today.month)
            for (targetMonth in startMonth..12) {
                repository.setMonthlyCharge(
                    customerId,
                    year,
                    targetMonth,
                    amount,
                    title
                )
            }

            _snackBarMessage.emit(
                "مبلغ از ماه ${JalaliCalendar.getMonthName(startMonth)} تا پایان سال ثبت شد"
            )
        }
    }

    fun batchSetMonthAmounts(
        customerIds: List<Long>,
        year: Int,
        month: Int,
        amount: Long
    ) {
        viewModelScope.launch {
            val today = JalaliCalendar.today()
            if (year != today.year) {
                _snackBarMessage.emit("تنظیم گروهی فقط برای ماه‌های باقی‌مانده سال جاری انجام می‌شود")
                return@launch
            }

            val startMonth = maxOf(month, today.month)
            val charges = customerIds.flatMap { customerId ->
                (startMonth..12).map { targetMonth ->
                    MonthlyCharge(
                        customerId = customerId,
                        year = year,
                        month = targetMonth,
                        amount = amount,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }

            repository.batchSetMonthlyCharges(charges)

            _snackBarMessage.emit(
                "مبلغ برای ${customerIds.size} مشتری با موفقیت اعمال شد"
            )
        }
    }

    // Payment & Transaction operations
    fun registerPayment(
        customerId: Long,
        amount: Long,
        shamsiDate: String,
        paymentMethod: String,
        referenceNumber: String,
        forMonth: Int?,
        forYear: Int?,
        notes: String
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                _snackBarMessage.emit(
                    "مبلغ پرداختی باید بیشتر از صفر باشد"
                )
                return@launch
            }

            val transaction = PaymentTransaction(
                customerId = customerId,
                type = TransactionType.PAYMENT.name,
                amount = amount,
                shamsiDate = shamsiDate,
                paymentMethod = paymentMethod,
                referenceNumber = referenceNumber.trim(),
                forMonth = forMonth,
                forYear = forYear,
                notes = notes.trim()
            )

            repository.insertTransaction(transaction)
            _snackBarMessage.emit("پرداخت با موفقیت ثبت شد.")
        }
    }

    fun batchRegisterPayments(
        customerIds: List<Long>,
        amountPerCustomer: Long,
        shamsiDate: String,
        paymentMethod: String,
        forMonth: Int?,
        forYear: Int?,
        notes: String
    ) {
        viewModelScope.launch {
            if (customerIds.isEmpty()) {
                _snackBarMessage.emit("هیچ مشتری انتخاب نشده است")
                return@launch
            }

            if (amountPerCustomer <= 0) {
                _snackBarMessage.emit("مبلغ باید بیشتر از صفر باشد")
                return@launch
            }

            val transactionsToInsert = customerIds.map { id ->
                PaymentTransaction(
                    customerId = id,
                    type = TransactionType.PAYMENT.name,
                    amount = amountPerCustomer,
                    shamsiDate = shamsiDate,
                    paymentMethod = paymentMethod,
                    referenceNumber = "پرداخت گروهی",
                    forMonth = forMonth,
                    forYear = forYear,
                    notes = notes
                )
            }

            repository.batchInsertTransactions(transactionsToInsert)

            _snackBarMessage.emit(
                "پرداخت گروهی برای ${customerIds.size} مشتری ثبت شد"
            )
        }
    }

    fun updateTransaction(transaction: PaymentTransaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            _snackBarMessage.emit("تراکنش با موفقیت ویرایش شد.")
        }
    }

    fun deleteTransaction(transaction: PaymentTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _snackBarMessage.emit("تراکنش با موفقیت حذف شد.")
        }
    }

    // Backup & Restore
    fun exportBackup(
        onReady: (jsonString: String, fileName: String) -> Unit
    ) {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("barberg_preferences", 0)
            val data = repository.getAllDataForBackup().copy(preferences = BackupManager.readPreferences(prefs))
            val json = BackupManager.exportToJson(data)
            val todayStr = JalaliCalendar
                .formatShort(JalaliCalendar.today(), false)
                .replace("/", "_")
            val fileName = "barberg_backup_$todayStr.json"

            onReady(json, fileName)
            _snackBarMessage.emit("فایل پشتیبان آماده شد.")
        }
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                val data = BackupManager.parseFromJson(jsonString)
                repository.restoreFromBackup(data)
                BackupManager.writePreferences(
                    getApplication<Application>().getSharedPreferences("barberg_preferences", 0),
                    data.preferences
                )

                _snackBarMessage.emit("اطلاعات با موفقیت بازیابی شد (${data.customers.size} مشتری، ${data.transactions.size} تراکنش، ${data.smsHistory.size} سابقه پیامک). برای اعمال تنظیمات ظاهری، برنامه را یک بار ببندید و دوباره باز کنید.")
            } catch (e: Exception) {
                e.printStackTrace()
                _snackBarMessage.emit("خطا در بازیابی اطلاعات: ساختار فایل نامعتبر است")
            }
        }
    }

    // Export CSV
    fun exportReportCsv(
        reportType: String,
        onReady: (csvContent: String, fileName: String) -> Unit
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val summaries = customerAccountSummaries.value
            val unit = state.currencyUnit
            val year = state.selectedYear
            val month = state.selectedMonth

            val todayStr = JalaliCalendar
                .formatShort(JalaliCalendar.today(), false)
                .replace("/", "_")

            val (content, fileName) = when (reportType) {
                "MONTHLY" -> {
                    val csv = CsvExporter.generateMonthlyReportCsv(
                        year,
                        month,
                        summaries,
                        unit
                    )
                    Pair(
                        csv,
                        "barberg_monthly_${year}_${month}_$todayStr.csv"
                    )
                }

                "ANNUAL" -> {
                    val csv = CsvExporter.generateAnnualMatrixCsv(
                        year,
                        summaries,
                        unit
                    )
                    Pair(
                        csv,
                        "barberg_annual_matrix_${year}_$todayStr.csv"
                    )
                }

                "DEBTORS" -> {
                    val csv = CsvExporter.generateDebtorsCsv(
                        summaries,
                        unit
                    )
                    Pair(csv, "barberg_debtors_$todayStr.csv")
                }

                "TRANSACTIONS" -> {
                    val customerNames = customers.value.associate {
                        it.id to it.name
                    }
                    val csv = CsvExporter.generateTransactionsCsv(
                        transactions.value,
                        customerNames,
                        unit
                    )
                    Pair(
                        csv,
                        "barberg_transactions_$todayStr.csv"
                    )
                }

                else -> Pair("", "report.csv")
            }

            onReady(content, fileName)
        }
    }
}



