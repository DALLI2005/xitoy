package com.commander.xitoy.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class SuperadminLoginRequest(
    @SerializedName("password") val password: String
)

data class SuperadminUser(
    @SerializedName("telegram_id")   val telegramId: Long,
    @SerializedName("name")          val name: String,
    @SerializedName("is_superadmin") val isSuperadmin: Boolean
)

data class SuperadminLoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user")  val user: SuperadminUser
)

data class AdminFcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)

data class SuperadminPasswordChangeRequest(
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class SuperadminOrderItem(
    @SerializedName("nomi")    val nomi: String,
    @SerializedName("variant") val variant: String?,
    @SerializedName("razmer")  val razmer: String?,
    @SerializedName("soni")    val soni: Int,
    @SerializedName("narx")    val narx: Long,
    @SerializedName("rasm")    val rasm: String?
)

data class SuperadminOrderDetail(
    @SerializedName("order_id")      val orderId: String,
    @SerializedName("telegram_id")   val telegramId: String,
    @SerializedName("fullname")      val fullname: String,
    @SerializedName("phone")         val phone: String,
    @SerializedName("location_link") val locationLink: String,
    @SerializedName("jami_summa")    val jamiSumma: Long,
    @SerializedName("status")        val status: String,
    @SerializedName("created_at")    val createdAt: String,
    @SerializedName("items")         val items: List<SuperadminOrderItem>
)

// /api/orders — Google Sheets'dagi barcha buyurtmalar ro'yxati (mijoz ismi bilan,
// mijozning shaxsiy order/list dan farqli — OrderItem'da fullname yo'q).
data class SuperadminOrderListItem(
    @SerializedName("order_id")      val orderId: String,
    @SerializedName("telegram_id")   val telegramId: String,
    @SerializedName("fullname")      val fullname: String,
    @SerializedName("phone")         val phone: String,
    @SerializedName("mahsulotlar")   val mahsulotlar: String,
    @SerializedName("jami_summa")    val jamiSumma: Long,
    @SerializedName("holat")         val holat: String,
    @SerializedName("sana")          val sana: String
)

data class SuperadminOrderListResponse(
    @SerializedName("orders") val orders: List<SuperadminOrderListItem>
)

interface SuperadminApi {

    @POST("api/superadmin-login")
    suspend fun login(@Body body: SuperadminLoginRequest): SuperadminLoginResponse

    @POST("api/admin/register-fcm-token")
    suspend fun registerFcmToken(
        @Header("X-Admin-Token") token: String,
        @Body body: AdminFcmTokenRequest
    ): Map<String, Boolean>

    @POST("api/superadmin/change-password")
    suspend fun changePassword(
        @Header("X-Admin-Token") token: String,
        @Body body: SuperadminPasswordChangeRequest
    ): Map<String, Boolean>

    @GET("api/orders")
    suspend fun listOrders(@Header("X-Admin-Token") token: String): SuperadminOrderListResponse

    @GET("api/admin/orders/{orderId}")
    suspend fun getOrderDetail(
        @Header("X-Admin-Token") token: String,
        @Path("orderId") orderId: String
    ): SuperadminOrderDetail
}
