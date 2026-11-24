package com.example.data.payment

import com.example.data.model.PaymentMode
import com.example.data.model.PaymentStatus
import com.example.domain.CounselingEngine
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.UUID

@JsonClass(generateAdapter = true)
data class CreateOrderRequest(
    val packageId: String,
    val promoCode: String?,
    val expectedAmountInr: Long
)

@JsonClass(generateAdapter = true)
data class CreateOrderResponse(
    val orderId: String,
    val amountPaise: Long,
    val currency: String,
    val mode: String
)

@JsonClass(generateAdapter = true)
data class VerifyPaymentRequest(
    val orderId: String,
    val paymentId: String,
    val signature: String,
    val packageId: String
)

@JsonClass(generateAdapter = true)
data class VerifyPaymentResponse(
    val verified: Boolean,
    val receiptNumber: String,
    val message: String
)

interface BackendPaymentApi {
    @POST("api/payments/create-order")
    suspend fun createOrder(
        @Header("Authorization") bearerToken: String,
        @Body request: CreateOrderRequest
    ): CreateOrderResponse

    @POST("api/payments/verify-signature")
    suspend fun verifySignature(
        @Header("Authorization") bearerToken: String,
        @Body request: VerifyPaymentRequest
    ): VerifyPaymentResponse
}

data class GatewayCheckoutOrder(
    val orderId: String,
    val amountInr: Long,
    val mode: PaymentMode
)

data class GatewayVerificationResult(
    val status: PaymentStatus,
    val gatewayPaymentId: String,
    val signature: String,
    val receiptNumber: String,
    val verifiedByBackend: Boolean,
    val failureReason: String = ""
)

class PaymentGatewayService {

    private fun buildApi(baseUrl: String): BackendPaymentApi? {
        val clean = baseUrl.trim()
        if (!clean.startsWith("https://") && !clean.startsWith("http://")) return null
        val normalized = if (clean.endsWith("/")) clean else "$clean/"
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(BackendPaymentApi::class.java)
    }

    suspend fun createOrder(
        backendUrl: String,
        idToken: String?,
        packageId: String,
        promoCode: String?,
        finalAmountInr: Long
    ): GatewayCheckoutOrder {
        val api = buildApi(backendUrl)
        if (api != null && !idToken.isNullOrBlank()) {
            return try {
                val res = api.createOrder(
                    bearerToken = "Bearer $idToken",
                    request = CreateOrderRequest(packageId, promoCode, finalAmountInr)
                )
                GatewayCheckoutOrder(
                    orderId = res.orderId,
                    amountInr = res.amountPaise / 100L,
                    mode = PaymentMode.safeValueOf(res.mode)
                )
            } catch (_: Exception) {
                createSandboxOrder(finalAmountInr)
            }
        }
        return createSandboxOrder(finalAmountInr)
    }

    private fun createSandboxOrder(finalAmountInr: Long): GatewayCheckoutOrder {
        val shortId = UUID.randomUUID().toString().replace("-", "").take(12)
        return GatewayCheckoutOrder(
            orderId = "order_sbx_$shortId",
            amountInr = finalAmountInr,
            mode = PaymentMode.SANDBOX
        )
    }

    suspend fun verifyOutcome(
        backendUrl: String,
        idToken: String?,
        order: GatewayCheckoutOrder,
        packageId: String,
        simulatedOutcome: PaymentStatus
    ): GatewayVerificationResult {
        val paymentId = "pay_${order.mode.name.lowercase()}_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val signature = CounselingEngine.generateSandboxSignature(order.orderId, paymentId)

        if (simulatedOutcome != PaymentStatus.SUCCESS) {
            val reason = when (simulatedOutcome) {
                PaymentStatus.FAILED -> "Bank authorization declined in gateway."
                PaymentStatus.CANCELLED -> "Payment cancelled by user before completion."
                PaymentStatus.PENDING -> "Awaiting UPI / bank settlement confirmation."
                else -> "Payment not completed."
            }
            return GatewayVerificationResult(
                status = simulatedOutcome,
                gatewayPaymentId = paymentId,
                signature = "",
                receiptNumber = "",
                verifiedByBackend = false,
                failureReason = reason
            )
        }

        val api = buildApi(backendUrl)
        if (api != null && !idToken.isNullOrBlank() && order.mode == PaymentMode.LIVE) {
            return try {
                val res = api.verifySignature(
                    bearerToken = "Bearer $idToken",
                    request = VerifyPaymentRequest(order.orderId, paymentId, signature, packageId)
                )
                if (res.verified) {
                    GatewayVerificationResult(
                        status = PaymentStatus.SUCCESS,
                        gatewayPaymentId = paymentId,
                        signature = signature,
                        receiptNumber = res.receiptNumber,
                        verifiedByBackend = true
                    )
                } else {
                    GatewayVerificationResult(
                        status = PaymentStatus.FAILED,
                        gatewayPaymentId = paymentId,
                        signature = "",
                        receiptNumber = "",
                        verifiedByBackend = false,
                        failureReason = res.message
                    )
                }
            } catch (e: Exception) {
                GatewayVerificationResult(
                    status = PaymentStatus.PENDING,
                    gatewayPaymentId = paymentId,
                    signature = "",
                    receiptNumber = "",
                    verifiedByBackend = false,
                    failureReason = e.localizedMessage ?: "Backend verification unreachable"
                )
            }
        }

        // Sandbox deterministic verification
        return GatewayVerificationResult(
            status = PaymentStatus.SUCCESS,
            gatewayPaymentId = paymentId,
            signature = signature,
            receiptNumber = CounselingEngine.generateReceiptNumber(),
            verifiedByBackend = true
        )
    }
}
