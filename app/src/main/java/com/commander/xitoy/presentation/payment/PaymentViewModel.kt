package com.commander.xitoy.presentation.payment

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.OrderApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val orderApi: OrderApi
) : ViewModel() {

    data class CardInfo(
        val cardNumber: String = "",
        val cardHolder: String = "",
        val isLoading: Boolean = true
    )

    private val _cardInfo = MutableStateFlow(CardInfo())
    val cardInfo: StateFlow<CardInfo> = _cardInfo

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    fun loadCard() {
        viewModelScope.launch {
            try {
                val info = orderApi.getPaymentCard()
                _cardInfo.value = CardInfo(
                    cardNumber = info.card_number,
                    cardHolder = info.card_holder,
                    isLoading  = false
                )
            } catch (e: Exception) {
                _cardInfo.value = CardInfo(isLoading = false)
            }
        }
    }

    fun uploadReceipt(orderId: String, telegramId: String, uri: Uri, context: Context) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw Exception("Rasmni o'qib bo'lmadi")
                val body = bytes.toRequestBody("image/*".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "receipt.jpg", body)
                orderApi.uploadReceipt(orderId, telegramId, part)
                _uploadState.value = UploadState.Success
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Xatolik yuz berdi")
            }
        }
    }

    fun resetUpload() {
        _uploadState.value = UploadState.Idle
    }
}
