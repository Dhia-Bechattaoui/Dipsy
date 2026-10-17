package com.dipsy.tap

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class ResultActivity : ComponentActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var ivStatus: ImageView
    private lateinit var tvResult: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvTransaction: TextView
    private lateinit var tvDetail: TextView
    private lateinit var btnNewPayment: Button

    private var pollingJob: Job? = null
    private var transactionId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        progressBar = findViewById(R.id.progressBar)
        ivStatus = findViewById(R.id.ivStatus)
        tvResult = findViewById(R.id.result)
        tvAmount = findViewById(R.id.amount2)
        tvTransaction = findViewById(R.id.transaction)
        tvDetail = findViewById(R.id.tvDetail)
        btnNewPayment = findViewById(R.id.btnNewPayment)

        transactionId = intent.getStringExtra("transaction_id") ?: ""
        val amount = intent.getLongExtra("amount", 0L)
        val initialStatus = intent.getStringExtra("status") ?: "pending"

        val qar = amount / 100.0
        tvAmount.text = "QAR " + String.format(Locale.US, "%,.2f", qar)
        tvTransaction.text = transactionId

        btnNewPayment.setOnClickListener {
            finish()
        }

        if (initialStatus == "pending" && transactionId.isNotEmpty()) {
            showPendingUI()
            startPolling()
        } else {
            val reason = intent.getStringExtra("reason")
            updateFinalUI(initialStatus, reason)
        }
    }

    override fun onStop() {
        super.onStop()
        pollingJob?.cancel()
    }

    private fun showPendingUI() {
        progressBar.visibility = View.VISIBLE
        ivStatus.visibility = View.GONE
        tvResult.text = "Processing..."
        tvTransaction.visibility = View.VISIBLE
        tvDetail.text = "Checking status every 1 s"
        tvDetail.visibility = View.VISIBLE
        btnNewPayment.visibility = View.GONE
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch {
            var attempts = 0
            while (isActive && attempts < 15) {
                delay(1000)
                attempts++
                try {
                    val response = ApiClient.api.transaction(transactionId)
                    if (response.isSuccessful && response.body() != null) {
                        val tx = response.body()!!
                        if (tx.status != "pending") {
                            updateFinalUI(tx.status, tx.reason)
                            return@launch
                        }
                    }
                } catch (_: Exception) {
                }
            }

            if (isActive) {
                showTimeoutUI()
            }
        }
    }

    private fun updateFinalUI(status: String, reason: String?) {
        progressBar.visibility = View.GONE
        ivStatus.visibility = View.VISIBLE
        btnNewPayment.visibility = View.VISIBLE

        if (status == "success") {
            ivStatus.setImageResource(R.drawable.ic_success)
            tvResult.text = "Payment approved"
            tvTransaction.visibility = View.VISIBLE
            tvDetail.visibility = View.GONE
        } else {
            ivStatus.setImageResource(R.drawable.ic_failed)
            tvResult.text = "Payment failed"
            tvTransaction.visibility = View.GONE
            if (!reason.isNullOrBlank()) {
                tvDetail.text = "Reason: $reason"
                tvDetail.visibility = View.VISIBLE
            } else {
                tvDetail.visibility = View.GONE
            }
        }
    }

    private fun showTimeoutUI() {
        progressBar.visibility = View.GONE
        ivStatus.visibility = View.GONE
        tvTransaction.visibility = View.GONE
        tvResult.text = "Still processing, check later"
        tvDetail.visibility = View.GONE
        btnNewPayment.visibility = View.VISIBLE
    }
}
