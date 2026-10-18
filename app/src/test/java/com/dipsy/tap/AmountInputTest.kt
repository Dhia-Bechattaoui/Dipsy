package com.dipsy.tap

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountInputTest {

    @Test
    fun `sequential digit entry shifts cents correctly like a card terminal`() {
        var amount = 0L

        // Typing 1 -> 0.01
        amount = appendDigit(amount, 1)
        assertEquals(1L, amount)
        assertEquals("QAR 0.01", formatQar(amount))

        // Typing 2 -> 0.12
        amount = appendDigit(amount, 2)
        assertEquals(12L, amount)
        assertEquals("QAR 0.12", formatQar(amount))

        // Typing 5 -> 1.25
        amount = appendDigit(amount, 5)
        assertEquals(125L, amount)
        assertEquals("QAR 1.25", formatQar(amount))

        // Typing 0 -> 12.50
        amount = appendDigit(amount, 0)
        assertEquals(1250L, amount)
        assertEquals("QAR 12.50", formatQar(amount))
    }

    @Test
    fun `leading zeros do not increment amount`() {
        var amount = 0L
        amount = appendDigit(amount, 0)
        assertEquals(0L, amount)
        assertEquals("QAR 0.00", formatQar(amount))
    }

    @Test
    fun `amount is capped at 7 digits and ignores extra digits`() {
        var amount = 0L

        // Enter 7 nines: 99,999.99 (9999999 cents)
        repeat(7) {
            amount = appendDigit(amount, 9)
        }
        assertEquals(9_999_999L, amount)
        assertEquals("QAR 99,999.99", formatQar(amount))

        // Any 8th digit must be ignored
        val capped = appendDigit(amount, 1)
        assertEquals(9_999_999L, capped)
        assertEquals("QAR 99,999.99", formatQar(capped))
    }

    @Test
    fun `currency formatting outputs commas and standard 2 decimal places`() {
        assertEquals("QAR 0.00", formatQar(0L))
        assertEquals("QAR 25.50", formatQar(2550L))
        assertEquals("QAR 1,500.00", formatQar(150000L))
        assertEquals("QAR 99,999.99", formatQar(9999999L))
    }

    @Test
    fun `formatAmount supports multi-currency formatting for USD, EUR, and QAR`() {
        assertEquals("USD 0.00", formatAmount(0L, "USD"))
        assertEquals("USD 25.50", formatAmount(2550L, "USD"))
        assertEquals("EUR 1,500.00", formatAmount(150000L, "EUR"))
        assertEquals("QAR 99,999.99", formatAmount(9999999L, "QAR"))
    }
}
