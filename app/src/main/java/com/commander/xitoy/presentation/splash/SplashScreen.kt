package com.commander.xitoy.presentation.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.commander.xitoy.domain.model.OnboardingManager
import com.commander.xitoy.domain.model.SessionManager
import com.commander.xitoy.presentation.common.DalliLoadingIndicator
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alphaAnim"
    )

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1000),
        label = "scaleAnim"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
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
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            DalliLoadingIndicator(
                size = 140.dp,
                logoTint = Color.White,
                dotColor = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "DALLI SHOP",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
        }
    }
}