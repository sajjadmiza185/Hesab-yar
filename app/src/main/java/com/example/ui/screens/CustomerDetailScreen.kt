package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import com.example.ui.components.BarbergButton as Button
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.BarbergCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import com.example.ui.theme.LocalBarbarg3D
import com.example.ui.theme.LocalBlueGloss3D
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Customer
import com.example.model.PaymentTransaction
import com.example.ui.components.CurrencyInputField
import com.example.ui.theme.BarbargError
import com.example.ui.theme.BarbargSuccess
import com.example.ui.viewmodel.BarbargViewModel
import com.example.util.CurrencyFormatter
import com.example.util.JalaliCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: BarbargViewModel,
    onBack: () -> Unit,
    onEditCustomer: (Customer) -> Unit,
    onOpenPaymentDialog: (customerId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summaries by viewModel.customerAccountSummaries.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    val accountSummary = summaries.find { it.customer.id == customerId }
    val customer = accountSummary?.customer
    val customerTx = transactions.filter { it.customerId == customerId }
    val smsHistory by remember(customerId) {
        viewModel.observeCustomerSmsHistory(customerId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: مبالغ ۱۲ ماهه, 1: تراکنش‌ها و پرداخت‌ها
    var monthToEdit by remember { mutableStateOf<Int?>(null) }
    var txToDelete by remember { mutableStateOf<PaymentTransaction?>(null) }
    var selectedSmsIds by remember(customerId) { mutableStateOf(setOf<Long>()) }
    var confirmSmsDelete by remember { mutableStateOf(false) }

    val unit = uiState.currencyUnit
    val persianDigits = uiState.usePersianDigits

    if (customer == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("مشتری مورد نظر یافت نشد.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) {
                    Text("بازگشت")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (customer.phone.isNotBlank()) {
                        IconButton(onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "تماس")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenPaymentDialog(customerId) },
                containerColor = BarbargSuccess,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 10.dp),
                modifier = Modifier.testTag("customer_detail_pay_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ثبت پرداخت", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("customer_detail_screen")
        ) {
            // Customer Header Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 10.dp else 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                accountSummary.group?.let { g ->
                                    Text(
                                        text = "گروه: ${g.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (customer.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "یادداشت: ${customer.notes}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Debt status badge
                        Surface(
                            color = if (accountSummary.balance > 0) MaterialTheme.colorScheme.errorContainer
                            else BarbargSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (accountSummary.balance > 0) "بدهکار"
                                else if (accountSummary.balance < 0) "بستانکار"
                                else "تسویه شده",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (accountSummary.balance > 0) BarbargError else BarbargSuccess,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (customer.phone.isNotBlank() || customer.address.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (customer.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (persianDigits) CurrencyFormatter.toPersianDigits(customer.phone) else customer.phone,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (customer.address.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = customer.address, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (customer.username.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "نام کاربری: ${customer.username}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (customer.passwordEncrypted.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "رمز عبور: ${customer.passwordEncrypted}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(
                        onClick = { onEditCustomer(customer) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ویرایش اطلاعات مشتری")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 KPI Boxes: Total Charges, Total Paid, Balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("کل صورتحساب", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    CurrencyFormatter.formatAmount(if (uiState.selectedYear == JalaliCalendar.today().year) (accountSummary.monthlyCharges[JalaliCalendar.today().month] ?: 0L) else 0L, unit, persianDigits),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("کل دریافتی", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    CurrencyFormatter.formatAmount(accountSummary.totalPayments, unit, persianDigits),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BarbargSuccess
                                )
                            }
                        }

                        Surface(
                            color = if (accountSummary.balance > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (accountSummary.balance > 0) "مانده بدهی" else "مانده حساب", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    CurrencyFormatter.formatAmount(accountSummary.balance, unit, persianDigits),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (accountSummary.balance > 0) BarbargError else BarbargSuccess
                                )
                            }
                        }
                    }
                }
            }

            // Tab Selector: ۱۲ ماهه vs تراکنش‌ها
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("مبالغ ۱۲ ماهه (${CurrencyFormatter.toPersianDigits(uiState.selectedYear.toString())})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("سوابق پرداخت‌ها (${customerTx.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("سوابق پیامک (${smsHistory.size})") }
                )
            }

            if (selectedTab == 0) {
                // 12 Shamsi Months Table
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items((1..12).toList()) { month ->
                        val chargeAmount = accountSummary.monthlyCharges[month] ?: 0L
                        val paidAmount = accountSummary.monthlyPayments[month] ?: 0L
                        val balance = chargeAmount - paidAmount
                        val monthName = JalaliCalendar.getMonthName(month)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$monthName ${if (persianDigits) CurrencyFormatter.toPersianDigits(uiState.selectedYear.toString()) else uiState.selectedYear.toString()}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(onClick = { monthToEdit = month }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "تغییر مبلغ ماه $monthName",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Divider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    thickness = 1.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("مبلغ کل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            CurrencyFormatter.formatAmount(chargeAmount, unit, persianDigits),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    Divider(
                                        modifier = Modifier.width(1.dp).height(44.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                        thickness = 1.dp
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("پرداختی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            CurrencyFormatter.formatAmount(paidAmount, unit, persianDigits),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BarbargSuccess,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    Divider(
                                        modifier = Modifier.width(1.dp).height(44.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                        thickness = 1.dp
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("بدهی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = when {
                                                balance > 0 -> CurrencyFormatter.formatAmount(balance, unit, persianDigits)
                                                chargeAmount > 0 && balance == 0L -> "تسویه"
                                                else -> "—"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (balance > 0) BarbargError else BarbargSuccess,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // Transactions History
                if (customerTx.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هیچ پرداختی برای این مشتری ثبت نشده است.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(customerTx) { tx ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = CurrencyFormatter.formatAmount(tx.amount, unit, persianDigits),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BarbargSuccess
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = tx.paymentMethod,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        val forMonthText = tx.forMonth?.let { " (مربوط به ${JalaliCalendar.getMonthName(it)})" } ?: ""
                                        Text(
                                            text = "تاریخ: ${if (persianDigits) CurrencyFormatter.toPersianDigits(tx.shamsiDate) else tx.shamsiDate}$forMonthText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (tx.referenceNumber.isNotBlank()) {
                                            Text(
                                                text = "شماره پیگیری: ${tx.referenceNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (tx.notes.isNotBlank()) {
                                            Text(
                                                text = "یادداشت: ${tx.notes}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    IconButton(onClick = { txToDelete = tx }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف پرداخت", tint = BarbargError)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // SMS_HISTORY_CONTENT_ADDED
                if (smsHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هنوز سابقه‌ای از پیامک برای این مشتری ثبت نشده است.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("انتخاب‌شده: ${selectedSmsIds.size}")
                            TextButton(
                                enabled = selectedSmsIds.isNotEmpty(),
                                onClick = { confirmSmsDelete = true }
                            ) { Text("حذف انتخاب‌شده‌ها") }
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(smsHistory, key = { it.id }) { sms ->
                            val statusText = when (sms.status) {
                                "PENDING" -> "در انتظار نتیجه ارسال"
                                "SENT" -> "ارسال‌شده"
                                "FAILED" -> "ارسال ناموفق"
                                "DELIVERY_FAILED" -> "تحویل ناموفق"
                                "DELIVERED" -> "تحویل‌شده"
                                else -> sms.status
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (LocalBarbarg3D.current) 6.dp else 1.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = sms.id in selectedSmsIds,
                                            onCheckedChange = { checked ->
                                                selectedSmsIds = if (checked) selectedSmsIds + sms.id
                                                else selectedSmsIds - sms.id
                                            }
                                        )
                                        Text("انتخاب برای حذف", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when (sms.status) {
                                            "SENT", "DELIVERED" -> BarbargSuccess
                                            "FAILED", "DELIVERY_FAILED" -> BarbargError
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    Text(
                                        text = "شماره مقصد: ${sms.phoneNumber}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "مبلغ پیامک: ${CurrencyFormatter.formatAmount(sms.amount, unit, persianDigits)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "زمان: ${
                                            java.text.SimpleDateFormat(
                                                "yyyy/MM/dd HH:mm",
                                                java.util.Locale.getDefault()
                                            ).format(java.util.Date(sms.sentAt))
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = sms.message,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (sms.errorReason.isNotBlank()) {
                                        Text(
                                            text = "جزئیات خطا: ${sms.errorReason}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BarbargError
                                        )
                                    }
                                    Text(
                                        text = "بخش‌های ارسال موفق: ${sms.completedSentParts} از ${sms.expectedParts}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (sms.completedDeliveryParts > 0 || sms.failedDeliveryParts > 0) {
                                        Text(
                                            text = "گزارش تحویل: موفق ${sms.completedDeliveryParts}، ناموفق ${sms.failedDeliveryParts}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    if (confirmSmsDelete) {
        AlertDialog(
            onDismissRequest = { confirmSmsDelete = false },
            title = { Text("حذف سوابق پیامک") },
            text = { Text("آیا از حذف ${selectedSmsIds.size} سابقهٔ انتخاب‌شده مطمئن هستید؟") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteSmsHistory(selectedSmsIds.toList())
                    selectedSmsIds = emptySet()
                    confirmSmsDelete = false
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSmsDelete = false }) { Text("انصراف") }
            }
        )
    }

    // Edit Month Charge Dialog
    monthToEdit?.let { m ->
        val currentAmount = accountSummary.monthlyCharges[m] ?: 0L
        var amountInput by remember { mutableStateOf(currentAmount) }

        AlertDialog(
            onDismissRequest = { monthToEdit = null },
            title = { Text("ثبت مبلغ ماه ${JalaliCalendar.getMonthName(m)} ${uiState.selectedYear}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مبلغ صورتحساب / باربرگ مشتری برای ماه ${JalaliCalendar.getMonthName(m)} را وارد کنید:")
                    CurrencyInputField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = "مبلغ کل",
                        currencyUnit = unit,
                        usePersianDigits = persianDigits
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setMonthlyCharge(
                            customerId = customerId,
                            year = uiState.selectedYear,
                            month = m,
                            amount = amountInput
                        )
                        monthToEdit = null
                    }
                ) {
                    Text("ذخیره", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { monthToEdit = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Delete Transaction confirmation
    txToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("حذف تراکنش پرداخت") },
            text = {
                Text("آیا از حذف پرداخت به مبلغ ${CurrencyFormatter.formatAmount(tx.amount, unit, persianDigits)} مورخ ${tx.shamsiDate} اطمینان دارید؟")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        txToDelete = null
                    }
                ) {
                    Text("حذف", color = BarbargError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}





