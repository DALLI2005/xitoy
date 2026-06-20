package com.commander.xitoy.domain.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/**
 * O'zbekiston Markaziy banki (CBU) ochiq API'sidan CNY (yuan) kursini oladi.
 * Endpoint: https://cbu.uz/uz/arkhiv-kursov-valyut/json/CNY/
 * Mavjud backend/Retrofit'ga tegmaydi — alohida, additiv.
 */
object CurrencyRateManager {

    data class CnyRate(
        val somText: String,     // "1 752"
        val percentText: String, // "0.42"
        val isUp: Boolean        // ▲ yoki ▼
    )

    private val _cnyRate = MutableStateFlow<CnyRate?>(null)
    val cnyRate: StateFlow<CnyRate?> = _cnyRate

    @Volatile
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        loaded = true
        try {
            val json = withContext(Dispatchers.IO) {
                val conn = (URL("https://cbu.uz/uz/arkhiv-kursov-valyut/json/CNY/").openConnection()
                        as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                }
                conn.inputStream.bufferedReader().use { it.readText() }
            }
            val obj = JSONArray(json).getJSONObject(0)
            val rate = obj.getString("Rate").toDouble()        // 1752.34
            val diff = obj.getString("Diff").toDoubleOrNull() ?: 0.0
            val prev = rate - diff
            val percent = if (prev != 0.0) (diff / prev) * 100.0 else 0.0
            _cnyRate.value = CnyRate(
                somText = formatSom(rate),
                percentText = String.format("%.2f", abs(percent)),
                isUp = diff >= 0
            )
        } catch (_: Exception) {
            loaded = false // keyingi safar qayta urinish uchun
        }
    }

    private fun formatSom(value: Double): String =
        value.toLong().toString().reversed().chunked(3).joinToString(" ").reversed()
}
