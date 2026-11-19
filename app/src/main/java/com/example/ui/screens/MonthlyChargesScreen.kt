package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import com.example.ui.components.BarbergButton as Button
import com.example.ui.components.BarbergCard as Card
import androidx.compose.material3.CardDefaults
import com.example.ui.theme.LocalBarbarg3D
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.BarbergOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CustomerAccountSummary
import com.example.model.CustomerGroup
import com.example.ui.components.CurrencyInputField
import com.example.ui.theme.BarbargSuccess
import com.example.ui.viewmodel.BarbargViewModel
import com.example.util.CurrencyFormatter
import com.example.util.JalaliCalendar

@Composable
fun MonthlyChargesScreen(
    viewModel: BarbargViewModel,
    onOpenCustomerDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summaries by viewModel.customerAccountSummaries.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    var activeMonth by remember(uiState.selectedMonth) { mutableIntStateOf(uiState.selectedMonth) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf<Long?>(null) }

    var editingCustomer by remember { mutableStateOf<CustomerAccountSummary?>(null) }
    var showBatchSetDialog by remember { mutableStateOf(false) }

    val unit = uiState.currencyUnit
    val persianDigits = uiState.usePersianDigits
    val activeMonthName = JalaliCalendar.getMonthName(activeMonth)

    val filteredList = summaries.filter { item ->
        val matchQuery = if (searchQuery.isBlank()) true else {
            val q = searchQuery.trim()
            item.customer.name.contains(q, ignoreCase = true) || item.customer.phone.contains(q)
        }
        val matchGroup = selectedGroupFilter == null || item.customer.groupId == selectedGroupFilter
        matchQuery && matchGroup
    }

    val totalMonthCharges = filteredList.sumOf { it.monthlyCharges[activeMonth] ?: 0L }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("monthly_charges_screen")
    ) {
        // Month tabs (Farvardin to Esfand)
        ScrollableTabRow(
            selectedTabIndex = activeMonth - 1,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            (1..12).forEach { m ->
                val isSelected = activeMonth == m
                Tab(
                    selected = isSelected,
                    onClick = {
                        activeMonth = m
                        viewModel.setSelectedMonth(m)
                    },
                    text = {
                        Text(
                            text = JalaliCalendar.getMonthName(m),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Search & Batch Action Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "صورتحساب‌های $activeMonthName سال ${CurrencyFormatter.toPersianDigits(uiState.selectedYear.toString())}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "مجموع مبالغ این ماه: ${CurrencyFormatter.formatAmount(totalMonthCharges, unit, persianDigits)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Batch set button
                Button(
                    onClick = { showBatchSetDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("batch_set_amounts_button")
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تنظیم گروهی", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("جستجو در مشتریان این ماه...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Groups Filter chips
            if (groups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedGroupFilter == null,
                            onClick = { selectedGroupFilter = null },
                            label = { Text("همه گروه‌ها") }
                        )
                    }
                    items(groups) { g ->
                        FilterChip(
                            selected = selectedGroupFilter == g.id,
                            onClick = { selectedGroupFilter = if (selectedGroupFilter == g.id) null else g.id },
                            label = { Text(g.name) }
                        )
                    }
                }
            }
        }

        // Customer Month List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (summaries.isEmpty()) "هیچ مشتری ثبت نشده است." else "مشتری‌ای مطابق با جستجو پیدا نشد.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.customer.id }) { item ->
                    val monthAmount = item.monthlyCharges[activeMonth] ?: 0L
                    val monthPaid = item.monthlyPayments[activeMonth] ?: 0L
                    val balance = monthAmount - monthPaid

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 8.dp else 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingCustomer = item }
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
                                        text = item.customer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    item.group?.let { g ->
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = g.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "مبلغ: ${CurrencyFormatter.formatAmount(monthAmount, unit, persianDigits)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 2
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(30.dp)
                                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(1.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "پرداختی: ${CurrencyFormatter.formatAmount(monthPaid, unit, persianDigits)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = BarbargSuccess,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (monthAmount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { editingCustomer = item }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "ویرایش مبلغ",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (monthAmount > 0) CurrencyFormatter.formatNumber(monthAmount, persianDigits) else "ثبت مبلغ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
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

    // Single Customer Month Charge Edit Dialog
    editingCustomer?.let { item ->
        val currentCharge = item.monthlyCharges[activeMonth] ?: 0L
        var amountInput by remember { mutableStateOf(currentCharge) }

        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { editingCustomer = null },
            title = {
                Text(
                    text = "مبلغ باربرگ: ${item.customer.name}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("مبلغ ماه $activeMonthName سال ${CurrencyFormatter.toPersianDigits(uiState.selectedYear.toString())} را وارد نمایید:")
                    CurrencyInputField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = "مبلغ $activeMonthName",
                        currencyUnit = unit,
                        usePersianDigits = persianDigits
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setMonthlyCharge(
                            customerId = item.customer.id,
                            year = uiState.selectedYear,
                            month = activeMonth,
                            amount = amountInput
                        )
                        editingCustomer = null
                    }
                ) {
                    Text("ذخیره", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCustomer = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Batch Set Amounts Dialog: all customers, one group, or selected customers
    if (showBatchSetDialog) {
        var batchAmount by remember { mutableStateOf(0L) }
        var targetMode by remember { mutableStateOf("ALL") } // ALL, GROUP, CUSTOMERS
        var targetGroupId by remember { mutableStateOf<Long?>(null) }
        var batchSearchQuery by remember { mutableStateOf("") }
        var selectedBatchCustomerIds by remember { mutableStateOf(setOf<Long>()) }

        val matchingBatchCustomers = summaries.filter { item ->
            batchSearchQuery.isBlank() ||
                item.customer.name.contains(batchSearchQuery.trim(), ignoreCase = true) ||
                item.customer.phone.contains(batchSearchQuery.trim())
        }
        val targetCount = when (targetMode) {
            "GROUP" -> summaries.count { it.customer.groupId == targetGroupId }
            "CUSTOMERS" -> selectedBatchCustomerIds.size
            else -> summaries.size
        }

        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { showBatchSetDialog = false },
            title = { Text("تنظیم گروهی مبلغ ماه $activeMonthName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "مبلغ را برای ماه $activeMonthName سال ${CurrencyFormatter.toPersianDigits(uiState.selectedYear.toString())} تعیین کنید. فقط مشتریان انتخاب‌شده تغییر می‌کنند.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text("اعمال برای:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        item {
                            FilterChip(
                                selected = targetMode == "ALL",
                                onClick = { targetMode = "ALL" },
                                label = { Text("تمام مشتریان (${summaries.size})") }
                            )
                        }
                        items(groups) { group ->
                            val count = summaries.count { it.customer.groupId == group.id }
                            FilterChip(
                                selected = targetMode == "GROUP" && targetGroupId == group.id,
                                onClick = { targetMode = "GROUP"; targetGroupId = group.id },
                                label = { Text("${group.name} ($count)") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = targetMode == "CUSTOMERS",
                                onClick = { targetMode = "CUSTOMERS" },
                                label = { Text("انتخاب چند مشتری") }
                            )
                        }
                    }

                    if (targetMode == "CUSTOMERS") {
                        OutlinedTextField(
                            value = batchSearchQuery,
                            onValueChange = { batchSearchQuery = it },
                            placeholder = { Text("جستجوی نام یا شماره تلفن") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (batchSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { batchSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "پاک کردن جستجو")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("انتخاب‌شده: ${selectedBatchCustomerIds.size} نفر", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = {
                                selectedBatchCustomerIds = selectedBatchCustomerIds + matchingBatchCustomers.map { it.customer.id }
                            }) { Text("انتخاب نتایج") }
                            TextButton(onClick = { selectedBatchCustomerIds = emptySet() }) { Text("پاک‌کردن") }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(matchingBatchCustomers, key = { it.customer.id }) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedBatchCustomerIds = if (item.customer.id in selectedBatchCustomerIds) {
                                            selectedBatchCustomerIds - item.customer.id
                                        } else {
                                            selectedBatchCustomerIds + item.customer.id
                                        }
                                    }.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.customer.id in selectedBatchCustomerIds,
                                        onCheckedChange = { checked ->
                                            selectedBatchCustomerIds = if (checked) selectedBatchCustomerIds + item.customer.id else selectedBatchCustomerIds - item.customer.id
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.customer.name, style = MaterialTheme.typography.bodyMedium)
                                        if (item.customer.phone.isNotBlank()) Text(item.customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    CurrencyInputField(
                        value = batchAmount,
                        onValueChange = { batchAmount = it },
                        label = "مبلغ یکسان برای ماه $activeMonthName",
                        currencyUnit = unit,
                        usePersianDigits = persianDigits
                    )
                    Text("تعداد مشتریان هدف: $targetCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = targetCount > 0,
                    onClick = {
                        val targets = when (targetMode) {
                            "GROUP" -> summaries.filter { it.customer.groupId == targetGroupId }.map { it.customer.id }
                            "CUSTOMERS" -> selectedBatchCustomerIds.toList()
                            else -> summaries.map { it.customer.id }
                        }
                        viewModel.batchSetMonthAmounts(
                            customerIds = targets,
                            year = uiState.selectedYear,
                            month = activeMonth,
                            amount = batchAmount
                        )
                        showBatchSetDialog = false
                    }
                ) { Text("اعمال گروهی", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showBatchSetDialog = false }) { Text("انصراف") }
            }
        )
    }

}



