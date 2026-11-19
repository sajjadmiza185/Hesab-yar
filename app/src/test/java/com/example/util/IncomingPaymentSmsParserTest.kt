package com.example.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingPaymentSmsParserTest {

    @Test
    fun recognizesInformalCustomerPaymentMessageWithoutAmount() {
        val message = "سلام داداش خوبی . برات واریز کردم.این ماه شارژ شده؟"
        assertTrue(IncomingPaymentSmsParser.isPaymentIntent(message))
        assertNull(IncomingPaymentSmsParser.parse(message))
    }

    @Test
    fun recognizesShortPaymentMessage() {
        assertTrue(IncomingPaymentSmsParser.isPaymentIntent("سلام واریز شد"))
    }

    @Test
    fun recognizesArabicYehAndPersianHalfSpaceVariants() {
        assertTrue(IncomingPaymentSmsParser.isPaymentIntent("برات واريز‌کردم"))
    }

    @Test
    fun doesNotTreatOrdinaryGreetingAsPayment() {
        assertFalse(IncomingPaymentSmsParser.isPaymentIntent("سلام داداش خوبی؟ این ماه شارژ شده؟"))
    }
}
