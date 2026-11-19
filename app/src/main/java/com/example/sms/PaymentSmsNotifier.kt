package com.example.sms

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

/**
 * Shows a system notification for an incoming SMS that was queued for manual review.
 * The payment is never recorded here; the user still approves it in the app.
 */
object PaymentSmsNotifier {
    private const val TAG = "PaymentSmsNotifier"
    private const val CHANNEL_ID = "incoming_payment_sms"

    @SuppressLint("MissingPermission")
    fun show(
        context: Context,
        notificationId: Long,
        customerName: String,
        amountRial: Long,
        shamsiDate: String
    ) {
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Pending SMS $notificationId was saved, but POST_NOTIFICATIONS permission is not granted")
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Pending SMS $notificationId was saved, but app notifications are disabled in system settings")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager?.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
        ) {
            Log.w(TAG, "Pending SMS $notificationId was saved, but its notification channel is disabled")
            return
        }

        val amountText = if (amountRial > 0L) {
            "مبلغ پیامک: ${"%,d".format(amountRial)} ریال"
        } else {
            "مبلغ در پیامک مشخص نیست؛ برای بررسی وارد برنامه شوید."
        }

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId.toInt(),
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("پیامک پرداخت جدید از $customerName")
            .setContentText(amountText)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$amountText\nتاریخ: $shamsiDate\nبرای تأیید یا رد، وارد برنامه شوید."
                )
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId.toInt(), notification)
            Log.i(TAG, "System notification posted for pending SMS $notificationId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Pending SMS $notificationId was saved, but system rejected notification permission", e)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "پیامک‌های پرداخت مشتریان",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "اعلان پیامک‌های دریافتی که ممکن است پرداخت مشتری باشند"
        }
        manager.createNotificationChannel(channel)
    }
}
