package com.commander.xitoy.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.commander.xitoy.R
import com.commander.xitoy.domain.model.OnboardingManager
import com.commander.xitoy.domain.model.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.7f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(20f) }
    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffsetY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(600)) }
        launch {
            logoScale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            delay(200)
            launch { textAlpha.animateTo(1f, tween(400)) }
            launch { textOffsetY.animateTo(0f, tween(400)) }
            delay(150)
            launch { taglineAlpha.animateTo(1f, tween(400)) }
            launch { taglineOffsetY.animateTo(0f, tween(400)) }
        }
    }

    LaunchedEffect(key1 = true) {
        delay(1200)

        val destination = when {
            SessionManager.isLoggedIn -> "main_screen"
            OnboardingManager.hasCompletedOnboardingOnce(context) -> "login"
            else -> "onboarding"
        }
        navController.navigate(destination) {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D1B69),
                        Color(0xFF4B1FDC),
                        Color(0xFF1A0F3D)
                    )
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_dalli_new),
                contentDescription = "Dalli Shop",
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color(0xFF00E5D1).copy(alpha = 0.3f),
                        spotColor = Color(0xFF00E5D1).copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(32.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Dalli Shop",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffsetY.value.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Xitoydan to'g'ridan-to'g'ri Rishtonga",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .offset(y = taglineOffsetY.value.dp)
            )
        }

        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(24.dp)
                .alpha(0.6f)
        )
    }
}
