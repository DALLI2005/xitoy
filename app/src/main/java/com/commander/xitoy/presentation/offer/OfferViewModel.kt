package com.commander.xitoy.presentation.offer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.AuthApi
import com.commander.xitoy.data.remote.OfferResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

sealed class OfferState {
    object Loading : OfferState()
    data class Success(val offer: OfferResponse) : OfferState()
    data class Error(val message: String) : OfferState()
}

// Ro'yxatdan o'tish ekrani ham shu ViewModel orqali oferta versiyasini oldindan
// yuklab qo'yadi (submit paytida offer_version sifatida yuborish uchun) — shuning
// uchun bu Hilt-scoped singleton emas, biroq LoginScreen va OfferScreen bir xil
// backstack entry'ga bog'lansa umumiy instansiyani ishlatishi mumkin.
@HiltViewModel
class OfferViewModel @Inject constructor(
    private val authApi: AuthApi
) : ViewModel() {

    private val _state = MutableStateFlow<OfferState>(OfferState.Loading)
    val state: StateFlow<OfferState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = OfferState.Loading
            try {
                val offer = authApi.getOffer()
                _state.value = OfferState.Success(offer)
            } catch (e: IOException) {
                // Tarmoq yo'q / ulanib bo'lmadi (timeout, DNS va h.k.)
                _state.value = OfferState.Error("Internet aloqasi yo'q. Ulanishni tekshirib, qayta urining.")
            } catch (e: HttpException) {
                // Server javob berdi, lekin xato status bilan (4xx/5xx)
                _state.value = OfferState.Error("Hozircha mavjud emas, keyinroq urinib ko'ring.")
            } catch (e: Exception) {
                // Kutilmagan holat (masalan noto'g'ri formatdagi javob) — xom
                // exception matnini foydalanuvchiga hech qachon ko'rsatmaymiz.
                _state.value = OfferState.Error("Hozircha mavjud emas, keyinroq urinib ko'ring.")
            }
        }
    }

    fun currentVersion(): String? = (_state.value as? OfferState.Success)?.offer?.version
}
