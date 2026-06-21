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
    val address: String = ""
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

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    private val _session = MutableStateFlow<UserSession?>(null)
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
                address = p.getString(KEY_ADDRESS, "") ?: ""
            )
        }
    }

    val isLoggedIn: Boolean
        get() = _session.value != null

    fun save(
        telegramId: String,
        ism: String,
        username: String,
        fullname: String = "",
        phone: String = "",
        address: String = ""
    ) {
        prefs?.edit()?.also { e ->
            e.putBoolean(KEY_LOGGED_IN, true)
            e.putString(KEY_TG_ID, telegramId)
            e.putString(KEY_ISM, ism)
            e.putString(KEY_USERNAME, username)
            e.putString(KEY_FULLNAME, fullname)
            e.putString(KEY_PHONE, phone)
            e.putString(KEY_ADDRESS, address)
            e.apply()
        }
        _session.value = UserSession(telegramId, ism, username, fullname, phone, address)
        appContext?.let { OnboardingManager.markCompletedOnce(it) }
    }

    fun logout() {
        prefs?.edit()?.clear()?.apply()
        _session.value = null
    }
}
