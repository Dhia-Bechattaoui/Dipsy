package com.dipsy.tap

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import java.util.UUID
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


class ResultActivity : ComponentActivity() {
    lateinit var idp: String
    lateinit var progressBar: ProgressBar

    lateinit var tvResult: TextView
    lateinit var tvTransaction: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result)
        val amount = intent.getLongExtra("amount", 0L)
        val currency = intent.getStringExtra("currency") ?: "QAR"
        val request = PayRequest(amount = amount, currency = currency)
        tvResult = findViewById<TextView>(R.id.result)
        progressBar = findViewById(R.id.progressBar)
        tvTransaction = findViewById<TextView>(R.id.transaction)

        startPayment(request)
    }

    private fun startPayment(request: PayRequest) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                tvResult.setText("Progressing....")
                val response = ApiClient.api.pay(
                    body = request,
                    idempotencyKey = UUID.randomUUID().toString()
                )
                val id = response.body()?.id
                tvTransaction.setText("tr_$id")
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val transaction = response.body()
                    tvResult.setText("Payment Approval")
                    Toast.makeText(this@ResultActivity, "Payment: ${transaction?.status}", Toast.LENGTH_LONG).show()
                } else {
                    tvResult.setText("Payment failed")
                    Toast.makeText(this@ResultActivity, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvResult.setText("Payment failed")
                Toast.makeText(this@ResultActivity, "Request Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
