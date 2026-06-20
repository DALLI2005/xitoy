package com.commander.xitoy.presentation.common

import androidx.compose.ui.graphics.Color
import com.commander.xitoy.ui.theme.DalliSuccess
import com.commander.xitoy.ui.theme.DalliSuccessSoft

data class OrderStage(val short: String, val full: String)

val ORDER_STAGES = listOf(
    OrderStage("Yangi",       "Buyurtma berildi"),
    OrderStage("Tasdiqlandi", "Tasdiqlandi"),
    OrderStage("Yo'lda",      "Yo'lda (transport)"),
    OrderStage("Yetkazildi",  "Yetkazib berildi")
)

fun holatToStage(holat: String): Int = when (holat) {
    "Tolov_kutilmoqda" -> 0
    "Tasdiqlandi"      -> 1
    "Yo'lda"           -> 2
    "Yetkazildi"       -> 3
    else               -> 0
}

fun holatDisplay(holat: String): String = when (holat) {
    "Tolov_kutilmoqda" -> "To'lov kutilmoqda"
    "Rad_etildi"       -> "Rad etildi"
    else               -> holat
}

fun holatColors(holat: String): Pair<Color, Color> = when (holat) {
    "Yangi"            -> Color(0xFFEEF2FF) to Color(0xFF1B40D4)
    "Tolov_kutilmoqda" -> Color(0xFFFEF9C3) to Color(0xFFCA8A04)
    "Tasdiqlandi"      -> Color(0xFFEDE9FE) to Color(0xFF7C3AED)
    "Yo'lda"           -> Color(0xFFFEF3C7) to Color(0xFFD97706)
    "Yetkazildi"       -> DalliSuccessSoft to DalliSuccess
    "Rad_etildi"       -> Color(0xFFFFE4E4) to Color(0xFFDC2626)
    else               -> Color(0xFFEEF2FF) to Color(0xFF1B40D4)
}
