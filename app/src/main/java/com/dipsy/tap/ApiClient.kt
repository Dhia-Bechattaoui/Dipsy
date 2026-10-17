package com.dipsy.tap

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class PayRequest(
    val amount: Long,
    val currency: String = "QAR"
)

data class Transaction(
    val id: String,
    val amount: Long,
    val currency: String,
    val status: String,
    val reason: String?,
    val createdAt: String,
    val updatedAt: String
)

data class TransactionList(
    val items: List<Transaction>,
    val count: Int
)

data class ApiError(
    val error: ErrorBody
) {
    data class ErrorBody(
        val code: String,
        val message: String
    )
}

interface PosApi {
    @POST("pay")
    suspend fun pay(
        @Body body: PayRequest,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): Response<Transaction>

    @GET("transactions/{id}")
    suspend fun transaction(
        @Path("id") id: String
    ): Response<Transaction>

    @GET("transactions")
    suspend fun transactions(
        @Query("limit") limit: Int = 20
    ): Response<TransactionList>
}

object ApiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("X-Api-Key", "test_key_123")
                .build()
            chain.proceed(request)
        }
        .build()

    val api: PosApi = Retrofit.Builder()
        .baseUrl("https://api.dibsy.cloud/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PosApi::class.java)
}
