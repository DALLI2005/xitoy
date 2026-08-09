package com.commander.xitoy.presentation.superadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.SuperadminApi
import com.commander.xitoy.data.remote.SuperadminPasswordChangeRequest
import com.commander.xitoy.domain.model.SuperadminSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class SuperadminChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class SuperadminChangePasswordViewModel @Inject constructor(
    private val api: SuperadminApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuperadminChangePasswordUiState())
    val uiState: StateFlow<SuperadminChangePasswordUiState> = _uiState.asStateFlow()

    fun onOldPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(oldPassword = value, error = null)
    }

    fun onNewPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value, error = null)
    }

    fun submit() {
        if (_uiState.value.isLoading) return
        val token = SuperadminSessionManager.tokenOrNull()
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "Seans tugagan, qayta kiring")
            return
        }
        val old = _uiState.value.oldPassword
        val new = _uiState.value.newPassword
        if (old.isBlank() || new.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Barcha maydonlarni to'ldiring")
            return
        }
        if (new.trim().length < 4) {
            _uiState.value = _uiState.value.copy(error = "Yangi parol kamida 4 belgidan iborat bo'lishi kerak")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                api.changePassword(token, SuperadminPasswordChangeRequest(old, new))
                _uiState.value = _uiState.value.copy(isLoading = false, success = true)
            } catch (e: HttpException) {
                val message = if (e.code() == 401) "Joriy parol noto'g'ri" else "Serverda xatolik yuz berdi"
                _uiState.value = _uiState.value.copy(isLoading = false, error = message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ulanishda xatolik. Internetni tekshiring."
                )
            }
        }
    }
}
