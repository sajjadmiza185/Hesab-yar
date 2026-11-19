package com.example.ui.screens

import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.BarbargViewModel
import com.example.util.CurrencyFormatter

@Composable
fun SmsNotificationsScreen(
    viewModel: BarbargViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.pendingSmsNotifications.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var selectedId by remember { mutableStateOf<Long?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var rejectId by remember { mutableStateOf<Long?>(null) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var groupApprovalId by remember { mutableStateOf<Long?>(null) }
    var groupSelectedIds by remember { mutableStateOf(setOf<Long>()) }
    var applyAllMembers by remember(groupApprovalId) { mutableStateOf(true) }

    val selected = notifications.firstOrNull { it.id == selectedId }
    val allSelected = notifications.isNotEmpty() &&
        notifications.all { it.id in selectedIds }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "اعلان‌های پرداخت",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onBack) { Text("بازگشت") }
        }

        Text(
            "پرداخت فقط پس از تأیید شما ثبت می‌شود.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        if (notifications.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) {
                                notifications.map { it.id }.toSet()
                            } else {
                                emptySet()
                            }
                        }
                    )
                    Text("انتخاب همه", modifier = Modifier.padding(top = 12.dp))
                }
                TextButton(
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { confirmBulkDelete = true }
                ) {
                    Text("حذف انتخاب‌شده‌ها (${selectedIds.size})")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (notifications.isEmpty()) {
            Text("اعلان تأییدنشده‌ای وجود ندارد.", modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notifications, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
    ) {
                        Row(
                            Modifier.padding(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.id in selectedIds,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) {
                                        selectedIds + item.id
                                    } else {
                                        selectedIds - item.id
                                    }
                                }
                            )
                            Column(
                                Modifier.weight(1f)
                                    .clickable { selectedId = item.id }
                                    .padding(8.dp)
                            ) {
                                Text(item.customerName, fontWeight = FontWeight.Bold)
                                if (item.amountRial > 0L) {
                                    Text("مبلغ پیامک: ${CurrencyFormatter.formatNumber(item.amountRial)} ریال")
                                } else {
                                    Text(
                                        "مبلغ در پیام نیست؛ برای پرداخت گروهی مبلغ واقعی لازم است.",
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    "تاریخ: ${item.shamsiDate} | شماره پیگیری: ${item.trackingNumber.ifBlank { "ندارد" }}"
                                )
                                Text("فرستنده: ${item.senderNumber}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "برای مشاهده متن و تصمیم‌گیری انتخاب کنید",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selected != null) {
        AlertDialog(
            onDismissRequest = { selectedId = null },
            title = { Text("بررسی پیامک پرداخت") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مشتری: ${selected.customerName}")
                    if (selected.amountRial > 0L) {
                        Text("مبلغ پیامک: ${CurrencyFormatter.formatNumber(selected.amountRial)} ریال")
                    } else {
                        Text(
                            "مبلغ پیامک مشخص نیست؛ پرداخت گروهی با این پیامک ممکن نیست.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text("تاریخ: ${selected.shamsiDate}")
                    Text("شماره پیگیری: ${selected.trackingNumber.ifBlank { "ندارد" }}")
                    Text("متن کامل پیامک:")
                    Text(selected.body, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "پیامک به‌تنهایی اثبات قطعی پرداخت بانکی نیست.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val leader = customers.firstOrNull { it.id == selected.customerId }
                    val groupId = leader?.groupId

                    if (groupId != null) {
                        val memberIds = customers
                            .filter { it.isActive && it.groupId == groupId }
                            .map { it.id }
                            .toSet()

                        groupSelectedIds = memberIds
                        applyAllMembers = true
                        groupApprovalId = selected.id
                    } else {
                        viewModel.approveSmsNotification(selected.id)
                    }
                    selectedId = null
                }) {
                    Text("تأیید و ادامه")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        rejectId = selected.id
                        selectedId = null
                    }) { Text("رد پیامک") }
                    TextButton(onClick = { selectedId = null }) { Text("بستن") }
                }
            }
        )
    }

    val groupItem = notifications.firstOrNull { it.id == groupApprovalId }
    val groupLeader = groupItem?.let { item ->
        customers.firstOrNull { it.id == item.customerId }
    }
    val groupId = groupLeader?.groupId
    val groupMembers = if (groupId != null) {
        customers.filter { it.isActive && it.groupId == groupId }
            .sortedBy { it.name }
    } else {
        emptyList()
    }

    if (groupApprovalId != null && groupItem != null) {
        AlertDialog(
            onDismissRequest = { groupApprovalId = null },
            title = { Text("تأیید پرداخت گروهی") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سرگروه: ${groupLeader?.name ?: groupItem.customerName}")
                    Text("مبلغ پیامک: ${CurrencyFormatter.formatNumber(groupItem.amountRial)} ریال")
                    Text("اعضای فعال گروه: ${groupMembers.size}")

                    HorizontalDivider()

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(
                            checked = applyAllMembers,
                            onCheckedChange = { checked ->
                                applyAllMembers = checked
                                if (checked) {
                                    groupSelectedIds = groupMembers.map { it.id }.toSet()
                                }
                            }
                        )
                        Text("اعمال برای تمام اعضای گروه")
                    }

                    if (!applyAllMembers) {
                        Text("اعضای موردنظر را انتخاب کنید:")
                        groupMembers.forEach { member ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(
                                    checked = member.id in groupSelectedIds,
                                    onCheckedChange = { checked ->
                                        groupSelectedIds = if (checked) {
                                            groupSelectedIds + member.id
                                        } else {
                                            groupSelectedIds - member.id
                                        }
                                    }
                                )
                                Text("${member.name} (${member.phone})")
                            }
                        }
                    }

                    val count = if (applyAllMembers) {
                        groupMembers.size
                    } else {
                        groupSelectedIds.count { id -> groupMembers.any { it.id == id } }
                    }

                    Text("تعداد انتخاب‌شده: $count")
                    Text(
                        if (groupItem.amountRial > 0L) {
                            "مبلغ پیامک بین اعضای انتخاب‌شده تقسیم می‌شود؛ باقی‌مانده نیز دقیق توزیع خواهد شد."
                        } else {
                            "مبلغ پیامک مشخص نیست؛ شارژ ماهانه هر عضو برای همین ماه جداگانه محاسبه می‌شود. برای همه اعضای انتخاب‌شده باید شارژ ماهانه تعریف شده باشد."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = groupMembers.isNotEmpty() &&
                        (if (applyAllMembers) groupMembers.size else groupSelectedIds.size) > 0,
                    onClick = {
                        val ids = if (applyAllMembers) {
                            groupMembers.map { it.id }
                        } else {
                            groupSelectedIds.toList()
                        }
                        viewModel.approveGroupSmsNotification(groupItem.id, ids)
                        groupApprovalId = null
                    }
                ) {
                    Text("تأیید و ثبت پرداخت گروهی")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupApprovalId = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (rejectId != null) {
        AlertDialog(
            onDismissRequest = { rejectId = null },
            title = { Text("تأیید حذف") },
            text = { Text("آیا از حذف این اعلان مطمئن هستید؟") },
            confirmButton = {
                Button(onClick = {
                    rejectId?.let { viewModel.rejectSmsNotification(it) }
                    rejectId = null
                }) { Text("بله، حذف شود") }
            },
            dismissButton = {
                TextButton(onClick = { rejectId = null }) { Text("انصراف") }
            }
        )
    }

    if (confirmBulkDelete) {
        AlertDialog(
            onDismissRequest = { confirmBulkDelete = false },
            title = { Text("تأیید حذف") },
            text = { Text("آیا از حذف این اعلان‌ها مطمئن هستید؟") },
            confirmButton = {
                Button(onClick = {
                    viewModel.rejectSmsNotifications(selectedIds.toList())
                    selectedIds = emptySet()
                    confirmBulkDelete = false
                }) { Text("بله، حذف شوند") }
            },
            dismissButton = {
                TextButton(onClick = { confirmBulkDelete = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}