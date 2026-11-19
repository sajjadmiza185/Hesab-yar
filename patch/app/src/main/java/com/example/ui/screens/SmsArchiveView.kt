package com.example.ui.screens

import androidx.compose.material3.CardDefaults
import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.SmsHistory
import com.example.ui.viewmodel.BarbargViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ARCHIVE_SMS_SENT = "com.example.BARBARG_ARCHIVE_SMS_SENT"
private const val ARCHIVE_SMS_HISTORY_ID = "archive_sms_history_id"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmsArchiveView(viewModel: BarbargViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val batches by viewModel.observeSmsSendBatches().collectAsStateWithLifecycle(initialValue = emptyList())
    val history by viewModel.observeAllSmsHistory().collectAsStateWithLifecycle(initialValue = emptyList())
    var expandedBatch by remember { mutableStateOf<Long?>(null) }
    var confirmBatch by remember { mutableStateOf<Long?>(null) }
    var confirmOld by remember { mutableStateOf(false) }
    var confirmSelectedDelete by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedBatchIds by remember { mutableStateOf(setOf<Long>()) }
    var batchSelectionMode by remember { mutableStateOf(false) }
    var confirmSelectedBatchDelete by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("") }
    val oldItems = history.filter { it.batchId == null }
    val failedItems = history.filter { it.status == "FAILED" }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()) }

    DisposableEffect(context) {
        val filter = IntentFilter(ARCHIVE_SMS_SENT)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent?) {
                if (intent?.action != ARCHIVE_SMS_SENT) return
                val id = intent.getLongExtra(ARCHIVE_SMS_HISTORY_ID, -1L)
                if (id > 0L) {
                    val ok = resultCode == Activity.RESULT_OK
                    viewModel.recordSmsSentPart(
                        id = id,
                        succeeded = if (ok) 1 else 0,
                        failed = if (ok) 0 else 1,
                        errorReason = if (ok) "" else "ارسال مجدد این بخش پیامک ناموفق بود (کد خطا: $resultCode)"
                    )
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    fun retrySelected() {
        val targets = history.filter { it.id in selectedIds && it.status == "FAILED" }
        if (targets.isEmpty() || busy) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            info = "مجوز ارسال پیامک فعال نیست. ابتدا مجوز SMS را از تنظیمات برنامه فعال کن."
            return
        }
        busy = true
        scope.launch {
            try {
                val batchId = viewModel.createSmsSendBatchAndReturn("ارسال مجدد ${targets.size} پیامک")
                var queued = 0
                var immediateFailed = 0
                targets.forEachIndexed { index, old ->
                    val phone = old.phoneNumber.trim()
                    val message = old.message
                    val smsManager = runCatching {
                        if (old.simSubscriptionId >= 0) {
                            SmsManager.getSmsManagerForSubscriptionId(old.simSubscriptionId)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }
                    }.getOrNull()
                    val parts = if (phone.isNotBlank() && smsManager != null) {
                        runCatching { smsManager.divideMessage(message).toList() }.getOrDefault(emptyList())
                    } else emptyList()
                    val validationError = when {
                        phone.isBlank() -> "شماره تلفن خالی است"
                        smsManager == null -> "سرویس پیامک در دسترس نیست"
                        parts.isEmpty() -> "متن پیامک قابل آماده‌سازی نیست"
                        else -> ""
                    }
                    val newId = viewModel.insertSmsHistoryAndReturn(
                        old.copy(
                            id = 0L,
                            sentAt = System.currentTimeMillis(),
                            status = if (validationError.isEmpty()) "PENDING" else "FAILED",
                            batchId = batchId,
                            expectedParts = maxOf(parts.size, 1),
                            completedSentParts = 0,
                            failedSentParts = if (validationError.isEmpty()) 0 else 1,
                            completedDeliveryParts = 0,
                            failedDeliveryParts = 0,
                            errorReason = validationError
                        )
                    )
                    if (validationError.isNotEmpty()) {
                        immediateFailed++
                        return@forEachIndexed
                    }
                    try {
                        val sentIntents = ArrayList<PendingIntent>(parts.size)
                        parts.indices.forEach { part ->
                            val callback = Intent(ARCHIVE_SMS_SENT)
                                .setPackage(context.packageName)
                                .putExtra(ARCHIVE_SMS_HISTORY_ID, newId)
                            val requestCode = ((newId % 1_000_000L).toInt() * 37 + part + index) and 0x7fffffff
                            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                            sentIntents += PendingIntent.getBroadcast(context, requestCode, callback, flags)
                        }
                        if (parts.size > 1) {
                            smsManager!!.sendMultipartTextMessage(phone, null, ArrayList(parts), sentIntents, null)
                        } else {
                            smsManager!!.sendTextMessage(phone, null, parts.first(), sentIntents.first(), null)
                        }
                        queued++
                    } catch (e: Exception) {
                        viewModel.recordSmsSentPart(
                            newId, 0, parts.size,
                            e.message ?: "شروع ارسال مجدد ناموفق بود"
                        )
                        immediateFailed++
                    }
                }
                selectedIds = emptySet()
                info = "نوبت ارسال مجدد ثبت شد. $queued پیامک برای ارسال به سیستم سپرده شد؛ $immediateFailed مورد با خطای فوری ثبت شد. نتیجهٔ نهایی پس از دریافت گزارش ارسال به‌روزرسانی می‌شود."
            } catch (e: Exception) {
                info = "ثبت ارسال مجدد انجام نشد: ${e.message ?: "خطای نامشخص"}"
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("آرشیو دائمی پیامک‌ها", style = MaterialTheme.typography.titleLarge)
        Text("هر گیرنده یک پیامک محسوب می‌شود؛ موفقیت بر اساس نتیجه ارسال ثبت‌شده است.",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = {
                selectedIds = if (selectedIds.containsAll(history.map { it.id }) && history.isNotEmpty()) {
                    emptySet()
                } else history.map { it.id }.toSet()
            }, enabled = history.isNotEmpty() && !busy, modifier = Modifier.weight(1f)) {
                Text("انتخاب همه ناموفق‌ها")
            }
            OutlinedButton(onClick = { selectedIds = emptySet(); selectionMode = false }, enabled = selectedIds.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Text("لغو انتخاب")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = ::retrySelected, enabled = selectedIds.isNotEmpty() && !busy, modifier = Modifier.weight(1f)) {
                Text(if (busy) "در حال ثبت..." else "ارسال مجدد (${selectedIds.size})")
            }
            OutlinedButton(onClick = { confirmSelectedDelete = true }, enabled = selectedIds.isNotEmpty() && !busy, modifier = Modifier.weight(1f)) {
                Text("حذف انتخاب‌شده‌ها")
            }
        }
        if (info.isNotBlank()) {
            Text(info, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))

        if (batches.isEmpty() && oldItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز سابقه‌ای برای نمایش وجود ندارد.")
            }
        } else {
            if (batchSelectionMode) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedBatchIds = emptySet()
                            batchSelectionMode = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("لغو انتخاب گزارش‌ها") }

                    Button(
                        onClick = { confirmSelectedBatchDelete = true },
                        enabled = selectedBatchIds.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("حذف گزارش‌ها (${selectedBatchIds.size})") }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(batches, key = { it.id }) { batch ->
                    val entries = history.filter { it.batchId == batch.id }
                    val success = entries.count { it.status == "SENT" || it.status == "DELIVERED" || it.status == "DELIVERY_FAILED" }
                    val failed = entries.count { it.status == "FAILED" }
                    val pending = entries.size - success - failed
                    Card(
                        Modifier.fillMaxWidth().combinedClickable(
                            onClick = {
                                if (batchSelectionMode) {
                                    selectedBatchIds =
                                        if (batch.id in selectedBatchIds)
                                            selectedBatchIds - batch.id
                                        else selectedBatchIds + batch.id
                                }
                            },
                            onLongClick = {
                                batchSelectionMode = true
                                selectedBatchIds = selectedBatchIds + batch.id
                            }
                        )
                    ,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (batchSelectionMode) {
                                    Checkbox(
                                        checked = batch.id in selectedBatchIds,
                                        onCheckedChange = { checked ->
                                            selectedBatchIds =
                                                if (checked) selectedBatchIds + batch.id
                                                else selectedBatchIds - batch.id
                                        }
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(batch.title.ifBlank { "نوبت ارسال پیامک" }, style = MaterialTheme.typography.titleMedium)
                                    Text(dateFormat.format(Date(batch.createdAt)), style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { expandedBatch = if (expandedBatch == batch.id) null else batch.id }) {
                                    Text(if (expandedBatch == batch.id) "بستن" else "جزئیات")
                                }
                            }
                            Text("موفق: $success    ناموفق: $failed    در انتظار: $pending")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { confirmBatch = batch.id }) { Text("حذف کل نوبت") }
                            }
                            if (expandedBatch == batch.id) {
                                entries.forEach { item ->
                                    SmsArchiveEntry(item, selectionMode, selectedIds.contains(item.id), onLongPress = { selectionMode = true; selectedIds = selectedIds + item.id }) { checked ->
                                        selectedIds = if (checked) selectedIds + item.id else selectedIds - item.id
                                    }
                                }
                                if (entries.isEmpty()) Text("پیامکی در این نوبت ثبت نشده است.")
                            }
                        }
                    }
                }
                if (oldItems.isNotEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
    ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("سوابق قدیمی / بدون گزارش نوبت", style = MaterialTheme.typography.titleMedium)
                                Text("تعداد: ${oldItems.size}")
                                oldItems.forEach { item ->
                                    SmsArchiveEntry(item, selectionMode, selectedIds.contains(item.id), onLongPress = { selectionMode = true; selectedIds = selectedIds + item.id }) { checked ->
                                        selectedIds = if (checked) selectedIds + item.id else selectedIds - item.id
                                    }
                                }
                                TextButton(onClick = { confirmOld = true }) { Text("حذف سوابق قدیمی") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmSelectedBatchDelete) {
        AlertDialog(
            onDismissRequest = { confirmSelectedBatchDelete = false },
            title = { Text("حذف گزارش‌های انتخاب‌شده؟") },
            text = {
                Text("${selectedBatchIds.size} گزارش نوبت ارسال حذف می‌شود. اطلاعات مشتریان و پرداخت‌ها حفظ می‌شوند.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val ids = selectedBatchIds.toList()
                    confirmSelectedBatchDelete = false
                    selectedBatchIds = emptySet()
                    batchSelectionMode = false
                    viewModel.deleteSmsSendBatches(ids) { expandedBatch = null }
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSelectedBatchDelete = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (confirmBatch != null) {
        AlertDialog(
            onDismissRequest = { confirmBatch = null },
            title = { Text("حذف نوبت ارسال؟") },
            text = { Text("تمام پیامک‌های ثبت‌شده در این نوبت حذف می‌شوند. اطلاعات مشتریان و پرداخت‌ها حذف نمی‌شوند.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = confirmBatch!!
                    confirmBatch = null
                    viewModel.deleteSmsSendBatch(id) { expandedBatch = null }
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmBatch = null }) { Text("انصراف") } }
        )
    }
    if (confirmOld) {
        AlertDialog(
            onDismissRequest = { confirmOld = false },
            title = { Text("حذف سوابق قدیمی؟") },
            text = { Text("فقط سوابق پیامکی که به نوبت ارسال متصل نیستند حذف می‌شوند.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmOld = false
                    viewModel.deleteSmsHistory(oldItems.map { it.id })
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmOld = false }) { Text("انصراف") } }
        )
    }
    if (confirmSelectedDelete) {
        AlertDialog(
            onDismissRequest = { confirmSelectedDelete = false },
            title = { Text("حذف پیامک‌های انتخاب‌شده؟") },
            text = { Text("${selectedIds.size} سابقه پیامک حذف می‌شود. اطلاعات مشتریان و پرداخت‌ها باقی می‌مانند.") },
            confirmButton = {
                TextButton(onClick = {
                    val ids = selectedIds.toList()
                    confirmSelectedDelete = false
                    selectedIds = emptySet()
                    viewModel.deleteSmsHistory(ids)
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmSelectedDelete = false }) { Text("انصراف") } }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SmsArchiveEntry(item: SmsHistory, selectionMode: Boolean, checked: Boolean, onLongPress: () -> Unit, onChecked: (Boolean) -> Unit) {
    val statusText = when (item.status) {
        "SENT", "DELIVERED" -> "موفق"
        "FAILED" -> "ناموفق"
        "DELIVERY_FAILED" -> "ارسال موفق؛ تحویل ناموفق"
        else -> "در انتظار نتیجه ارسال"
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).pointerInput(selectionMode, checked) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val longPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) return@withTimeoutOrNull false
                }
                false
            }
            if (longPress == null) {
                onLongPress()
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } else if (selectionMode) {
                onChecked(!checked)
            }
        }
    }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) Checkbox(checked = checked, onCheckedChange = onChecked)
            Column(Modifier.weight(1f)) {
                Text(item.customerName.ifBlank { "بدون نام" }, style = MaterialTheme.typography.titleSmall)
                Text(item.phoneNumber, style = MaterialTheme.typography.bodySmall)
            }
            Text(statusText, color = if (item.status == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        if (item.status == "FAILED" || item.status == "DELIVERY_FAILED") {
            Text("متن: ${item.message}", style = MaterialTheme.typography.bodySmall)
            Text("علت: ${item.errorReason.ifBlank { "علت خطا ثبت نشده است" }}", style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
    }
}
