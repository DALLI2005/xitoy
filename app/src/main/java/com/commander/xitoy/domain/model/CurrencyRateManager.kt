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
 * Har 30 daqiqada bir marta yangilanadi.
 */
object CurrencyRateManager {

    data class CnyRate(
        val somText: String,
        val percentText: String,
        val isUp: Boolean
    )

    private val _cnyRate = MutableStateFlow<CnyRate?>(null)
    val cnyRate: StateFlow<CnyRate?> = _cnyRate

    private var lastLoadedAt: Long = 0L
    private const val REFRESH_INTERVAL_MS = 30 * 60 * 1000L

    suspend fun load(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && _cnyRate.value != null && now - lastLoadedAt < REFRESH_INTERVAL_MS) return
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
            val rate = obj.getString("Rate").toDouble()
            val diff = obj.getString("Diff").toDoubleOrNull() ?: 0.0
            val prev = rate - diff
            val percent = if (prev != 0.0) (diff / prev) * 100.0 else 0.0
            _cnyRate.value = CnyRate(
                somText = formatSom(rate),
                percentText = String.format("%.2f", abs(percent)),
                isUp = diff >= 0
            )
            lastLoadedAt = now
        } catch (_: Exception) {
            // eski qiymat saqlanib qoladi
        }
    }

    private fun formatSom(value: Double): String =
        value.toLong().toString().reversed().chunked(3).joinToString(" ").reversed()
}
