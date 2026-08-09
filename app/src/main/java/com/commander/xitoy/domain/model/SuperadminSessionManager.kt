package com.commander.xitoy.domain.model

import android.content.Context
import android.content.SharedPreferences

/**
 * Superadmin panel sessiyasi — SessionManager.kt naqshidan, lekin alohida:
 * parol bilan ochilgach 3 kunga "qulf ochiladi", muddat o'tsa avtomatik
 * yopiladi (foydalanuvchi qayta kirishga urinsa parol yana so'raladi).
 * XitoyApp.onCreate ichida init(context) chaqirilishi shart.
 */
object SuperadminSessionManager {
    private const val PREFS = "dalli_superadmin_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_UNLOCKED_UNTIL = "unlocked_until"

    private const val UNLOCK_DURATION_MS = 3L * 24 * 60 * 60 * 1000 // 3 kun

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** Sessiya hali amal qilyaptimi (parol qayta so'ralmasdan kirish mumkinmi). */
    fun isUnlocked(): Boolean {
        val until = prefs?.getLong(KEY_UNLOCKED_UNTIL, 0L) ?: 0L
        return until > System.currentTimeMillis()
    }

    /** Amal qilayotgan token — muddat o'tgan bo'lsa null (qayta parol kerak). */
    fun tokenOrNull(): String? =
        if (isUnlocked()) prefs?.getString(KEY_TOKEN, null) else null

    /** Parol to'g'ri tekshirilgach chaqiriladi — 3 kunlik sessiyani boshlaydi. */
    fun unlock(token: String) {
        val until = System.currentTimeMillis() + UNLOCK_DURATION_MS
        prefs?.edit()
            ?.putString(KEY_TOKEN, token)
            ?.putLong(KEY_UNLOCKED_UNTIL, until)
            ?.apply()
    }

    /** Sessiyani darhol yopadi (masalan xavfsizlik uchun qo'lda chiqish). */
    fun lock() {
        prefs?.edit()?.clear()?.apply()
    }
}
