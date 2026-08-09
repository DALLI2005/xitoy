package com.commander.xitoy.presentation.superadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.AdminFcmTokenRequest
import com.commander.xitoy.data.remote.SuperadminApi
import com.commander.xitoy.data.remote.SuperadminLoginRequest
import com.commander.xitoy.domain.model.SuperadminSessionManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import javax.inject.Inject

data class SuperadminLoginUiState(
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class SuperadminLoginViewModel @Inject constructor(
    private val api: SuperadminApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuperadminLoginUiState())
    val uiState: StateFlow<SuperadminLoginUiState> = _uiState.asStateFlow()

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    /** Dialog yopilganda/qayta ochilganda holatni tozalash uchun. */
    fun reset() {
        _uiState.value = SuperadminLoginUiState()
    }

    fun submit() {
        if (_uiState.value.isLoading) return
        val password = _uiState.value.password
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Parolni kiriting")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val response = api.login(SuperadminLoginRequest(password))
                SuperadminSessionManager.unlock(response.token)
                registerFcmToken(response.token)
                _uiState.value = _uiState.value.copy(isLoading = false, success = true)
            } catch (e: HttpException) {
                val message = if (e.code() == 401) "Parol noto'g'ri" else "Serverda xatolik yuz berdi"
                _uiState.value = _uiState.value.copy(isLoading = false, error = message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ulanishda xatolik. Internetni tekshiring."
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun registerFcmToken(adminToken: String) {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            api.registerFcmToken(adminToken, AdminFcmTokenRequest(fcmToken))
        } catch (_: Exception) {
            // Push ro'yxatdan o'tmasa ham kirish muvaffaqiyatli hisoblanadi —
            // shunchaki push kelmasligi mumkin, panel o'zi ishlayveradi.
        }
    }
}
