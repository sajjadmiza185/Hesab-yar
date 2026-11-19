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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import com.example.ui.components.BarbergCard as Card
import androidx.compose.material3.CardDefaults
import com.example.ui.theme.LocalBarbarg3D
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PaymentTransaction
import com.example.ui.components.CurrencyInputField
import com.example.ui.theme.BarbargError
import com.example.ui.theme.BarbargSuccess
import com.example.ui.theme.BarbargWarning
import com.example.ui.viewmodel.BarbargViewModel
import com.example.util.CurrencyFormatter
import com.example.util.JalaliCalendar

@Composable
fun TransactionsScreen(
    viewModel: BarbargViewModel,
    onOpenCustomerDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    val customerMap = remember(customers) { customers.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf(uiState.transactionSearchQuery) }

    var txToEdit by remember { mutableStateOf<PaymentTransaction?>(null) }
    var txToDelete by remember { mutableStateOf<PaymentTransaction?>(null) }

    val unit = uiState.currencyUnit
    val persianDigits = uiState.usePersianDigits

    val filteredList = remember(transactions, searchQuery) {
        transactions.filter { tx ->
            val custName = customerMap[tx.customerId]?.name ?: ""
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim()
                custName.contains(q, ignoreCase = true) ||
                        tx.referenceNumber.contains(q, ignoreCase = true) ||
                        tx.notes.contains(q, ignoreCase = true) ||
                        tx.shamsiDate.contains(q)
            }
            matchesSearch
        }
    }

    val totalAmount = filteredList.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen")
    ) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تاریخچه تراکنش‌ها",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "مجموع: ${CurrencyFormatter.formatAmount(totalAmount, unit, persianDigits)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.setTransactionSearchQuery(it)
                },
                placeholder = { Text("جستجو در نام مشتری، شماره پیگیری، تاریخ...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.setTransactionSearchQuery("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))


        }

        // Transactions list
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (transactions.isEmpty()) "هنوز هیچ تراکنشی ثبت نشده است."
                    else "تراکنشی مطابق با جستجو پیدا نشد.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { tx ->
                    val customer = customerMap[tx.customerId]
                    TransactionItemCard(
                        transaction = tx,
                        customerName = customer?.name ?: "مشتری نامشخص",
                        currencyUnit = unit,
                        usePersianDigits = persianDigits,
                        onCustomerClick = {
                            customer?.let { onOpenCustomerDetail(it.id) }
                        },
                        onEditClick = { txToEdit = tx },
                        onDeleteClick = { txToDelete = tx }
                    )
                }
            }
        }
    }

    // Edit Transaction Dialog
    txToEdit?.let { tx ->
        var amountInput by remember { mutableStateOf(tx.amount) }
        var methodInput by remember { mutableStateOf(tx.paymentMethod) }
        var refInput by remember { mutableStateOf(tx.referenceNumber) }
        var notesInput by remember { mutableStateOf(tx.notes) }

        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { txToEdit = null },
            title = { Text("ویرایش تراکنش") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CurrencyInputField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = "مبلغ",
                        currencyUnit = unit,
                        usePersianDigits = persianDigits
                    )

                    OutlinedTextField(
                        value = methodInput,
                        onValueChange = { methodInput = it },
                        label = { Text("روش پرداخت") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = refInput,
                        onValueChange = { refInput = it },
                        label = { Text("شماره پیگیری") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("توضیحات") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = tx.copy(
                            amount = amountInput,
                            paymentMethod = methodInput,
                            referenceNumber = refInput,
                            notes = notesInput
                        )
                        viewModel.updateTransaction(updated)
                        txToEdit = null
                    }
                ) {
                    Text("ذخیره تغییرات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToEdit = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Delete Transaction confirmation
    txToDelete?.let { tx ->
        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = { txToDelete = null },
            title = { Text("حذف تراکنش") },
            text = {
                Text("آیا از حذف این تراکنش به مبلغ ${CurrencyFormatter.formatAmount(tx.amount, unit, persianDigits)} اطمینان دارید؟")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        txToDelete = null
                    }
                ) {
                    Text("حذف قطعی", color = BarbargError, fontWeight = FontWeight.Bold)
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

@Composable
fun TransactionItemCard(
    transaction: PaymentTransaction,
    customerName: String,
    currencyUnit: String,
    usePersianDigits: Boolean,
    onCustomerClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (LocalBarbarg3D.current) 8.dp else 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onCustomerClick)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = transaction.paymentMethod,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
                )
                Spacer(modifier = Modifier.height(7.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        text = CurrencyFormatter.formatAmount(transaction.amount, currencyUnit, usePersianDigits),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BarbargSuccess
                    )
                    val monthText = transaction.forMonth?.let { "بابت ${JalaliCalendar.getMonthName(it)}" } ?: ""
                    if (monthText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f))
                        )
                        Text(
                            text = monthText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                )
                Spacer(modifier = Modifier.height(5.dp))
                val dateStr = if (usePersianDigits) CurrencyFormatter.toPersianDigits(transaction.shamsiDate) else transaction.shamsiDate
                Text(
                    text = "تاریخ: $dateStr" + if (transaction.referenceNumber.isNotBlank()) " | پیگیری: ${transaction.referenceNumber}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (transaction.notes.isNotBlank()) {
                    Text(
                        text = "یادداشت: ${transaction.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(76.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = BarbargError)
                }
            }
        }
    }
}



