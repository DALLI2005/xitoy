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
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import javax.inject.Inject

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val fullname: String = "",
    val phoneDigits: String = "",      // +998 dan keyingi 9 ta raqam
    val password: String = "",
    val confirmPassword: String = "",
    val offerAccepted: Boolean = false,
    val offerVersion: String = "",     // fon rejimida GET /api/offer dan oldindan yuklanadi
    val fullnameError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val offerError: String? = null,
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

    init {
        fetchOfferVersion()
    }

    private fun fetchOfferVersion() {
        viewModelScope.launch {
            try {
                val offer = authApi.getOffer()
                _uiState.value = _uiState.value.copy(offerVersion = offer.version)
            } catch (_: Exception) {
                // Ro'yxatdan o'tishda qayta uriniladi (submit paytida versiya bo'sh
                // bo'lsa xatolik ko'rsatiladi) — fon xatosini shu yerda ko'rsatmaymiz
            }
        }
    }

    fun switchMode(mode: AuthMode) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(
            mode = mode,
            fullnameError = null, phoneError = null,
            passwordError = null, confirmError = null,
            offerError = null, generalError = null
        )
    }

    fun onOfferAcceptedChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(offerAccepted = value, offerError = null, generalError = null)
        if (value && _uiState.value.offerVersion.isEmpty()) fetchOfferVersion()
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
        var offerError: String? = null

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
        if (s.mode == AuthMode.REGISTER && !s.offerAccepted) {
            offerError = "Ommaviy oferta shartlariga rozilik bildirilishi shart"
        }

        _uiState.value = s.copy(
            fullnameError = fullnameError,
            phoneError = phoneError,
            passwordError = passwordError,
            confirmError = confirmError,
            offerError = offerError
        )
        return fullnameError == null && phoneError == null &&
            passwordError == null && confirmError == null && offerError == null
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
                            password = s.password,
                            offerAccepted = s.offerAccepted,
                            offerVersion = s.offerVersion
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

    @Suppress("DEPRECATION")
    private fun registerFcmToken(userId: String) {
        if (userId.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                authApi.registerFcmToken(FcmTokenRequest(userId, token))
            } catch (_: Exception) {}
        }
    }
}
