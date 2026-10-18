package com.dipsy.tap

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {

    private var amount: Long = 0L
    private var currency: String = "QAR"
    private var idempotencyKey: String = UUID.randomUUID().toString()
    private var isSending: Boolean = false

    private lateinit var tvAmount: TextView
    private lateinit var btnOK: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutError: View
    private lateinit var tvError: TextView
    private lateinit var btnRetry: Button
    private lateinit var btnCurrencyQar: TextView
    private lateinit var btnCurrencyUsd: TextView
    private lateinit var btnCurrencyEur: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAmount = findViewById(R.id.amount)
        btnOK = findViewById(R.id.btnOK)
        progressBar = findViewById(R.id.progressBar)
        layoutError = findViewById(R.id.layoutError)
        tvError = findViewById(R.id.tvError)
        btnRetry = findViewById(R.id.btnRetry)
        btnCurrencyQar = findViewById(R.id.btnCurrencyQar)
        btnCurrencyUsd = findViewById(R.id.btnCurrencyUsd)
        btnCurrencyEur = findViewById(R.id.btnCurrencyEur)

        btnCurrencyQar.setOnClickListener { setCurrency("QAR") }
        btnCurrencyUsd.setOnClickListener { setCurrency("USD") }
        btnCurrencyEur.setOnClickListener { setCurrency("EUR") }

        findViewById<TextView>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        setupNumpad()
        updateCurrencyUI()
        updateDisplay()
    }

    private fun setupNumpad() {
        val digitButtons = mapOf(
            R.id.btn0 to 0,
            R.id.btn1 to 1,
            R.id.btn2 to 2,
            R.id.btn3 to 3,
            R.id.btn4 to 4,
            R.id.btn5 to 5,
            R.id.btn6 to 6,
            R.id.btn7 to 7,
            R.id.btn8 to 8,
            R.id.btn9 to 9
        )

        for ((id, digit) in digitButtons) {
            findViewById<Button>(id).setOnClickListener {
                if (!isSending) {
                    addDigit(digit)
                }
            }
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            if (!isSending) {
                amount = 0L
                updateDisplay()
                resetErrorAndKey()
            }
        }

        btnOK.setOnClickListener {
            sendPayment()
        }

        btnRetry.setOnClickListener {
            sendPayment()
        }
    }

    private fun addDigit(value: Int) {
        val next = appendDigit(amount, value)
        if (next != amount) {
            amount = next
            updateDisplay()
            if (layoutError.visibility == View.VISIBLE) {
                resetErrorAndKey()
            }
        }
    }

    private fun setCurrency(newCurrency: String) {
        if (isSending || currency == newCurrency) return
        currency = newCurrency
        updateCurrencyUI()
        updateDisplay()
        resetErrorAndKey()
    }

    private fun updateCurrencyUI() {
        val buttons = listOf(
            btnCurrencyQar to "QAR",
            btnCurrencyUsd to "USD",
            btnCurrencyEur to "EUR"
        )
        for ((button, curr) in buttons) {
            val isSelected = (curr == currency)
            button.setBackgroundResource(
                if (isSelected) R.drawable.bg_currency_selected else R.drawable.bg_currency_unselected
            )
            button.setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#555555"))
        }
    }

    private fun updateDisplay() {
        tvAmount.text = formatAmount(amount, currency)
        btnOK.isEnabled = amount > 0 && !isSending
    }

    private fun resetErrorAndKey() {
        idempotencyKey = UUID.randomUUID().toString()
        layoutError.visibility = View.GONE
    }

    private fun sendPayment() {
        if (amount <= 0 || isSending) return

        isSending = true
        btnOK.isEnabled = false
        progressBar.visibility = View.VISIBLE
        layoutError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = ApiClient.api.pay(
                    body = PayRequest(amount = amount, currency = currency),
                    idempotencyKey = idempotencyKey
                )

                progressBar.visibility = View.GONE
                isSending = false
                btnOK.isEnabled = amount > 0

                if (response.isSuccessful && response.body() != null) {
                    val transaction = response.body()!!
                    val intent = Intent(this@MainActivity, ResultActivity::class.java).apply {
                        putExtra("transaction_id", transaction.id)
                        putExtra("amount", transaction.amount)
                        putExtra("currency", transaction.currency)
                        putExtra("status", transaction.status)
                    }
                    startActivity(intent)
                    amount = 0L
                    updateDisplay()
                    resetErrorAndKey()
                } else {
                    val errorMsg = parseErrorMessage(response.errorBody()?.string())
                        ?: "Request failed (${response.code()})"
                    showError(errorMsg, canRetry = (response.code() == 500))
                }
            } catch (_: Exception) {
                progressBar.visibility = View.GONE
                isSending = false
                btnOK.isEnabled = amount > 0
                showError("No network connection", canRetry = true)
            }
        }
    }

    private fun showError(message: String, canRetry: Boolean) {
        tvError.text = message
        btnRetry.visibility = if (canRetry) View.VISIBLE else View.GONE
        layoutError.visibility = View.VISIBLE
    }

    private fun parseErrorMessage(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            val errorObj = Gson().fromJson(json, ApiError::class.java)
            errorObj.error.message
        } catch (_: Exception) {
            null
        }
    }
}

internal fun appendDigit(current: Long, digit: Int): Long {
    val next = (current * 10) + digit
    return if (next <= 99_999_99L) next else current
}

internal fun formatAmount(cents: Long, currency: String = "QAR"): String {
    return "$currency " + String.format(Locale.US, "%,.2f", cents / 100.0)
}

internal fun formatQar(cents: Long): String {
    return formatAmount(cents, "QAR")
}
