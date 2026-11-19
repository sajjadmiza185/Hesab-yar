package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.Customer
import com.example.model.CustomerGroup
import com.example.model.MonthlyCharge
import com.example.model.PaymentTransaction
import com.example.model.PendingSmsNotification
import com.example.model.SmsHistory
import com.example.model.SmsSendBatch
import com.example.model.Subscription

@Database(
    entities = [
        CustomerGroup::class,
        Customer::class,
        MonthlyCharge::class,
        PaymentTransaction::class,
        PendingSmsNotification::class,
        SmsHistory::class,
        SmsSendBatch::class,
        Subscription::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerGroupDao(): CustomerGroupDao
    abstract fun customerDao(): CustomerDao
    abstract fun monthlyChargeDao(): MonthlyChargeDao
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingSmsDao(): PendingSmsDao
    abstract fun smsHistoryDao(): SmsHistoryDao
    abstract fun smsSendBatchDao(): SmsSendBatchDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS pending_sms_notifications (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, customerId INTEGER NOT NULL, customerName TEXT NOT NULL, senderNumber TEXT NOT NULL, body TEXT NOT NULL, amountRial INTEGER NOT NULL, shamsiDate TEXT NOT NULL, forMonth INTEGER NOT NULL, forYear INTEGER NOT NULL, trackingNumber TEXT NOT NULL, receivedAt INTEGER NOT NULL, status TEXT NOT NULL, dedupeKey TEXT NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pending_sms_notifications_dedupeKey ON pending_sms_notifications(dedupeKey)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sms_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        customerId INTEGER NOT NULL,
                        customerName TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        message TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        sentAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        simSubscriptionId INTEGER NOT NULL,
                        expectedParts INTEGER NOT NULL,
                        completedSentParts INTEGER NOT NULL,
                        failedSentParts INTEGER NOT NULL,
                        completedDeliveryParts INTEGER NOT NULL,
                        failedDeliveryParts INTEGER NOT NULL,
                        errorReason TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_history_customerId ON sms_history(customerId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_history_sentAt ON sms_history(sentAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_history_status ON sms_history(status)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE customer_groups ADD COLUMN leaderCustomerId INTEGER")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE customers ADD COLUMN username TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE customers ADD COLUMN passwordEncrypted TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sms_send_batches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        title TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_send_batches_createdAt ON sms_send_batches(createdAt)")
                database.execSQL("ALTER TABLE sms_history ADD COLUMN batchId INTEGER")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_history_batchId ON sms_history(batchId)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS subscription (id INTEGER NOT NULL, planCode TEXT NOT NULL, customerLimit INTEGER NOT NULL, purchaseToken TEXT NOT NULL, productId TEXT NOT NULL, purchasedAt INTEGER NOT NULL, isVerified INTEGER NOT NULL, PRIMARY KEY(id))")
                database.execSQL("INSERT OR IGNORE INTO subscription (id, planCode, customerLimit, purchaseToken, productId, purchasedAt, isVerified) VALUES (1, 'FREE', 5, '', '', 0, 0)")
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "barberg_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


