package com.example.billing

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.entity.PurchaseState
import ir.cafebazaar.poolakey.request.PurchaseRequest

/** Poolakey wrapper for permanent, non-consumable capacity products. */
class BazaarBillingManager(context: Context) {
    companion object {
        private const val RSA_PUBLIC_KEY = "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwDGRVJ5fl7+fOKdl/BXxJQONGQ7KIbMFkiDCMQ4QXjHvfrls4RFi1sNEMF/S3KlIVklansSGOnel4GKeZZYQYkH1bupaAc5widU3DGU6NtW3aDWchwsczKqWv140ztFogy1xrk6+l/Cnw2tamUXX6NMSBCuPEQ9wo2QHaVm1CiWUNxvQzfxQ+VsQSUo24MWSPmg1/y+3wvkSKGAcr8nqkY6zev1OGsrSpO1v7H3jwsCAwEAAQ=="
        val PRODUCT_LIMITS = mapOf(
            "sara_hora_25" to 25,
            "sara_hora_50" to 50,
            "sara_hora_100" to 100,
            "sara_hora_unlimited" to Int.MAX_VALUE
        )
    }

    private val payment = Payment(
        context = context.applicationContext,
        config = PaymentConfiguration(
            localSecurityCheck = SecurityCheck.Enable(rsaPublicKey = RSA_PUBLIC_KEY)
        )
    )

    fun connect(
        onConnected: () -> Unit,
        onFailure: (Throwable) -> Unit
    ): Connection = payment.connect {
        connectionSucceed { onConnected() }
        connectionFailed { onFailure(it) }
        disconnected { }
    }

    fun restore(onResult: (List<PurchaseInfo>) -> Unit, onFailure: (Throwable) -> Unit) {
        payment.getPurchasedProducts {
            querySucceed { purchases ->
                onResult(purchases.filter { it.purchaseState == PurchaseState.PURCHASED && it.productId in PRODUCT_LIMITS })
            }
            queryFailed { onFailure(it) }
        }
    }

    fun purchase(
        registry: ActivityResultRegistry,
        productId: String,
        onSuccess: (PurchaseInfo) -> Unit,
        onFailure: (Throwable) -> Unit,
        onCanceled: () -> Unit
    ) {
        payment.purchaseProduct(
            registry = registry,
            request = PurchaseRequest(
                productId = productId,
                payload = "barberg-capacity",
                dynamicPriceToken = null
            )
        ) {
            purchaseSucceed { purchase ->
                if (purchase.purchaseState == PurchaseState.PURCHASED && purchase.productId in PRODUCT_LIMITS) {
                    onSuccess(purchase)
                } else onFailure(IllegalStateException("Purchase is not in PURCHASED state"))
            }
            purchaseCanceled { onCanceled() }
            purchaseFailed { onFailure(it) }
            failedToBeginFlow { onFailure(it) }
        }
    }

    fun disconnect(connection: Connection?) {
        connection?.disconnect()
    }
}
