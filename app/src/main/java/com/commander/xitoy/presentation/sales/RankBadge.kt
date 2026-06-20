package com.commander.xitoy.presentation.sales

import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.commander.xitoy.R
import kotlinx.coroutines.delay

@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val typeface = remember {
        val base = ResourcesCompat.getFont(context, R.font.playfair_display_italic_variable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && base != null) {
            Typeface.create(base, 900, true)
        } else {
            base ?: Typeface.DEFAULT_BOLD
        }
    }

    // Flutter dagi TweenSequence ni takrorlash:
    // 0.0 → 1.3 (360ms, easeOut) → 0.9 (120ms) → 1.0 (120ms)
    val scale = remember { Animatable(0f) }
    LaunchedEffect(rank) {
        delay(300L + rank * 100L)          // rank 1→400ms, 2→500ms, 3→600ms
        scale.animateTo(1.3f, tween(360, easing = EaseOut))
        scale.animateTo(0.9f, tween(120, easing = EaseInOut))
        scale.animateTo(1.0f, tween(120, easing = EaseInOut))
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    ) {
        val text = rank.toString()
        val fontSize = 72.dp.toPx()
        val x = 8.dp.toPx()
        val y = size.height * 0.88f

        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas

            // Qatlam 1: 3D jigarrang soya
            canvas.drawText(
                text, x + 6.dp.toPx(), y + 6.dp.toPx(),
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(255, 90, 45, 0)
                    textSize = fontSize
                    this.typeface = typeface
                    style = android.graphics.Paint.Style.FILL
                }
            )

            // Qatlam 2: Qalin oq outline
            canvas.drawText(
                text, x + 2.dp.toPx(), y + 2.dp.toPx(),
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textSize = fontSize
                    this.typeface = typeface
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 8.dp.toPx()
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }
            )

            // Qatlam 3: Sariq-orange gradient
            canvas.drawText(
                text, x + 2.dp.toPx(), y + 2.dp.toPx(),
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        x, 0f, x, size.height,
                        intArrayOf(
                            android.graphics.Color.parseColor("#FFE033"),
                            android.graphics.Color.parseColor("#FF8C00")
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    textSize = fontSize
                    this.typeface = typeface
                    style = android.graphics.Paint.Style.FILL
                }
            )
        }
    }
}
