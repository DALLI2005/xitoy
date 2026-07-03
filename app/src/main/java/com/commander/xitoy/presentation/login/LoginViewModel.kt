package com.commander.xitoy.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.AuthApi
import com.commander.xitoy.data.remote.FcmTokenRequest
import com.commander.xitoy.data.remote.LoginRequest
import com.commander.xitoy.data.remote.RegisterRequest
import com.commander.xitoy.domain.model.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val fullname: String = "",
    val phoneDigits: String = "",      // +998 dan keyingi 9 ta raqam
    val password: String = "",
    val confirmPassword: String = "",
    val fullnameError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun switchMode(mode: AuthMode) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(
            mode = mode,
            fullnameError = null, phoneError = null,
            passwordError = null, confirmError = null,
            generalError = null
        )
    }

    fun onFullnameChange(value: String) {
        _uiState.value = _uiState.value.copy(fullname = value, fullnameError = null, generalError = null)
    }

    fun onPhoneChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(9)
        _uiState.value = _uiState.value.copy(phoneDigits = digits, phoneError = null, generalError = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, generalError = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, confirmError = null, generalError = null)
    }

    private fun fullPhone() = "+998" + _uiState.value.phoneDigits

    private fun validate(): Boolean {
        val s = _uiState.value
        var fullnameError: String? = null
        var phoneError: String? = null
        var passwordError: String? = null
        var confirmError: String? = null

        if (s.mode == AuthMode.REGISTER && s.fullname.trim().length < 3) {
            fullnameError = "Ism kamida 3 belgidan iborat bo'lishi kerak"
        }
        if (s.phoneDigits.length != 9) {
            phoneError = "Telefon raqamni to'liq kiriting (9 ta raqam)"
        }
        if (s.password.length < 6) {
            passwordError = "Parol kamida 6 belgidan iborat bo'lishi kerak"
        }
        if (s.mode == AuthMode.REGISTER && s.confirmPassword != s.password) {
            confirmError = "Parollar mos kelmadi"
        }

        _uiState.value = s.copy(
            fullnameError = fullnameError,
            phoneError = phoneError,
            passwordError = passwordError,
            confirmError = confirmError
        )
        return fullnameError == null && phoneError == null &&
            passwordError == null && confirmError == null
    }

    fun submit() {
        if (_uiState.value.isLoading || !validate()) return
        _uiState.value = _uiState.value.copy(isLoading = true, generalError = null)

        viewModelScope.launch {
            try {
                val s = _uiState.value
                val response = if (s.mode == AuthMode.REGISTER) {
                    authApi.register(
                        RegisterRequest(
                            fullname = s.fullname.trim(),
                            phone = fullPhone(),
                            password = s.password
                        )
                    )
                } else {
                    authApi.loginPassword(
                        LoginRequest(phone = fullPhone(), password = s.password)
                    )
                }

                SessionManager.save(
                    telegramId = response.userId.toString(),
                    ism = response.fullname.split(" ").firstOrNull() ?: response.fullname,
                    username = "",
                    fullname = response.fullname,
                    phone = response.phone,
                    address = "",
                    token = response.token
                )
                registerFcmToken(response.userId.toString())
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: HttpException) {
                val message = when (e.code()) {
                    409 -> "Bu raqam allaqachon ro'yxatdan o'tgan"
                    404 -> "Bu raqam ro'yxatdan o'tmagan"
                    401 -> "Parol noto'g'ri"
                    400 -> "Ma'lumotlar noto'g'ri kiritildi, tekshirib qaytadan urining"
                    else -> "Serverda xatolik yuz berdi, keyinroq urinib ko'ring"
                }
                _uiState.value = _uiState.value.copy(isLoading = false, generalError = message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generalError = "Ulanishda xatolik. Internetni tekshiring."
                )
            }
        }
    }

    private fun registerFcmToken(userId: String) {
        if (userId.isEmpty()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    authApi.registerFcmToken(FcmTokenRequest(userId, token))
                } catch (_: Exception) {}
            }
        }
    }
}
