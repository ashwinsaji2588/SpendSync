package com.example.expensetracker

import com.example.expensetracker.data.TransactionType
import com.example.expensetracker.domain.SmsParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserEngineTest {

    private val parser = SmsParserEngine()

    @Test
    fun parse_expenseSms_returnsCorrectTransactionEntity() {
        val sms = "Rs. 450.00 debited from A/c *1234 at Zomato on 16-Aug-26."
        val transaction = parser.parse(sms, "HDFCBK")

        assertNotNull(transaction)
        assertEquals(450.00, transaction!!.amount, 0.001)
        assertEquals("Zomato", transaction.merchantName)
        assertEquals(TransactionType.EXPENSE, transaction.transactionType)
        assertEquals("Food", transaction.category)
        assertEquals("Account ending in 1234", transaction.accountName)
    }

    @Test
    fun parse_incomeSms_returnsCorrectTransactionEntity() {
        val sms = "INR 25,000.00 credited to account ending 5678."
        val transaction = parser.parse(sms, "ICICIB")

        assertNotNull(transaction)
        assertEquals(25000.00, transaction!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, transaction.transactionType)
        assertEquals("Account ending in 5678", transaction.accountName)
    }

    @Test
    fun parse_creditCardPayment_returnsTransferType() {
        val sms = "Rs. 5000.00 debited for credit card payment to Info: BookMyShow."
        val transaction = parser.parse(sms, "SBIINB")

        assertNotNull(transaction)
        assertEquals(5000.00, transaction!!.amount, 0.001)
        assertEquals(TransactionType.TRANSFER, transaction.transactionType)
        assertEquals("BookMyShow", transaction.merchantName)
        assertEquals("Entertainment", transaction.category)
    }

    @Test
    fun parse_nonTransactionSms_returnsNull() {
        val sms = "Your OTP for login is 123456."
        val transaction = parser.parse(sms, "ADM-OTP")

        assertNull(transaction)
    }
}
