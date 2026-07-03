package com.commander.xitoy.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class AuthStartResponse(
    @SerializedName("login_token") val loginToken: String,
    @SerializedName("telegram_url") val telegramUrl: String
)

data class AuthCheckResponse(
    @SerializedName("status") val status: String,
    @SerializedName("telegram_id") val telegramId: String? = null,
    @SerializedName("ism") val ism: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("fullname") val fullname: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("location_link") val locationLink: String? = null
)

data class FcmTokenRequest(
    @SerializedName("telegram_id") val telegramId: String,
    @SerializedName("fcm_token")   val fcmToken: String
)

data class RegisterRequest(
    @SerializedName("fullname") val fullname: String,
    @SerializedName("phone")    val phone: String,
    @SerializedName("password") val password: String
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

    @GET("auth/start")
    suspend fun authStart(): AuthStartResponse

    @GET("auth/check")
    suspend fun authCheck(@Query("login_token") token: String): AuthCheckResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login-password")
    suspend fun loginPassword(@Body body: LoginRequest): AuthResponse

    @POST("auth/register-fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Map<String, String>
}
