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
        // 2. Build the PayRequest data object
        val request = PayRequest(amount = amount, currency = currency)
        // 3. Call api.pay() to start the network request
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
                // Access 'api.pay()' and pass the request object
                val response = api.pay(
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


data class PayRequest(val amount: Long, val currency: String = "QAR")
data class Transaction(
    val id: String,
    val amount: Long,
    val currency: String,
    val status: String, // "pending" | "success" | "failed"
    val reason: String?,
    val createdAt: String,
    val updatedAt: String,
)

data class TransactionList(val items: List<Transaction>, val count: Int)
data class ApiError(val error: ErrorBody) {
    data class ErrorBody(val code: String, val message: String)
}

interface PosApi {
    @POST("pay")
    suspend fun pay(
        @Body body: PayRequest,
        @Header("Idempotency-Key") idempotencyKey: String?,
    ): Response<Transaction>

    @GET("transactions/{id}")
    suspend fun transaction(@Path("id") id: String): Response<Transaction>

    @GET("transactions")
    suspend fun transactions(@Query("limit") limit: Int = 20):
            Response<TransactionList>
}

val client = OkHttpClient.Builder()
    .addInterceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header(
                    "X-Api-Key",
                    "test_key_123"
                )
                .build()
        )
    }
    .build()
val api: PosApi = Retrofit.Builder()
    .baseUrl("https://api.dibsy.cloud/")
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(PosApi::class.java)
