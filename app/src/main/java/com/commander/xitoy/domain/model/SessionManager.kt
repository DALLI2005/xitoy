package com.commander.xitoy.domain.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Saqlangan foydalanuvchi sessiyasi
data class UserSession(
    val telegramId: String,
    val ism: String,
    val username: String,
    val fullname: String = "",
    val phone: String = "",
    val address: String = "",
    val token: String = ""
)

// Ilova bo'ylab yagona login sessiyasi (Singleton) — SharedPreferences orqali saqlanadi.
// XitoyApp.onCreate ichida init(context) chaqirilishi shart.
object SessionManager {
    private const val PREFS = "dalli_session"
    private const val KEY_LOGGED_IN = "is_logged_in"
    private const val KEY_TG_ID = "telegram_id"
    private const val KEY_ISM = "ism"
    private const val KEY_USERNAME = "username"
    private const val KEY_FULLNAME = "fullname"
    private const val KEY_PHONE = "phone"
    private const val KEY_ADDRESS = "address"
    private const val KEY_TOKEN = "token"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    private val guestSession = UserSession(
        telegramId = "guest_user",
        ism = "Foydalanuvchi",
        username = "guest",
        fullname = "Mehmon",
        phone = "+998900000000",
        address = "Rishton",
        token = "guest_token"
    )

    private val _session = MutableStateFlow<UserSession?>(guestSession)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        if (p.getBoolean(KEY_LOGGED_IN, false)) {
            _session.value = UserSession(
                telegramId = p.getString(KEY_TG_ID, "") ?: "",
                ism = p.getString(KEY_ISM, "") ?: "",
                username = p.getString(KEY_USERNAME, "") ?: "",
                fullname = p.getString(KEY_FULLNAME, "") ?: "",
                phone = p.getString(KEY_PHONE, "") ?: "",
                address = p.getString(KEY_ADDRESS, "") ?: "",
                token = p.getString(KEY_TOKEN, "") ?: ""
            )
        } else {
            // Login/ro'yxatdan o'tish vaqtincha o'chirilgan — avtomatik mehmon seansi
            _session.value = guestSession
        }
    }

    val isLoggedIn: Boolean
        get() = true

    fun save(
        telegramId: String,
        ism: String,
        username: String,
        fullname: String = "",
        phone: String = "",
        address: String = "",
        token: String = ""
    ) {
        prefs?.edit()?.also { e ->
            e.putBoolean(KEY_LOGGED_IN, true)
            e.putString(KEY_TG_ID, telegramId)
            e.putString(KEY_ISM, ism)
            e.putString(KEY_USERNAME, username)
            e.putString(KEY_FULLNAME, fullname)
            e.putString(KEY_PHONE, phone)
            e.putString(KEY_ADDRESS, address)
            e.putString(KEY_TOKEN, token)
            e.apply()
        }
        _session.value = UserSession(telegramId, ism, username, fullname, phone, address, token)
        appContext?.let { OnboardingManager.markCompletedOnce(it) }
    }

    // Faqat tokenni yangilaydi (masalan, parol o'zgartirilganda) — boshqa maydonlar o'zgarmaydi
    fun updateToken(newToken: String) {
        val current = _session.value ?: return
        prefs?.edit()?.putString(KEY_TOKEN, newToken)?.apply()
        _session.value = current.copy(token = newToken)
    }

    fun logout() {
        prefs?.edit()?.clear()?.apply()
        _session.value = guestSession
    }
}
