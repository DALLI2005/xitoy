package com.commander.xitoy.presentation.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Timer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

fun getRemainingSeconds(expiresAt: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Tashkent")
        val cleaned = expiresAt.trim().split("+")[0].split("Z")[0].take(19)
        val expireDate = sdf.parse(cleaned) ?: return 0L
        val diffMs = expireDate.time - System.currentTimeMillis()
        if (diffMs <= 0L) 0L else diffMs / 1000L
    } catch (e: Exception) {
        0L
    }
}

fun formatTimer(totalSeconds: Long): Triple<String, String, String> {
    val hours   = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return Triple(
        hours.toString().padStart(2, '0'),
        minutes.toString().padStart(2, '0'),
        seconds.toString().padStart(2, '0'),
    )
}

@Composable
fun DiscountTimer(
    remainingSeconds: Long,
    modifier: Modifier = Modifier,
) {
    val (hours, minutes, seconds) = formatTimer(remainingSeconds)

    val isUrgent = remainingSeconds < 300
    val chipColor = when {
        remainingSeconds < 300  -> Color(0xFFDC2626)
        remainingSeconds < 1800 -> Color(0xFFEA580C)
        else                    -> Color(0xFF1E293B)
    }

    // Pulse animatsiyasi faqat 5 daqiqa qolganda ishlaydi
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val chipScale = if (isUrgent) pulseScale else 1f

    Row(
        modifier = modifier
            .scale(chipScale)
            .background(chipColor.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            Lucide.Timer,
            contentDescription = null,
            tint = chipColor,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "$hours:$minutes:$seconds",
            color = chipColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
        )
        if (isUrgent) {
            Text(
                "· SHOSHILING!",
                color = chipColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// Faqat timer matni o'z ichiga oladi — ProductCard'ning butun tanasi
// qayta chizilmaydi, faqat shu kichik composable yangilanadi.
@Composable
fun CountdownText(discountExpires: String, modifier: Modifier = Modifier) {
    var remainingSeconds by remember(discountExpires) {
        mutableLongStateOf(getRemainingSeconds(discountExpires))
    }
    LaunchedEffect(discountExpires) {
        while (remainingSeconds > 0L) {
            delay(1000L)
            remainingSeconds = getRemainingSeconds(discountExpires)
        }
    }
    if (remainingSeconds > 0L) {
        DiscountTimer(remainingSeconds = remainingSeconds, modifier = modifier)
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier
        ) {
            Icon(Lucide.Timer, null, tint = Color.Gray, modifier = Modifier.size(13.dp))
            Text("Chegirma tugadi", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
