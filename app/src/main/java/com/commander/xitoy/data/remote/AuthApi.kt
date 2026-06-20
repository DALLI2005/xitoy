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

interface AuthApi {

    @GET("auth/start")
    suspend fun authStart(): AuthStartResponse

    @GET("auth/check")
    suspend fun authCheck(@Query("login_token") token: String): AuthCheckResponse

    @POST("auth/register-fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Map<String, String>
}
