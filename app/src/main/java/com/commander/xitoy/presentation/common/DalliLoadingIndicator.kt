package com.commander.xitoy.presentation.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.commander.xitoy.R
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DalliLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    logoTint: Color = Color.White,
    dotColor: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dalli_loading")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer { rotationZ = rotation }
        ) {
            val dotCount = 8
            val radius = this.size.minDimension / 2 * 0.82f
            val dotRadius = this.size.minDimension * 0.045f
            for (i in 0 until dotCount) {
                val angle = (360f / dotCount) * i
                val angleRad = Math.toRadians(angle.toDouble())
                val x = center.x + radius * cos(angleRad).toFloat()
                val y = center.y + radius * sin(angleRad).toFloat()
                val alpha = 0.15f + 0.85f * (i.toFloat() / dotCount)
                drawCircle(
                    color = dotColor.copy(alpha = alpha),
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_great_wall),
            contentDescription = "Dalli Shop",
            tint = logoTint,
            modifier = Modifier
                .size(size * 0.45f)
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                }
        )
    }
}
