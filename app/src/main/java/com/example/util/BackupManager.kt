package com.example.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.FileProvider
import com.example.data.BackupContainer
import com.example.model.Customer
import com.example.model.CustomerGroup
import com.example.model.MonthlyCharge
import com.example.model.PaymentTransaction
import com.example.model.PendingSmsNotification
import com.example.model.SmsHistory
import com.example.model.SmsSendBatch
import com.example.model.Subscription
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/** One app preference, stored with its type so it can be written back exactly. */
data class BackupPreference(val key: String, val type: String, val value: String)

object BackupManager {

    const val FORMAT_VERSION = 2

    // Keys that are device-bound or security-sensitive are never exported or imported.
    private fun isExcludedKey(key: String): Boolean {
        val k = key.lowercase()
        return k == "manager_access_granted" || k.contains("pin") || k.contains("hash") || k.contains("salt")
    }

    fun exportToJson(data: BackupContainer): String {
        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("appName", "حساب یار")
        root.put("exportDate", JalaliCalendar.formatShort(JalaliCalendar.today(), false))
        root.put("timestamp", System.currentTimeMillis())

        root.put("groups", JSONArray().apply {
            data.groups.forEach { g ->
                put(
                    JSONObject()
                        .put("id", g.id)
                        .put("name", g.name)
                        .put("description", g.description)
                        .put("colorHex", g.colorHex)
                        .put("leaderCustomerId", g.leaderCustomerId ?: JSONObject.NULL)
                        .put("createdAt", g.createdAt)
                )
            }
        })

        root.put("customers", JSONArray().apply {
            data.customers.forEach { c ->
                put(
                    JSONObject()
                        .put("id", c.id)
                        .put("name", c.name)
                        .put("phone", c.phone)
                        .put("groupId", c.groupId ?: JSONObject.NULL)
                        .put("address", c.address)
                        .put("notes", c.notes)
                        .put("isActive", c.isActive)
                        .put("createdAt", c.createdAt)
                        .put("username", c.username)
                        .put("passwordEncrypted", c.passwordEncrypted)
                )
            }
        })

        root.put("monthlyCharges", JSONArray().apply {
            data.monthlyCharges.forEach { m ->
                put(
                    JSONObject()
                        .put("id", m.id)
                        .put("customerId", m.customerId)
                        .put("year", m.year)
                        .put("month", m.month)
                        .put("amount", m.amount)
                        .put("title", m.title)
                        .put("notes", m.notes)
                        .put("updatedAt", m.updatedAt)
                )
            }
        })

        root.put("transactions", JSONArray().apply {
            data.transactions.forEach { t ->
                put(
                    JSONObject()
                        .put("id", t.id)
                        .put("customerId", t.customerId)
                        .put("type", t.type)
                        .put("amount", t.amount)
                        .put("shamsiDate", t.shamsiDate)
                        .put("timestamp", t.timestamp)
                        .put("paymentMethod", t.paymentMethod)
                        .put("referenceNumber", t.referenceNumber)
                        .put("forMonth", t.forMonth ?: JSONObject.NULL)
                        .put("forYear", t.forYear ?: JSONObject.NULL)
                        .put("notes", t.notes)
                )
            }
        })

        root.put("smsSendBatches", JSONArray().apply {
            data.smsSendBatches.forEach { b ->
                put(JSONObject().put("id", b.id).put("createdAt", b.createdAt).put("title", b.title))
            }
        })

        root.put("smsHistory", JSONArray().apply {
            data.smsHistory.forEach { h ->
                put(
                    JSONObject()
                        .put("id", h.id)
                        .put("customerId", h.customerId)
                        .put("customerName", h.customerName)
                        .put("phoneNumber", h.phoneNumber)
                        .put("message", h.message)
                        .put("amount", h.amount)
                        .put("sentAt", h.sentAt)
                        .put("status", h.status)
                        .put("simSubscriptionId", h.simSubscriptionId)
                        .put("expectedParts", h.expectedParts)
                        .put("completedSentParts", h.completedSentParts)
                        .put("failedSentParts", h.failedSentParts)
                        .put("completedDeliveryParts", h.completedDeliveryParts)
                        .put("failedDeliveryParts", h.failedDeliveryParts)
                        .put("errorReason", h.errorReason)
                        .put("batchId", h.batchId ?: JSONObject.NULL)
                )
            }
        })

        root.put("pendingSmsNotifications", JSONArray().apply {
            data.pendingSmsNotifications.forEach { n ->
                put(
                    JSONObject()
                        .put("id", n.id)
                        .put("customerId", n.customerId)
                        .put("customerName", n.customerName)
                        .put("senderNumber", n.senderNumber)
                        .put("body", n.body)
                        .put("amountRial", n.amountRial)
                        .put("shamsiDate", n.shamsiDate)
                        .put("forMonth", n.forMonth)
                        .put("forYear", n.forYear)
                        .put("trackingNumber", n.trackingNumber)
                        .put("receivedAt", n.receivedAt)
                        .put("status", n.status)
                        .put("dedupeKey", n.dedupeKey)
                )
            }
        })

        data.subscription?.let { s ->
            root.put(
                "subscription",
                JSONObject()
                    .put("planCode", s.planCode)
                    .put("customerLimit", s.customerLimit)
                    .put("purchaseToken", s.purchaseToken)
                    .put("productId", s.productId)
                    .put("purchasedAt", s.purchasedAt)
                    .put("isVerified", s.isVerified)
            )
        }

        root.put("preferences", JSONArray().apply {
            data.preferences.forEach { p ->
                put(JSONObject().put("key", p.key).put("type", p.type).put("value", p.value))
            }
        })

        return root.toString(2)
    }

    /** Reads every app preference (except excluded security keys) with its type. */
    fun readPreferences(prefs: SharedPreferences): List<BackupPreference> =
        prefs.all.mapNotNull { (key, value) ->
            if (isExcludedKey(key)) return@mapNotNull null
            when (value) {
                is Boolean -> BackupPreference(key, "boolean", value.toString())
                is Int -> BackupPreference(key, "int", value.toString())
                is Long -> BackupPreference(key, "long", value.toString())
                is Float -> BackupPreference(key, "float", value.toString())
                is String -> BackupPreference(key, "string", value)
                is Set<*> -> BackupPreference(key, "stringSet", JSONArray(value.map { it.toString() }).toString())
                else -> null
            }
        }

    /** Writes preferences back from a backup. Excluded keys are skipped even if present in the file. */
    fun writePreferences(prefs: SharedPreferences, items: List<BackupPreference>) {
        val editor = prefs.edit()
        items.forEach { item ->
            if (isExcludedKey(item.key)) return@forEach
            runCatching {
                when (item.type) {
                    "boolean" -> editor.putBoolean(item.key, item.value.toBoolean())
                    "int" -> editor.putInt(item.key, item.value.toInt())
                    "long" -> editor.putLong(item.key, item.value.toLong())
                    "float" -> editor.putFloat(item.key, item.value.toFloat())
                    "string" -> editor.putString(item.key, item.value)
                    "stringSet" -> {
                        val arr = JSONArray(item.value)
                        editor.putStringSet(item.key, (0 until arr.length()).map { arr.getString(it) }.toSet())
                    }
                    else -> Unit
                }
            }
        }
        editor.apply()
    }

    fun parseFromJson(jsonString: String): BackupContainer {
        val root = JSONObject(jsonString)

        val groups = root.optJSONArray("groups").mapObjects { o ->
            CustomerGroup(
                id = o.optLong("id", 0L),
                name = o.getString("name"),
                description = o.optString("description", ""),
                colorHex = o.optString("colorHex", "#1E88E5"),
                leaderCustomerId = o.longOrNull("leaderCustomerId"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val customers = root.optJSONArray("customers").mapObjects { o ->
            Customer(
                id = o.optLong("id", 0L),
                name = o.getString("name"),
                phone = o.optString("phone", ""),
                groupId = o.longOrNull("groupId"),
                address = o.optString("address", ""),
                notes = o.optString("notes", ""),
                isActive = o.optBoolean("isActive", true),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                username = o.optString("username", ""),
                passwordEncrypted = o.optString("passwordEncrypted", "")
            )
        }

        val charges = root.optJSONArray("monthlyCharges").mapObjects { o ->
            MonthlyCharge(
                id = o.optLong("id", 0L),
                customerId = o.getLong("customerId"),
                year = o.getInt("year"),
                month = o.getInt("month"),
                amount = o.getLong("amount"),
                title = o.optString("title", ""),
                notes = o.optString("notes", ""),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        val transactions = root.optJSONArray("transactions").mapObjects { o ->
            PaymentTransaction(
                id = o.optLong("id", 0L),
                customerId = o.getLong("customerId"),
                type = o.optString("type", "PAYMENT"),
                amount = o.getLong("amount"),
                shamsiDate = o.getString("shamsiDate"),
                timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                paymentMethod = o.optString("paymentMethod", "کارت به کارت"),
                referenceNumber = o.optString("referenceNumber", ""),
                forMonth = o.intOrNull("forMonth"),
                forYear = o.intOrNull("forYear"),
                notes = o.optString("notes", "")
            )
        }

        val batches = root.optJSONArray("smsSendBatches").mapObjects { o ->
            SmsSendBatch(
                id = o.optLong("id", 0L),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                title = o.optString("title", "")
            )
        }

        val smsHistory = root.optJSONArray("smsHistory").mapObjects { o ->
            SmsHistory(
                id = o.optLong("id", 0L),
                customerId = o.getLong("customerId"),
                customerName = o.optString("customerName", ""),
                phoneNumber = o.optString("phoneNumber", ""),
                message = o.optString("message", ""),
                amount = o.optLong("amount", 0L),
                sentAt = o.optLong("sentAt", System.currentTimeMillis()),
                status = o.optString("status", "PENDING"),
                simSubscriptionId = o.optInt("simSubscriptionId", -1),
                expectedParts = o.optInt("expectedParts", 1),
                completedSentParts = o.optInt("completedSentParts", 0),
                failedSentParts = o.optInt("failedSentParts", 0),
                completedDeliveryParts = o.optInt("completedDeliveryParts", 0),
                failedDeliveryParts = o.optInt("failedDeliveryParts", 0),
                errorReason = o.optString("errorReason", ""),
                batchId = o.longOrNull("batchId")
            )
        }

        val pendingSms = root.optJSONArray("pendingSmsNotifications").mapObjects { o ->
            val receivedAt = o.optLong("receivedAt", System.currentTimeMillis())
            PendingSmsNotification(
                customerId = o.getLong("customerId"),
                customerName = o.optString("customerName", ""),
                senderNumber = o.optString("senderNumber", ""),
                body = o.optString("body", ""),
                amountRial = o.optLong("amountRial", 0L),
                shamsiDate = o.optString("shamsiDate", ""),
                forMonth = o.optInt("forMonth", 1),
                forYear = o.optInt("forYear", 1400),
                trackingNumber = o.optString("trackingNumber", ""),
                receivedAt = receivedAt,
                status = o.optString("status", "PENDING"),
                dedupeKey = o.optString("dedupeKey", "").ifBlank {
                    "restored_${o.optLong("id", 0L)}_$receivedAt"
                }
            )
        }

        val subscription = root.optJSONObject("subscription")?.let { o ->
            Subscription(
                id = 1,
                planCode = o.optString("planCode", "FREE"),
                customerLimit = o.optInt("customerLimit", 5),
                purchaseToken = o.optString("purchaseToken", ""),
                productId = o.optString("productId", ""),
                purchasedAt = o.optLong("purchasedAt", 0L),
                isVerified = o.optBoolean("isVerified", false)
            )
        }

        val preferences = root.optJSONArray("preferences").mapObjects { o ->
            BackupPreference(
                key = o.getString("key"),
                type = o.optString("type", "string"),
                value = o.optString("value", "")
            )
        }

        return BackupContainer(
            groups = groups,
            customers = customers,
            monthlyCharges = charges,
            transactions = transactions,
            smsSendBatches = batches,
            smsHistory = smsHistory,
            pendingSmsNotifications = pendingSms,
            subscription = subscription,
            preferences = preferences
        )
    }

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).map { transform(getJSONObject(it)) }
    }

    private fun JSONObject.longOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONObject.intOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) getInt(key) else null

    fun shareBackupFile(context: Context, jsonContent: String, fileName: String) {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(jsonContent.toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری و ذخیره فایل پشتیبان باربرگ")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
