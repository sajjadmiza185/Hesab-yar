package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import android.Manifest
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.app.PendingIntent
import android.app.Activity
import android.os.Build
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import com.example.ui.components.BarbergCard as Card
import androidx.compose.material3.CardDefaults
import com.example.ui.theme.LocalBarbarg3D
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.BarbergOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Customer
import com.example.model.SmsHistory
import com.example.model.CustomerAccountSummary
import com.example.model.CustomerGroup
import com.example.ui.components.CurrencyInputField
import com.example.util.JalaliCalendar
import com.example.ui.theme.BarbargError
import com.example.ui.theme.BarbargSuccess
import com.example.ui.viewmodel.BarbargViewModel
import com.example.util.CurrencyFormatter
import com.example.util.CustomerDocumentImporter
import com.example.util.ImportedCustomer
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
private const val ACTION_BARBARG_SMS_SENT = "com.example.BARBARG_SMS_SENT"
private const val ACTION_BARBARG_SMS_DELIVERED = "com.example.BARBARG_SMS_DELIVERED"
private const val EXTRA_SMS_BATCH_ID = "barberg_sms_batch_id"
private const val EXTRA_SMS_HISTORY_ID = "barberg_sms_history_id"
private data class PreparedSms(
    val item: CustomerAccountSummary,
    val phone: String,
    val message: String,
    val parts: List<String>,
    val historyId: Long
)

@Composable
fun CustomersScreen(
    viewModel: BarbargViewModel,
    onOpenCustomerDetail: (Long) -> Unit,
    onOpenPaymentDialog: (customerId: Long) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summaries by viewModel.customerAccountSummaries.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    // فهرست کامل مشتریان برای مدیریت اعضای گروه
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var showGroupManagementDialog by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedCustomerIds by remember { mutableStateOf(setOf<Long>()) }
    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }
    var showBulkSmsDialog by remember { mutableStateOf(false) }
    var showGroupLeaderPicker by remember { mutableStateOf(false) }
    var groupLeaderCandidateId by remember { mutableStateOf<Long?>(null) }
    var activeSmsSubscriptions by remember { mutableStateOf<List<SubscriptionInfo>>(emptyList()) }
    var selectedSmsSubscriptionId by remember { mutableStateOf<Int?>(null) }
    var bulkSmsSending by remember { mutableStateOf(false) }
    var activeSmsBatchId by remember { mutableStateOf(0L) }
    var smsExpectedCallbacks by remember { mutableStateOf(0) }
    var smsReceivedCallbacks by remember { mutableStateOf(0) }
    var smsSentParts by remember { mutableStateOf(0) }
    var smsFailedParts by remember { mutableStateOf(0) }
    var smsDeliveredParts by remember { mutableStateOf(0) }
    var smsDeliveryFailedParts by remember { mutableStateOf(0) }
    var smsImmediateFailures by remember { mutableStateOf(0) }
    var showMissingSmsChargeWarning by remember { mutableStateOf(false) }
    var pendingCustomerSave by remember { mutableStateOf<CustomerDraft?>(null) }
    var duplicateWarning by remember { mutableStateOf("") }
    var importedCustomers by remember { mutableStateOf<List<ImportedCustomer>>(emptyList()) }
    var importFileName by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf("") }
    var showImportPreview by remember { mutableStateOf(false) }
    var importBusy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(ACTION_BARBARG_SMS_SENT)
            addAction(ACTION_BARBARG_SMS_DELIVERED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent?) {
                if (intent == null) return
                val historyId = intent.getLongExtra(EXTRA_SMS_HISTORY_ID, -1L)
                val callbackOk = resultCode == Activity.RESULT_OK

                if (historyId > 0L) {
                    when (intent.action) {
                        ACTION_BARBARG_SMS_SENT -> viewModel.recordSmsSentPart(
                            id = historyId,
                            succeeded = if (callbackOk) 1 else 0,
                            failed = if (callbackOk) 0 else 1,
                            errorReason = if (callbackOk) "" else "ارسال این بخش پیامک ناموفق بود"
                        )
                        ACTION_BARBARG_SMS_DELIVERED -> viewModel.recordSmsDeliveryPart(
                            id = historyId,
                            succeeded = if (callbackOk) 1 else 0,
                            failed = if (callbackOk) 0 else 1
                        )
                    }
                }

                val callbackBatchId = intent.getLongExtra(EXTRA_SMS_BATCH_ID, -1L)
                if (callbackBatchId != activeSmsBatchId) return

                if (intent.action == ACTION_BARBARG_SMS_DELIVERED) {
                    if (resultCode == Activity.RESULT_OK) {
                        smsDeliveredParts += 1
                        Toast.makeText(context, "گزارش شبکه: تحویل پیامک تأیید شد.", Toast.LENGTH_SHORT).show()
                    } else {
                        smsDeliveryFailedParts += 1
                    }
                    return
                }
                if (intent.action != ACTION_BARBARG_SMS_SENT || !bulkSmsSending) return

                smsReceivedCallbacks += 1
                if (resultCode == Activity.RESULT_OK) smsSentParts += 1 else smsFailedParts += 1

                if (smsReceivedCallbacks >= smsExpectedCallbacks) {
                    bulkSmsSending = false
                    showBulkSmsDialog = false
                    val sent = smsSentParts
                    val failedParts = smsFailedParts
                    val immediateFailed = smsImmediateFailures
                    Toast.makeText(
                        context,
                        "نتیجهٔ ارسال: $sent بخش پیامک با موفقیت ارسال شد؛ $failedParts بخش ناموفق؛ $immediateFailed مورد بدون شماره یا خطای فوری. تحویل نهایی تضمین نمی‌شود.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    fun prepareBulkSms() {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        val subscriptions = runCatching {
            subscriptionManager?.activeSubscriptionInfoList.orEmpty()
        }.getOrDefault(emptyList())
        if (subscriptions.isEmpty()) {
            Toast.makeText(context, "سیم‌کارت فعالی پیدا نشد؛ سیم‌کارت و مجوز دسترسی به تلفن را بررسی کن.", Toast.LENGTH_LONG).show()
            return
        }
        activeSmsSubscriptions = subscriptions.sortedBy { it.simSlotIndex }
        selectedSmsSubscriptionId = activeSmsSubscriptions.firstOrNull()?.subscriptionId
        showBulkSmsDialog = true
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val smsGranted = results[Manifest.permission.SEND_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val phoneGranted = results[Manifest.permission.READ_PHONE_STATE] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (smsGranted && phoneGranted) prepareBulkSms()
        else Toast.makeText(context, "برای ارسال گروهی، مجوز پیامک و شناسایی سیم‌کارت لازم است.", Toast.LENGTH_LONG).show()
    }

    fun requestBulkSms() {
        val missing = listOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE)
            .filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) prepareBulkSms() else smsPermissionLauncher.launch(missing.toTypedArray())
    }

    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importBusy = true
            val result = runCatching {
                val displayName = CustomerDocumentImporter.getDisplayName(context, uri)
                val rows = context.contentResolver.openInputStream(uri)?.use { input ->
                    CustomerDocumentImporter.parse(input, displayName)
                } ?: error("خواندن فایل ممکن نشد")
                displayName to rows
            }
            importBusy = false
            result.onSuccess { (name, rows) ->
                if (rows.isEmpty()) {
                    importError = "هیچ ردیف مشتری قابل شناسایی در فایل پیدا نشد. ستون نام مشتری را بررسی کن."
                } else {
                    importFileName = name
                    importedCustomers = rows
                    importError = ""
                    showImportPreview = true
                }
            }.onFailure { error ->
                importError = "خواندن فایل ناموفق بود: ${error.message ?: "ساختار فایل پشتیبانی نمی‌شود"}"
            }
        }
    }

    // فیلتر مشتریان بر اساس جستجو، گروه و بدهکار بودن
    val filteredList = remember(
        summaries,
        uiState.customerSearchQuery,
        uiState.selectedGroupFilterId,
        uiState.debtorOnlyFilter
    ) {
        summaries.filter { item ->
            val matchQuery =
                if (uiState.customerSearchQuery.isBlank()) {
                    true
                } else {
                    val query = uiState.customerSearchQuery.trim()

                    item.customer.name.contains(
                        query,
                        ignoreCase = true
                    ) ||
                        item.customer.phone.contains(query) ||
                        item.customer.address.contains(
                            query,
                            ignoreCase = true
                        )
                }

            val matchGroup =
                uiState.selectedGroupFilterId == null ||
                    item.customer.groupId == uiState.selectedGroupFilterId

            val matchDebtor =
                !uiState.debtorOnlyFilter || item.balance > 0

            matchQuery && matchGroup && matchDebtor
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    customerToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "افزودن مشتری جدید"
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("customers_screen")
        ) {
            // جستجوی مشتریان
            OutlinedTextField(
                value = uiState.customerSearchQuery,
                onValueChange = {
                    viewModel.setCustomerSearchQuery(it)
                },
                placeholder = {
                    Text("جستجوی نام مشتری، تلفن، آدرس...")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (uiState.customerSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.setCustomerSearchQuery("")
                            }
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "پاک کردن"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("customer_search_input")
            )

            // ورود گروهی از فایل Excel یا Word؛ تا زمان تأیید پیش‌نمایش هیچ داده‌ای ذخیره نمی‌شود.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { importFilePicker.launch(arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/msword"
                    )) },
                    enabled = !importBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (importBusy) "در حال خواندن فایل…" else "ورود گروهی از Excel / Word")
                }
            }
            if (importError.isNotBlank()) {
                Text(
                    text = importError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // ابزارهای انتخاب گروهی مشتریان
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = {
                    selectionMode = !selectionMode
                    selectedCustomerIds = emptySet()
                }, modifier = Modifier.weight(1f)) {
                    Text(if (selectionMode) "لغو انتخاب" else "انتخاب چند مشتری")
                }
                OutlinedButton(
                    onClick = {
                        if (selectedCustomerIds.isEmpty()) {
                            selectionMode = true
                        } else {
                            requestBulkSms()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("پیامک گروهی")
                }
                if (selectionMode) {
                    OutlinedButton(onClick = {
                        selectedCustomerIds = if (selectedCustomerIds.size == filteredList.size) emptySet()
                        else filteredList.map { it.customer.id }.toSet()
                    }, modifier = Modifier.weight(1f)) {
                        Text(if (filteredList.isNotEmpty() && selectedCustomerIds.size == filteredList.size) "لغو همه" else "انتخاب همه")
                    }
                }
            }
            if (selectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${selectedCustomerIds.size} مشتری انتخاب شده")
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {

                        TextButton(
                            enabled = selectedCustomerIds.isNotEmpty(),
                            onClick = { showBulkDeleteConfirmation = true }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = BarbargError)
                            Spacer(Modifier.width(3.dp))
                            Text("حذف", color = BarbargError)
                        }
                    }
                }
            }

            // مدیریت گروه‌ها
            OutlinedButton(
                onClick = {
                    showGroupManagementDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("مدیریت گروه‌های مشتریان (${groups.size})")
            }

            // فیلتر گروه و بدهکاران
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedGroupFilterId == null &&
                            !uiState.debtorOnlyFilter,
                        onClick = {
                            viewModel.setGroupFilter(null)

                            if (uiState.debtorOnlyFilter) {
                                viewModel.toggleDebtorOnlyFilter()
                            }
                        },
                        label = {
                            Text("همه (${summaries.size})")
                        }
                    )
                }

                item {
                    val debtorCount = summaries.count {
                        it.balance > 0
                    }

                    FilterChip(
                        selected = uiState.debtorOnlyFilter,
                        onClick = {
                            viewModel.toggleDebtorOnlyFilter()
                        },
                        label = {
                            Text("فقط بدهکاران ($debtorCount)")
                        }
                    )
                }

                items(groups, key = { it.id }) { group ->
                    val isSelected =
                        uiState.selectedGroupFilterId == group.id

                    val groupCount = summaries.count {
                        it.customer.groupId == group.id
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setGroupFilter(
                                if (isSelected) null else group.id
                            )
                        },
                        label = {
                            Text("${group.name} ($groupCount)")
                        }
                    )
                }
            }

            if (uiState.selectedGroupFilterId != null) {
                val activeGroup = groups.firstOrNull { it.id == uiState.selectedGroupFilterId }
                val groupMembers = summaries.filter { it.customer.groupId == uiState.selectedGroupFilterId }
                OutlinedButton(
                    onClick = {
                        groupLeaderCandidateId = groupMembers.firstOrNull()?.customer?.id
                        showGroupLeaderPicker = true
                    },
                    enabled = groupMembers.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ارسال پیامک مبلغ ماهانه به سرگروه${activeGroup?.name?.let { " گروه «$it»" } ?: ""}")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // فهرست مشتریان
            if (filteredList.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (summaries.isEmpty()) {
                            "هیچ مشتری ثبت نشده است.\nبرای شروع، دکمه + را لمس کنید."
                        } else {
                            "هیچ مشتری با این مشخصات یافت نشد."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        filteredList,
                        key = { it.customer.id }
                    ) { item ->
                        CustomerCard(
                            item = item,
                            selectionMode = selectionMode,
                            isSelected = item.customer.id in selectedCustomerIds,
                            onToggleSelected = {
                                selectedCustomerIds = if (item.customer.id in selectedCustomerIds) {
                                    selectedCustomerIds - item.customer.id
                                } else selectedCustomerIds + item.customer.id
                            },
                            currencyUnit = uiState.currencyUnit,
                            usePersianDigits = uiState.usePersianDigits,
                            currentMonthCharge = if (uiState.selectedYear == JalaliCalendar.today().year) (item.monthlyCharges[JalaliCalendar.today().month] ?: 0L) else 0L,
                            hasCurrentMonthCharge = uiState.selectedYear == JalaliCalendar.today().year && item.monthlyCharges.containsKey(JalaliCalendar.today().month),
                            onClick = {
                                onOpenCustomerDetail(item.customer.id)
                            },
                            onPayClick = {
                                onOpenPaymentDialog(item.customer.id)
                            },
                            onEditClick = {
                                customerToEdit = item.customer
                                showAddEditDialog = true
                            },
                            onDeleteClick = {
                                customerToDelete = item.customer
                            }
                        )
                    }
                }
            }
        }
    }

    // پیش‌نمایش و تأیید ورود گروهی
    if (showImportPreview) {
        val duplicateRows = importedCustomers.filterIndexed { index, row ->
            val matchesExisting = customers.any { existing ->
                (row.name.isNotBlank() && existing.name.normalizeCustomerKey() == row.name.normalizeCustomerKey()) ||
                    (row.phone.filter { it.isDigit() }.isNotBlank() && existing.phone.filter { it.isDigit() } == row.phone.filter { it.isDigit() })
            }
            val matchesEarlierImport = importedCustomers.take(index).any { earlier ->
                (row.name.isNotBlank() && earlier.name.normalizeCustomerKey() == row.name.normalizeCustomerKey()) ||
                    (row.phone.filter { it.isDigit() }.isNotBlank() && earlier.phone.filter { it.isDigit() } == row.phone.filter { it.isDigit() })
            }
            matchesExisting || matchesEarlierImport
        }
        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { showImportPreview = false },
            title = { Text("پیش‌نمایش ورود مشتریان") },
            text = {
                Column {
                    Text("فایل: $importFileName")
                    Text("تعداد ردیف‌های قابل ورود: ${importedCustomers.size}")
                    Text("مواردی که با مشتریان فعلی نام یا تلفن مشابه دارند: ${duplicateRows.size}")
                    Spacer(Modifier.height(8.dp))
                    Text("نمونه داده‌ها:")
                    importedCustomers.take(8).forEachIndexed { index, row ->
                        val isDuplicate = row in duplicateRows
                        Text("${index + 1}. ${row.name}  ${row.phone}" + if (isDuplicate) "  ⚠ تکراری احتمالی" else "")
                    }
                    if (importedCustomers.size > 8) Text("و ${importedCustomers.size - 8} ردیف دیگر…")
                    Spacer(Modifier.height(8.dp))
                    Text("هیچ مشتری تا انتخاب یکی از دکمه‌های افزودن ثبت نمی‌شود.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val duplicateIndexes = importedCustomers.indices.filter { index ->
                        val row = importedCustomers[index]
                        customers.any { existing ->
                            (row.name.isNotBlank() && existing.name.normalizeCustomerKey() == row.name.normalizeCustomerKey()) ||
                                (row.phone.filter { it.isDigit() }.isNotBlank() && existing.phone.filter { it.isDigit() } == row.phone.filter { it.isDigit() })
                        } || importedCustomers.take(index).any { earlier ->
                            (row.name.isNotBlank() && earlier.name.normalizeCustomerKey() == row.name.normalizeCustomerKey()) ||
                                (row.phone.filter { it.isDigit() }.isNotBlank() && earlier.phone.filter { it.isDigit() } == row.phone.filter { it.isDigit() })
                        }
                    }.toSet()
                    val nonDuplicates = importedCustomers.filterIndexed { index, _ -> index !in duplicateIndexes }
                    viewModel.importCustomers(nonDuplicates)
                    showImportPreview = false
                    importedCustomers = emptyList()
                }, enabled = importedCustomers.size > duplicateRows.size) {
                    Text("افزودن بدون موارد تکراری")
                }
            },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (duplicateRows.isNotEmpty()) {
                        TextButton(onClick = {
                            viewModel.importCustomers(importedCustomers)
                            showImportPreview = false
                            importedCustomers = emptyList()
                        }) { Text("افزودن همه با تأیید تکراری‌ها") }
                    }
                    TextButton(onClick = { showImportPreview = false }) { Text("انصراف") }
                }
            }
        )
    }

    // مدیریت گروه‌ها و تخصیص چند مشتری به گروه
    if (showGroupManagementDialog) {
        GroupManagementDialog(
            groups = groups,
            customers = customers,
            onDismiss = {
                showGroupManagementDialog = false
            },
            onSaveGroup = { id, name, description, color, leaderCustomerId ->
                viewModel.saveGroup(
                    id,
                    name,
                    description,
                    color,
                    leaderCustomerId
                )
            },
            onDeleteGroup = { group ->
                viewModel.deleteGroup(group)
            },
            onAssignCustomers = { group, customerIds ->
                viewModel.setGroupMembers(
                    group.id,
                    customerIds
                )
            }
        )
    }

    // افزودن یا ویرایش مشتری
    if (showAddEditDialog) {
        CustomerDialog(
            customer = customerToEdit,
            groups = groups,
            selectedMonth = uiState.selectedMonth,
            selectedYear = uiState.selectedYear,
            currencyUnit = uiState.currencyUnit,
            usePersianDigits = uiState.usePersianDigits,
            initialMonthlyAmount = if (customerToEdit != null) summaries.firstOrNull { it.customer.id == customerToEdit!!.id }?.monthlyCharges?.get(uiState.selectedMonth) ?: 0L else 0L,
            onDismiss = {
                showAddEditDialog = false
            },
            onSave = { id, name, phone, groupId, address, notes, monthlyAmount, username, passwordEncrypted ->
                val draft = CustomerDraft(
                    id, name, phone, groupId, address, notes,
                    monthlyAmount,
                    username, passwordEncrypted
                )
                val nameKey = name.trim().normalizeCustomerKey()
                val phoneKey = phone.filter { it.isDigit() }
                val duplicate = customers.firstOrNull { existing ->
                    existing.id != id && (
                        (nameKey.isNotBlank() && existing.name.normalizeCustomerKey() == nameKey) ||
                        (phoneKey.isNotBlank() && existing.phone.filter { it.isDigit() } == phoneKey)
                    )
                }
                showAddEditDialog = false
                if (duplicate != null) {
                    pendingCustomerSave = draft
                    duplicateWarning = buildString {
                        append("مشتری مشابهی قبلاً ثبت شده است: «${duplicate.name}»")
                        if (duplicate.phone.isNotBlank()) append("، تلفن ${duplicate.phone}")
                        append(". آیا با وجود تکراری بودن، مشتری جدید ثبت شود؟")
                    }
                } else {
                    viewModel.saveCustomer(
                        id, name, phone, groupId, address, notes,
                        username = draft.username,
                        passwordEncrypted = draft.passwordEncrypted,
                        monthlyAmount = draft.monthlyAmount,
                        monthlyYear = uiState.selectedYear,
                        monthlyMonth = uiState.selectedMonth
                    )
                }
            }
        )
    }

    // هشدار تکراری بودن؛ تصمیم نهایی با کاربر است
    if (pendingCustomerSave != null) {
        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { pendingCustomerSave = null },
            title = { Text("هشدار: مشتری تکراری") },
            text = { Text(duplicateWarning) },
            confirmButton = {
                TextButton(onClick = {
                    pendingCustomerSave?.let { draft ->
                        viewModel.saveCustomer(
                            draft.id, draft.name, draft.phone, draft.groupId, draft.address, draft.notes,
                            username = draft.username,
                            passwordEncrypted = draft.passwordEncrypted,
                            monthlyAmount = draft.monthlyAmount,
                            monthlyYear = uiState.selectedYear,
                            monthlyMonth = uiState.selectedMonth
                        )
                    }
                    pendingCustomerSave = null
                    duplicateWarning = ""
                }) { Text("با این حال ثبت کن") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingCustomerSave = null
                    duplicateWarning = ""
                }) { Text("صرف‌نظر") }
            }
        )
    }

    if (showGroupLeaderPicker) {
        val groupMembers = summaries.filter { it.customer.groupId == uiState.selectedGroupFilterId }
        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { showGroupLeaderPicker = false },
            title = { Text("انتخاب سرگروه برای پیامک") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("فقط به یک نفر از اعضای این گروه پیامک ارسال می‌شود. سرگروه را انتخاب کن:")
                    groupMembers.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { groupLeaderCandidateId = item.customer.id },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = groupLeaderCandidateId == item.customer.id,
                                onClick = { groupLeaderCandidateId = item.customer.id }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.customer.name, fontWeight = FontWeight.SemiBold)
                                Text(item.customer.phone.ifBlank { "بدون شماره موبایل" }, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = groupLeaderCandidateId != null && groupMembers.any { it.customer.id == groupLeaderCandidateId },
                    onClick = {
                        val leaderId = groupLeaderCandidateId ?: return@TextButton
                        selectedCustomerIds = setOf(leaderId)
                        selectionMode = false
                        showGroupLeaderPicker = false
                        requestBulkSms()
                    }
                ) { Text("ادامه و تنظیم پیامک") }
            },
            dismissButton = {
                TextButton(onClick = { showGroupLeaderPicker = false }) { Text("انصراف") }
            }
        )
    }

    if (showBulkSmsDialog) {
        val selectedCustomers = summaries.filter { it.customer.id in selectedCustomerIds }
        // در حالت پیامک سرگروه، فقط خود سرگروه گیرنده است؛ مبلغ از مجموع شارژ اعضای فعال گروه محاسبه می‌شود.
        val groupSmsMode = uiState.selectedGroupFilterId != null &&
            selectedCustomers.size == 1 &&
            selectedCustomers.first().customer.groupId == uiState.selectedGroupFilterId
        val groupSmsTotal = if (groupSmsMode) {
            summaries.asSequence()
                .filter {
                    it.customer.isActive &&
                        it.customer.groupId == uiState.selectedGroupFilterId
                }
                .sumOf { it.monthlyCharges[uiState.selectedMonth] ?: 0L }
        } else {
            0L
        }
        fun smsAmountFor(item: CustomerAccountSummary): Long =
            if (groupSmsMode) groupSmsTotal
            else item.monthlyCharges[uiState.selectedMonth] ?: 0L

        val cardPreferences = remember(context) {
            context.getSharedPreferences("barberg_preferences", Context.MODE_PRIVATE)
        }
        val bankCards = remember(context) {
            runCatching {
                val array = JSONArray(cardPreferences.getString("bank_cards_json", "[]"))
                (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val title = item.optString("title").trim()
                    val number = item.optString("number").filter { it.isDigit() }
                    if (title.isNotBlank() && number.isNotBlank()) title to number else null
                }
            }.getOrDefault(emptyList<Pair<String, String>>())
        }
        var selectedBankCardNumber by remember(context, bankCards) {
            val saved = cardPreferences.getString("selected_bank_card_number", "").orEmpty()
            mutableStateOf(if (bankCards.any { it.second == saved }) saved else bankCards.firstOrNull()?.second.orEmpty())
        }
        var smsTemplate by remember {
            mutableStateOf("سلام {نام}، مبلغ ماهانه شما برای ماه {ماه} {سال} برابر {مبلغ} {واحد} است.\nشماره کارت: {کارت}\nبا تشکر، باربرگ")
        }

        fun buildSmsMessage(item: CustomerAccountSummary): String {
            val amount = smsAmountFor(item)
            val monthName = JalaliCalendar.getMonthName(uiState.selectedMonth)
            val formattedAmount = CurrencyFormatter.formatNumber(amount, uiState.usePersianDigits)
            val yearText = if (uiState.usePersianDigits) {
                CurrencyFormatter.toPersianDigits(uiState.selectedYear.toString())
            } else {
                uiState.selectedYear.toString()
            }
            var message = smsTemplate
                .replace("{نام}", item.customer.name)
                .replace("{ماه}", monthName)
                .replace("{سال}", yearText)
                .replace("{مبلغ}", formattedAmount)
                .replace("{واحد}", uiState.currencyUnit)
                .replace("{کارت}", selectedBankCardNumber)

            // حتی اگر کاربر متغیر مبلغ را از الگو حذف کرده باشد، مبلغ ماهانه در پیام نهایی درج می‌شود.
            if (!smsTemplate.contains("{مبلغ}")) {
                message = message.trimEnd() + "\nمبلغ ماهانه: $formattedAmount ${uiState.currencyUnit}"
            }
            // شماره کارت انتخاب‌شده هم در همه پیام‌ها درج می‌شود؛ چه متغیر آن در الگو باشد چه نباشد.
            if (selectedBankCardNumber.isNotBlank() && !smsTemplate.contains("{کارت}")) {
                message = message.trimEnd() + "\nشماره کارت: $selectedBankCardNumber"
            }
            return message
        }

        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { if (!bulkSmsSending) showBulkSmsDialog = false },
            title = { Text(if (groupSmsMode) "ارسال پیامک مبلغ ماهانه به سرگروه" else "ارسال گروهی پیامک مبلغ ماهانه") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (groupSmsMode) {
                            "یک پیامک فقط به سرگروه ارسال می‌شود؛ مبلغ، مجموع شارژ ماهانه اعضای فعال گروه است."
                        } else {
                            "برای ${selectedCustomers.size} مشتری انتخاب‌شده پیامک مبلغ ماهانه ماه ${JalaliCalendar.getMonthName(uiState.selectedMonth)} ${uiState.selectedYear} ارسال می‌شود."
                        }
                    )
                    OutlinedTextField(
                        value = smsTemplate,
                        onValueChange = { smsTemplate = it },
                        label = { Text("متن پیامک (قابل ویرایش)") },
                        supportingText = { Text("متغیرها: {نام}، {ماه}، {سال}، {مبلغ}، {واحد}، {کارت}") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("کارت بانکی برای پیامک:", fontWeight = FontWeight.Bold)
                    if (bankCards.isEmpty()) {
                        Text("ابتدا از تنظیمات ← تنظیمات مالی، یک کارت بانکی ثبت کن.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    } else {
                        bankCards.forEach { card ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedBankCardNumber = card.second
                                    cardPreferences.edit().putString("selected_bank_card_number", card.second).apply()
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedBankCardNumber == card.second,
                                    onClick = {
                                        selectedBankCardNumber = card.second
                                        cardPreferences.edit().putString("selected_bank_card_number", card.second).apply()
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(card.first, fontWeight = FontWeight.SemiBold)
                                    Text(card.second, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    Text("سیم‌کارت ارسال:", fontWeight = FontWeight.Bold)
                    activeSmsSubscriptions.forEach { subscription ->
                        val label = "سیم‌کارت ${subscription.simSlotIndex + 1}" +
                            if (subscription.displayName?.toString()?.isNotBlank() == true) " — ${subscription.displayName}" else ""
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = selectedSmsSubscriptionId == subscription.subscriptionId,
                                onClick = { selectedSmsSubscriptionId = subscription.subscriptionId }
                            )
                            Text(label, modifier = Modifier.clickable { selectedSmsSubscriptionId = subscription.subscriptionId })
                        }
                    }
                    Text("پیش‌نمایش کامل پیام هر مشتری:", fontWeight = FontWeight.SemiBold)
                    selectedCustomers.forEach { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("گیرنده: ${item.customer.name} — ${item.customer.phone.ifBlank { "بدون شماره" }}", fontWeight = FontWeight.SemiBold)
                                Text(buildSmsMessage(item), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Text("پیامک ممکن است هزینه داشته باشد. قبل از ارسال، شماره‌ها و مبلغ‌ها را بررسی کن.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !bulkSmsSending && selectedSmsSubscriptionId != null && selectedCustomers.isNotEmpty() && smsTemplate.isNotBlank() && selectedBankCardNumber.isNotBlank(),
                    onClick = {
                        if (selectedBankCardNumber.isBlank()) {
                            Toast.makeText(context, "ابتدا یک کارت بانکی را در تنظیمات مالی ثبت و انتخاب کن.", Toast.LENGTH_LONG).show()
                            return@TextButton
                        }
                        val missingChargeCustomers = selectedCustomers.filter {
                            smsAmountFor(it) <= 0L
                        }
                        if (missingChargeCustomers.isNotEmpty()) {
                            showMissingSmsChargeWarning = true
                            return@TextButton
                        }
                        val subId = selectedSmsSubscriptionId ?: return@TextButton
                        val smsManager = runCatching { SmsManager.getSmsManagerForSubscriptionId(subId) }.getOrNull()
                        if (smsManager == null) {
                            Toast.makeText(context, "دسترسی به سرویس پیامک ممکن نشد.", Toast.LENGTH_LONG).show()
                            return@TextButton
                        }

                        coroutineScope.launch {
                            val batchId = viewModel.createSmsSendBatchAndReturn(
                                title = "ارسال گروهی"
                            )
                            activeSmsBatchId = batchId
                            smsExpectedCallbacks = 0
                            smsReceivedCallbacks = 0
                            smsSentParts = 0
                            smsFailedParts = 0
                            smsDeliveredParts = 0
                            smsDeliveryFailedParts = 0
                            smsImmediateFailures = 0
                            bulkSmsSending = true

                            val preparedMessages = mutableListOf<PreparedSms>()

                            selectedCustomers.forEach { item ->
                                val phone = item.customer.phone.trim()
                                val message = buildSmsMessage(item)
                                val parts = if (phone.isBlank()) emptyList() else
                                    runCatching { smsManager.divideMessage(message).toList() }
                                        .getOrDefault(emptyList())

                                val error = when {
                                    phone.isBlank() -> "شماره تلفن مشتری ثبت نشده است"
                                    parts.isEmpty() -> "متن پیامک قابل آماده‌سازی نبود"
                                    else -> ""
                                }
                                val invalid = error.isNotBlank()

                                val historyId = viewModel.insertSmsHistoryAndReturn(
                                    SmsHistory(
                                        customerId = item.customer.id,
                                        customerName = item.customer.name,
                                        phoneNumber = phone,
                                        message = message,
                                        amount = smsAmountFor(item),
                                        batchId = batchId,
                                        simSubscriptionId = subId,
                                        expectedParts = maxOf(parts.size, 1),
                                        status = if (invalid) "FAILED" else "PENDING",
                                        failedSentParts = if (invalid) 1 else 0,
                                        errorReason = error
                                    )
                                )

                                preparedMessages += PreparedSms(
                                    item = item,
                                    phone = phone,
                                    message = message,
                                    parts = parts,
                                    historyId = historyId
                                )
                            }

                            smsExpectedCallbacks = preparedMessages.sumOf {
                                if (it.phone.isBlank() || it.parts.isEmpty()) 0 else it.parts.size
                            }
                            smsImmediateFailures = preparedMessages.count {
                                it.phone.isBlank() || it.parts.isEmpty()
                            }

                            preparedMessages.forEachIndexed { index, prepared ->
                                val phone = prepared.phone
                                val parts = prepared.parts
                                if (phone.isBlank() || parts.isEmpty()) return@forEachIndexed

                                val sentIntents = ArrayList<PendingIntent>(parts.size)
                                val deliveryIntents = ArrayList<PendingIntent>(parts.size)

                                parts.indices.forEach { partIndex ->
                                    val callbackIntent = Intent(ACTION_BARBARG_SMS_SENT)
                                        .setPackage(context.packageName)
                                        .putExtra(EXTRA_SMS_BATCH_ID, batchId)
                                        .putExtra(EXTRA_SMS_HISTORY_ID, prepared.historyId)

                                    val requestCode =
                                        ((batchId % 1_000_000L).toInt() * 31 +
                                            index * 101 + partIndex) and 0x7fffffff
                                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            PendingIntent.FLAG_IMMUTABLE
                                        } else 0

                                    sentIntents += PendingIntent.getBroadcast(
                                        context, requestCode, callbackIntent, flags
                                    )

                                    val deliveryIntent = Intent(ACTION_BARBARG_SMS_DELIVERED)
                                        .setPackage(context.packageName)
                                        .putExtra(EXTRA_SMS_BATCH_ID, batchId)
                                        .putExtra(EXTRA_SMS_HISTORY_ID, prepared.historyId)
                                    val deliveryRequestCode =
                                        (requestCode + 50_000_003) and 0x7fffffff

                                    deliveryIntents += PendingIntent.getBroadcast(
                                        context, deliveryRequestCode, deliveryIntent, flags
                                    )
                                }

                                val sent = runCatching {
                                    if (parts.size > 1) {
                                        smsManager.sendMultipartTextMessage(
                                            phone, null, ArrayList(parts),
                                            sentIntents, deliveryIntents
                                        )
                                    } else {
                                        smsManager.sendTextMessage(
                                            phone, null, parts.first(),
                                            sentIntents.first(), deliveryIntents.first()
                                        )
                                    }
                                }

                                if (sent.isFailure) {
                                    viewModel.recordSmsSentPart(
                                        id = prepared.historyId,
                                        succeeded = 0,
                                        failed = parts.size,
                                        errorReason = sent.exceptionOrNull()?.message
                                            ?: "شروع ارسال پیامک ناموفق بود"
                                    )
                                    smsImmediateFailures += 1
                                    smsExpectedCallbacks =
                                        (smsExpectedCallbacks - parts.size).coerceAtLeast(0)
                                }
                            }

                            if (smsExpectedCallbacks == 0) {
                                bulkSmsSending = false
                                showBulkSmsDialog = false
                                Toast.makeText(
                                    context,
                                    "ارسال پیامک شروع نشد. شماره‌ها، مجوز پیامک و سیم‌کارت را بررسی کنید. موارد ناموفق: $smsImmediateFailures",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) { Text(if (bulkSmsSending) "در حال ارسال…" else "تأیید و ارسال") }
            },
            dismissButton = {
                TextButton(enabled = !bulkSmsSending, onClick = { showBulkSmsDialog = false }) { Text("انصراف") }
            }
        )
    }

    if (showMissingSmsChargeWarning) {
        val customersWithoutCharge = summaries.filter {
            it.customer.id in selectedCustomerIds &&
                (it.monthlyCharges[uiState.selectedMonth] ?: 0L) <= 0L
        }
        AlertDialog(
            onDismissRequest = { showMissingSmsChargeWarning = false },
            title = { Text("شارژ ماهانه ثبت نشده") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("برای مشتریان زیر در ماه ${JalaliCalendar.getMonthName(uiState.selectedMonth)} ${uiState.selectedYear} مبلغی ثبت نشده یا مبلغ صفر است:")
                    customersWithoutCharge.forEach { item ->
                        Text("• ${item.customer.name}")
                    }
                    Text("می‌توانی این افراد را از ارسال فعلی کنار بگذاری و سپس برای بقیه ارسال کنی، یا انصراف بدهی.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val excludedIds = customersWithoutCharge.map { it.customer.id }.toSet()
                    selectedCustomerIds = selectedCustomerIds - excludedIds
                    showMissingSmsChargeWarning = false
                    Toast.makeText(context, "افراد بدون شارژ از انتخاب ارسال کنار گذاشته شدند. برای ارسال به بقیه، دوباره «تأیید و ارسال» را بزن.", Toast.LENGTH_LONG).show()
                }) { Text("کنار گذاشتن و ادامه") }
            },
            dismissButton = {
                TextButton(onClick = { showMissingSmsChargeWarning = false }) { Text("انصراف") }
            }
        )
    }

    if (showBulkDeleteConfirmation) {
        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { showBulkDeleteConfirmation = false },
            title = { Text("حذف گروهی مشتریان") },
            text = { Text("آیا از حذف ${selectedCustomerIds.size} مشتری انتخاب‌شده و تمام مبالغ ماهانه و تراکنش‌های مرتبط با آن‌ها مطمئن هستید؟ این عملیات قابل بازگشت نیست.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomers(selectedCustomerIds.toList())
                    selectedCustomerIds = emptySet()
                    selectionMode = false
                    showBulkDeleteConfirmation = false
                }) { Text("حذف قطعی", color = BarbargError) }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirmation = false }) { Text("انصراف") }
            }
        )
    }

    // تأیید حذف مشتری
    customerToDelete?.let { customer ->
        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = {
                customerToDelete = null
            },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = BarbargError
                )
            },
            title = {
                Text("حذف مشتری")
            },
            text = {
                Text(
                    "آیا از حذف مشتری «${customer.name}» و کلیه مبالغ ماهانه " +
                        "و تراکنش‌های مربوط به آن اطمینان دارید؟ " +
                        "این عمل غیرقابل بازگشت است."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        customerToDelete = null
                    }
                ) {
                    Text(
                        "حذف قطعی",
                        color = BarbargError,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        customerToDelete = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun CustomerCard(
    item: CustomerAccountSummary,
    currencyUnit: String,
    usePersianDigits: Boolean,
    currentMonthCharge: Long,
    hasCurrentMonthCharge: Boolean,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelected: () -> Unit = {},
    onClick: () -> Unit,
    onPayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    val statusColor = when {
        !hasCurrentMonthCharge -> Color(0xFF1976D2)
        item.balance > 0 -> Color(0xFFD32F2F)
        else -> Color(0xFF2E7D32)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (LocalBarbarg3D.current) 8.dp else 1.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, statusColor, RoundedCornerShape(16.dp))
            .clickable(onClick = if (selectionMode) onToggleSelected else onClick)
            .testTag("customer_card_${item.customer.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // نام، شماره، گروه و منو
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (selectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = item.customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (item.customer.phone.isNotBlank()) {
                            Text(
                                text = if (usePersianDigits) {
                                    CurrencyFormatter.toPersianDigits(
                                        item.customer.phone
                                    )
                                } else {
                                    item.customer.phone
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.group?.let { group ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 3.dp
                                )
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = {
                                menuExpanded = true
                            }
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "گزینه‌ها"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {
                                menuExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("ویرایش")
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                }
                            )

                            if (item.customer.phone.isNotBlank()) {
                                DropdownMenuItem(
                                    text = {
                                        Text("تماس تلفنی")
                                    },
                                    onClick = {
                                        menuExpanded = false

                                        try {
                                            val intent = Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse(
                                                    "tel:${item.customer.phone}"
                                                )
                                            )
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Call,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "حذف مشتری",
                                        color = BarbargError
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = BarbargError
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // خلاصه مالی
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(
                            text = "کل صورتحساب",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = CurrencyFormatter.formatAmount(
                                currentMonthCharge,
                                currencyUnit,
                                usePersianDigits
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(38.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f))
                    )

                    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(
                            text = "کل دریافتی",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = CurrencyFormatter.formatAmount(
                                item.totalPayments,
                                currencyUnit,
                                usePersianDigits
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BarbargSuccess
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(38.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f))
                    )

                    Column(
                        modifier = Modifier.weight(1f).padding(start = 8.dp, end = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (item.balance > 0) {
                                "مانده بدهی"
                            } else {
                                "وضعیت حساب"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.balance > 0) {
                                BarbargError
                            } else {
                                BarbargSuccess
                            }
                        )

                        Text(
                            text = if (item.balance > 0) {
                                CurrencyFormatter.formatAmount(
                                    item.balance,
                                    currencyUnit,
                                    usePersianDigits
                                )
                            } else if (item.balance < 0) {
                                "بستانکار: " +
                                    CurrencyFormatter.formatAmount(
                                        Math.abs(item.balance),
                                        currencyUnit,
                                        usePersianDigits
                                    )
                            } else {
                                "تسویه"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.balance > 0) {
                                BarbargError
                            } else {
                                BarbargSuccess
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // دکمه‌های عملیات مشتری
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPayClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 4.dp
                    ),
                    modifier = Modifier.testTag(
                        "customer_pay_button_${item.customer.id}"
                    )
                ) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        "ثبت پرداخت",
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
                ) {
                    Text(
                        "جزئیات و مبالغ ماهانه",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private data class CustomerDraft(
    val id: Long,
    val name: String,
    val phone: String,
    val groupId: Long?,
    val address: String,
    val notes: String,
    val monthlyAmount: Long = 0L,
    val username: String = "",
    val passwordEncrypted: String = ""
)

private fun String.normalizeCustomerKey(): String = trim()
    .replace("ي", "ی")
    .replace("ك", "ک")
    .replace(Regex("\\s+"), " ")
    .lowercase()

@Composable
fun CustomerDialog(
    customer: Customer?,
    groups: List<CustomerGroup>,
    selectedMonth: Int,
    selectedYear: Int,
    currencyUnit: String,
    usePersianDigits: Boolean,
    initialMonthlyAmount: Long = 0L,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        phone: String,
        groupId: Long?,
        address: String,
        notes: String,
        monthlyAmount: Long,
                    username: String,
        passwordEncrypted: String
    ) -> Unit
) {
    var name by remember(customer?.id) {
        mutableStateOf(customer?.name ?: "")
    }

    var phone by remember(customer?.id) {
        mutableStateOf(customer?.phone ?: "")
    }

    var username by remember(customer?.id) { mutableStateOf(customer?.username ?: "") }
    var password by remember(customer?.id) { mutableStateOf(customer?.passwordEncrypted ?: "") }

    var selectedGroupId by remember(customer?.id) {
        mutableStateOf(customer?.groupId)
    }

    var address by remember(customer?.id) {
        mutableStateOf(customer?.address ?: "")
    }

    var notes by remember(customer?.id) {
        mutableStateOf(customer?.notes ?: "")
    }
    var monthlyAmount by remember(customer?.id, initialMonthlyAmount) { mutableStateOf(initialMonthlyAmount) }

    AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (customer == null) {
                    "افزودن مشتری جدید"
                } else {
                    "ویرایش مشتری"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("نام مشتری / فروشگاه *")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )

                // شماره تماس: فقط رقم و حداکثر ۱۱ رقم
                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        phone = input
                            .filter { it.isDigit() }
                            .take(11)
                    },
                    label = {
                        Text("شماره موبایل (اختیاری)")
                    },
                    singleLine = true,
                    isError = phone.isNotBlank() && phone.length != 11,
                    supportingText = {
                        Text(
                            when {
                                phone.isBlank() -> "وارد کردن شماره موبایل ضروری نیست"
                                phone.length == 11 -> "شماره تماس کامل است"
                                else -> "شماره تماس باید دقیقاً ۱۱ رقم باشد (${phone.length}/11)"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "گروه مشتری",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("نام کاربری") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("رمز عبور") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedGroupId == null,
                            onClick = {
                                selectedGroupId = null
                            },
                            label = {
                                Text("عمومی / بدون گروه")
                            }
                        )
                    }

                    items(groups, key = { it.id }) { group ->
                        FilterChip(
                            selected = selectedGroupId == group.id,
                            onClick = {
                                selectedGroupId = group.id
                            },
                            label = {
                                Text(group.name)
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                    },
                    label = {
                        Text("آدرس / مسیر")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                        text = "مبلغ ماهانه از ${JalaliCalendar.getMonthName(selectedMonth)} ${CurrencyFormatter.toPersianDigits(selectedYear.toString())} به بعد",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "این مبلغ از ماه انتخاب‌شده تا پایان سال مالی اعمال می‌شود؛ ماه‌های قبل بدون تغییر می‌مانند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyInputField(
                        value = monthlyAmount,
                        onValueChange = { monthlyAmount = it },
                        label = "مبلغ ماهانه",
                        currencyUnit = currencyUnit,
                        usePersianDigits = usePersianDigits,
                        testTag = "customer_monthly_amount_input"
                    )

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                    },
                    label = {
                        Text("توضیحات و یادداشت")
                    },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        customer?.id ?: 0L,
                        name.trim(),
                        phone,
                        selectedGroupId,
                        address,
                        notes,
                        monthlyAmount,
                        username.trim(),
                        password
                    )
                },
                enabled = name.isNotBlank() && (phone.isBlank() || phone.length == 11),
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text(
                    "ذخیره",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}









