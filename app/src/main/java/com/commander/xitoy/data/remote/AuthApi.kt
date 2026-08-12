package com.commander.xitoy.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class FcmTokenRequest(
    @SerializedName("telegram_id") val telegramId: String,
    @SerializedName("fcm_token")   val fcmToken: String
)

data class RegisterRequest(
    @SerializedName("fullname")       val fullname: String,
    @SerializedName("phone")          val phone: String,
    @SerializedName("password")       val password: String,
    @SerializedName("offer_accepted") val offerAccepted: Boolean,
    @SerializedName("offer_version")  val offerVersion: String
)

data class OfferResponse(
    @SerializedName("version")    val version: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("title")      val title: String,
    @SerializedName("content")    val content: String
)

data class LoginRequest(
    @SerializedName("phone")    val phone: String,
    @SerializedName("password") val password: String
)

data class ChangePasswordRequest(
    @SerializedName("user_id")      val userId: Long,
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class ChangePasswordResponse(
    @SerializedName("ok")    val ok: Boolean,
    @SerializedName("token") val token: String
)

data class AuthResponse(
    @SerializedName("ok")       val ok: Boolean,
    @SerializedName("user_id")  val userId: Long,
    @SerializedName("token")    val token: String,
    @SerializedName("fullname") val fullname: String,
    @SerializedName("phone")    val phone: String
)

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login-password")
    suspend fun loginPassword(@Body body: LoginRequest): AuthResponse

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): ChangePasswordResponse

    @POST("auth/register-fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Map<String, String>

    @GET("api/offer")
    suspend fun getOffer(): OfferResponse
}
