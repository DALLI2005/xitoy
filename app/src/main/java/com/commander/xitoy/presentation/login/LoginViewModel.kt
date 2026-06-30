package com.commander.xitoy.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.AuthApi
import com.commander.xitoy.data.remote.FcmTokenRequest
import com.commander.xitoy.domain.model.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState        // boshlang'ich — tugma ko'rinadi
    data object Starting : LoginUiState    // /auth/start chaqirilmoqda
    data object Waiting : LoginUiState     // Telegramda tasdiqlash kutilmoqda (polling)
    data object Success : LoginUiState     // tasdiqlandi — Home ga o'tiladi
    data object Timeout : LoginUiState     // 2 daqiqa o'tdi — tasdiqlanmadi
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    // Telegram havolasini ochish uchun bir martalik signal (null = ochiladigan narsa yo'q)
    private val _openUrl = MutableStateFlow<String?>(null)
    val openUrl: StateFlow<String?> = _openUrl

    private var pollingJob: Job? = null
    private var lastTelegramUrl: String? = null

    fun startLogin() {
        pollingJob?.cancel()
        _uiState.value = LoginUiState.Starting
        pollingJob = viewModelScope.launch {
            try {
                val start = authApi.authStart()
                lastTelegramUrl = start.telegramUrl
                _openUrl.value = start.telegramUrl       // ekran Telegramni ochadi
                _uiState.value = LoginUiState.Waiting
                pollConfirmation(start.loginToken)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Ulanishda xatolik. Internetni tekshiring.")
            }
        }
    }

    private suspend fun pollConfirmation(token: String) {
        val maxAttempts = 60   // 60 × 2s = 120s = 2 daqiqa
        repeat(maxAttempts) {
            delay(2000)
            try {
                val res = authApi.authCheck(token)
                if (res.status == "confirmed") {
                    val telegramId = res.telegramId ?: ""
                    SessionManager.save(
                        telegramId = telegramId,
                        ism = res.ism ?: "",
                        username = res.username ?: "",
                        fullname = res.fullname ?: "",
                        phone = res.phone ?: "",
                        address = res.address ?: ""
                    )
                    _uiState.value = LoginUiState.Success
                    registerFcmToken(telegramId)
                    return
                }
            } catch (e: Exception) {
                // tarmoq vaqtinchalik uzilishi — keyingi urinishda davom etadi
            }
        }
        // 2 daqiqa o'tdi — tasdiqlanmadi
        _uiState.value = LoginUiState.Timeout
    }

    // Ekran havolani ochgach chaqiriladi
    fun onUrlOpened() {
        _openUrl.value = null
    }

    // Kutish paytida "Qaytadan urinish" bosilganda — pollingni to'xtatib, boshiga qaytaradi
    fun cancelAndRetry() {
        pollingJob?.cancel()
        _uiState.value = LoginUiState.Idle
        _openUrl.value = null
    }

    // Timeout holatida oxirgi Telegram havolasini qayta ochish
    fun reopenTelegram() {
        _openUrl.value = lastTelegramUrl
    }

    fun reset() {
        pollingJob?.cancel()
        _uiState.value = LoginUiState.Idle
        _openUrl.value = null
    }

    private fun registerFcmToken(telegramId: String) {
        if (telegramId.isEmpty()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    authApi.registerFcmToken(FcmTokenRequest(telegramId, token))
                } catch (_: Exception) {}
            }
        }
    }
}
