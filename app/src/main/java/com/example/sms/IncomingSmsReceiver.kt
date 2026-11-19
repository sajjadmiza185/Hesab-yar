package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.AppDatabase
import com.example.model.PendingSmsNotification
import com.example.util.IncomingPaymentSmsParser
import com.example.util.JalaliCalendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest

class IncomingSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "SMS broadcast received, but PDU decoding failed", e)
            return
        }

        if (messages.isEmpty()) {
            Log.w(TAG, "SMS broadcast contained no decoded message parts")
            return
        }

        val sender = messages.firstNotNullOfOrNull { it.originatingAddress?.takeIf(String::isNotBlank) }.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val senderPhone = IncomingPaymentSmsParser.normalizePhone(sender)
                val senderDigits = IncomingPaymentSmsParser.normalizeDigits(sender).filter(Char::isDigit)
                val senderSuffix = senderDigits.takeLast(4)
                Log.i(TAG, "Incoming SMS: parts=${messages.size}, senderSuffix=****$senderSuffix, bodyLength=${body.length}")

                if (senderDigits.length < 7) {
                    Log.w(TAG, "SMS ignored: sender address does not contain at least 7 digits")
                    return@launch
                }

                val db = AppDatabase.getDatabase(context)
                val activeCustomers = db.customerDao().getAllCustomersList().filter { customer ->
                    customer.isActive && customer.phone.isNotBlank()
                }

                // Prefer an exact normalized phone match. Only use the historical last-seven
                // digit fallback when no exact match exists, and still require uniqueness.
                val exactMatches = if (senderPhone.isNotBlank()) {
                    activeCustomers.filter { customer ->
                        IncomingPaymentSmsParser.normalizePhone(customer.phone) == senderPhone
                    }
                } else {
                    emptyList()
                }

                val matchingCustomers = if (exactMatches.isNotEmpty()) {
                    exactMatches
                } else {
                    val lastSeven = senderDigits.takeLast(7)
                    activeCustomers.filter { customer ->
                        val customerDigits = IncomingPaymentSmsParser.normalizeDigits(customer.phone)
                            .filter(Char::isDigit)
                        customerDigits.length >= 7 && customerDigits.takeLast(7) == lastSeven
                    }
                }

                val customer = matchingCustomers.singleOrNull()
                if (customer == null) {
                    Log.w(
                        TAG,
                        "SMS not queued: activeCustomers=${activeCustomers.size}, exactMatches=${exactMatches.size}, fallbackMatches=${matchingCustomers.size}"
                    )
                    return@launch
                }

                val paymentIntent = IncomingPaymentSmsParser.isPaymentIntent(body)
                Log.i(TAG, "SMS sender matched customerId=${customer.id}; paymentIntent=$paymentIntent")
                if (!paymentIntent) {
                    Log.i(TAG, "SMS ignored as non-payment message")
                    return@launch
                }

                val parsed = IncomingPaymentSmsParser.parse(body)
                val today = JalaliCalendar.today()
                val amountRial = parsed?.amountRial ?: 0L
                val date = parsed?.shamsiDate ?: today.toString()
                val year = parsed?.year ?: today.year
                val month = parsed?.month ?: today.month
                val tracking = parsed?.tracking.orEmpty()

                // If no stable receipt reference exists, use the SMS timestamp so that
                // identical short messages sent on separate occasions are not collapsed.
                val identity = parsed?.dedupeKey ?: run {
                    val timestamp = messages.firstOrNull()?.timestampMillis
                        ?.takeIf { it > 0L } ?: System.currentTimeMillis()
                    val key = "$senderDigits|${body.trim()}|$timestamp"
                    MessageDigest.getInstance("SHA-256")
                        .digest(key.toByteArray(Charsets.UTF_8))
                        .joinToString("") { "%02x".format(it) }
                }

                val rowId = db.pendingSmsDao().insert(
                    PendingSmsNotification(
                        customerId = customer.id,
                        customerName = customer.name,
                        senderNumber = sender,
                        body = body,
                        amountRial = amountRial,
                        shamsiDate = date,
                        forMonth = month,
                        forYear = year,
                        trackingNumber = tracking,
                        dedupeKey = identity + "_" + senderDigits
                    )
                )

                if (rowId == -1L) {
                    Log.w(TAG, "Payment-intent SMS matched customerId=${customer.id}, but was already queued (dedupe conflict)")
                    return@launch
                }

                Log.i(TAG, "Payment SMS queued: pendingId=$rowId, customerId=${customer.id}, hasAmount=${amountRial > 0L}")
                PaymentSmsNotifier.show(
                    context = context,
                    notificationId = rowId,
                    customerName = customer.name,
                    amountRial = amountRial,
                    shamsiDate = date
                )
            } catch (e: Exception) {
                Log.e(TAG, "Incoming SMS processing failed before review notification could be completed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "IncomingSmsReceiver"
    }
}
