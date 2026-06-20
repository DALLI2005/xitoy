package com.commander.xitoy.presentation.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.commander.xitoy.ui.theme.DalliLine2
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliSuccess

@Composable
fun OrderProgressBar(stage: Int, modifier: Modifier = Modifier) {
    val done = stage == ORDER_STAGES.size - 1
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        ORDER_STAGES.indices.forEach { i ->
            val active = i <= stage
            val fillColor = when {
                active && done -> DalliSuccess
                active         -> DalliPrimary
                else           -> DalliLine2
            }
            val fill by animateFloatAsState(
                targetValue = if (active) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 500,
                    delayMillis    = i * 130,
                    easing         = FastOutSlowInEasing
                ),
                label = "fill_$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DalliLine2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(fillColor)
                )
            }
        }
    }
}
