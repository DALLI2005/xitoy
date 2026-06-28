package com.commander.xitoy.data.remote

import com.commander.xitoy.domain.model.Product
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class OrderItemDetail(
    val nomi: String,
    val variant: String? = null,
    val razmer: String? = null,
    val soni: Int,
    val narx: Long,
    val rasm: String? = null
)

data class OrderRequest(
    val telegram_id: String,
    val fullname: String,
    val phone: String,
    val location_link: String,
    val mahsulotlar: String,
    val jami_summa: Long,
    val mahsulot_rasm: String? = null,
    val mahsulotlar_royxati: List<OrderItemDetail> = emptyList()
)

data class OrderResponse(
    val status: String,
    val order_id: String
)

data class OrderItem(
    val order_id: String,
    val mahsulotlar: String,
    val jami_summa: Long,
    val holat: String,
    val sana: String
)

data class OrderListResponse(
    val orders: List<OrderItem>
)

data class PaymentCardInfo(
    val card_number: String,
    val card_holder: String
)

data class ReceiptResponse(
    val status: String
)

data class CartSyncRequest(
    val telegram_id:   String,
    val mahsulot_nomi: String
)

data class CartCancelRequest(
    val telegram_id: String
)

interface OrderApi {
    @POST("order/create")
    suspend fun createOrder(@Body order: OrderRequest): OrderResponse

    @GET("order/list")
    suspend fun listOrders(@Query("telegram_id") telegramId: String): OrderListResponse

    @GET("order/payment-card")
    suspend fun getPaymentCard(): PaymentCardInfo

    @Multipart
    @POST("order/upload-receipt")
    suspend fun uploadReceipt(
        @Query("order_id") orderId: String,
        @Query("telegram_id") telegramId: String,
        @Part file: MultipartBody.Part
    ): ReceiptResponse

    @POST("cart/sync")
    suspend fun syncCart(@Body request: CartSyncRequest): Map<String, String>

    @POST("cart/cancel-reminder")
    suspend fun cancelCartReminder(@Body request: CartCancelRequest): Map<String, String>

    @GET("api/favorites")
    suspend fun getFavorites(@Query("telegram_id") telegramId: String): List<Product>

    @POST("api/favorites/{productId}")
    suspend fun addFavorite(
        @Path("productId") productId: String,
        @Query("telegram_id") telegramId: String
    ): Map<String, Any>

    @DELETE("api/favorites/{productId}")
    suspend fun removeFavorite(
        @Path("productId") productId: String,
        @Query("telegram_id") telegramId: String
    ): Map<String, Any>
}
