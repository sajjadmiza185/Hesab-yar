package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Customer
import com.example.model.CustomerAccountSummary
import com.example.model.CustomerGroup
import com.example.ui.components.CurrencyInputField
import com.example.ui.components.ShamsiDatePickerDialog
import com.example.ui.theme.BarbargSuccess
import com.example.util.CurrencyFormatter
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualPaymentDialog(
    initialCustomerId: Long?,
    customers: List<Customer>,
    summaries: List<CustomerAccountSummary>,
    currencyUnit: String,
    usePersianDigits: Boolean,
    currentYear: Int,
    onDismiss: () -> Unit,
    onSavePayment: (
        customerId: Long,
        amount: Long,
        shamsiDate: String,
        paymentMethod: String,
        referenceNumber: String,
        forMonth: Int?,
        forYear: Int?,
        notes: String
    ) -> Unit
) {
    var selectedCustomerId by remember {
        mutableStateOf(initialCustomerId ?: customers.firstOrNull()?.id ?: 0L)
    }
    var amount by remember { mutableLongStateOf(0L) }
    var selectedDate by remember { mutableStateOf(JalaliCalendar.today()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val paymentMethods = listOf("کارت به کارت", "نقدی", "پوز بانکی", "حواله", "چک", "سایر")
    var selectedMethod by remember { mutableStateOf(paymentMethods[0]) }
    var referenceNumber by remember { mutableStateOf("") }
    var forMonth by remember { mutableStateOf<Int?>(JalaliCalendar.today().month) }
    var notes by remember { mutableStateOf("") }

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val customerSummary = summaries.find { it.customer.id == selectedCustomerId }

    // Auto-suggest debt amount if available
    val suggestedDebt = customerSummary?.balance?.takeIf { it > 0 } ?: 0L

    AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = BarbargSuccess)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت پرداخت جدید", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer selection dropdown
                ExposedDropdownMenuBox(
                    expanded = customerDropdownExpanded,
                    onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "انتخاب مشتری",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("مشتری / طرف حساب *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    selectedCustomerId = c.id
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Balance indicator & Quick fill debt button
                if (customerSummary != null && customerSummary.balance > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مانده بدهی: ${CurrencyFormatter.formatAmount(customerSummary.balance, currencyUnit, usePersianDigits)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { amount = customerSummary.balance }
                            ) {
                                Text("پرداخت کل بدهی", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Amount input
                CurrencyInputField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "مبلغ پرداختی *",
                    currencyUnit = currencyUnit,
                    usePersianDigits = usePersianDigits,
                    testTag = "payment_amount_input"
                )

                // Shamsi Date selector Card
                OutlinedCard(
                    onClick = { showDatePicker = true },
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("تاریخ پرداخت", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = JalaliCalendar.formatMedium(selectedDate, usePersianDigits),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text("تغییر", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }

                // Payment method chips
                Text(
                    text = "روش پرداخت",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(paymentMethods) { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method) }
                        )
                    }
                }

                // Reference number
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("شماره پیگیری / شماره چک") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // For which Shamsi month
                Text(
                    text = "مربوط به ماه (اختیاری)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = forMonth == null,
                            onClick = { forMonth = null },
                            label = { Text("بدون تفکیک") }
                        )
                    }
                    items((1..12).toList()) { m ->
                        FilterChip(
                            selected = forMonth == m,
                            onClick = { forMonth = m },
                            label = { Text(JalaliCalendar.getMonthName(m)) }
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("توضیحات و بابت") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedCustomerId > 0 && amount > 0) {
                        onSavePayment(
                            selectedCustomerId,
                            amount,
                            selectedDate.toString(),
                            selectedMethod,
                            referenceNumber,
                            forMonth,
                            currentYear,
                            notes
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text("ثبت نهایی", fontWeight = FontWeight.Bold, color = BarbargSuccess)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )

    if (showDatePicker) {
        ShamsiDatePickerDialog(
            initialDate = selectedDate,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { date ->
                selectedDate = date
                showDatePicker = false
            }
        )
    }
}

@Composable
fun BatchPaymentDialog(
    customers: List<Customer>,
    groups: List<CustomerGroup>,
    currencyUnit: String,
    usePersianDigits: Boolean,
    currentYear: Int,
    onDismiss: () -> Unit,
    onSaveBatch: (
        customerIds: List<Long>,
        amountPerCustomer: Long,
        shamsiDate: String,
        paymentMethod: String,
        forMonth: Int?,
        forYear: Int?,
        notes: String
    ) -> Unit
) {
    var amountPerCustomer by remember { mutableLongStateOf(0L) }
    var selectedDate by remember { mutableStateOf(JalaliCalendar.today()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf("کارت به کارت") }
    var forMonth by remember { mutableStateOf<Int?>(JalaliCalendar.today().month) }
    var notes by remember { mutableStateOf("") }

    val selectedCustomerIds = remember { mutableStateListOf<Long>() }

    // Quick selection helpers
    fun selectAll() {
        selectedCustomerIds.clear()
        selectedCustomerIds.addAll(customers.map { it.id })
    }

    fun selectByGroup(groupId: Long) {
        selectedCustomerIds.clear()
        selectedCustomerIds.addAll(customers.filter { it.groupId == groupId }.map { it.id })
    }

    fun clearSelection() {
        selectedCustomerIds.clear()
    }

    AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = BarbargSuccess)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت پرداخت گروهی", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "مبلغ و شرایط پرداخت را تعیین کنید، سپس مشتریان مورد نظر را انتخاب نمایید:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Amount per customer
                CurrencyInputField(
                    value = amountPerCustomer,
                    onValueChange = { amountPerCustomer = it },
                    label = "مبلغ پرداختی هر مشتری",
                    currencyUnit = currencyUnit,
                    usePersianDigits = usePersianDigits,
                    testTag = "batch_payment_amount_input"
                )

                // Date
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تاریخ: ${JalaliCalendar.formatMedium(selectedDate, usePersianDigits)}")
                        Text("تغییر", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }

                // Month
                Text(
                    text = "مربوط به ماه",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = forMonth == null,
                            onClick = { forMonth = null },
                            label = { Text("بدون ماه") }
                        )
                    }
                    items((1..12).toList()) { m ->
                        FilterChip(
                            selected = forMonth == m,
                            onClick = { forMonth = m },
                            label = { Text(JalaliCalendar.getMonthName(m)) }
                        )
                    }
                }

                // Selection Shortcuts
                Text(
                    text = "انتخاب مشتریان (${selectedCustomerIds.size} نفر انتخاب شده):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCustomerIds.size == customers.size && customers.isNotEmpty(),
                            onClick = { selectAll() },
                            label = { Text("همه مشتریان") }
                        )
                    }
                    items(groups) { g ->
                        FilterChip(
                            selected = false,
                            onClick = { selectByGroup(g.id) },
                            label = { Text("گروه ${g.name}") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { clearSelection() },
                            label = { Text("لغو انتخاب") }
                        )
                    }
                }

                // Customer checklist
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
    ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    ) {
                        items(customers) { c ->
                            val isChecked = selectedCustomerIds.contains(c.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedCustomerIds.remove(c.id)
                                        else selectedCustomerIds.add(c.id)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedCustomerIds.add(c.id)
                                        else selectedCustomerIds.remove(c.id)
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(c.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("توضیحات پرداخت گروهی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedCustomerIds.isNotEmpty() && amountPerCustomer > 0) {
                        onSaveBatch(
                            selectedCustomerIds.toList(),
                            amountPerCustomer,
                            selectedDate.toString(),
                            selectedMethod,
                            forMonth,
                            currentYear,
                            notes
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_batch_payment_button")
            ) {
                Text("ثبت برای ${selectedCustomerIds.size} نفر", fontWeight = FontWeight.Bold, color = BarbargSuccess)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )

    if (showDatePicker) {
        ShamsiDatePickerDialog(
            initialDate = selectedDate,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { date ->
                selectedDate = date
                showDatePicker = false
            }
        )
    }
}
