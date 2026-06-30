package com.commander.xitoy.presentation.login

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commander.xitoy.R
import com.commander.xitoy.presentation.common.DalliLoadingIndicator
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimaryDark
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Send

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val openUrl by viewModel.openUrl.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(openUrl) {
        val url = openUrl ?: return@LaunchedEffect
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Telegram ilovasini ochib bo'lmadi", Toast.LENGTH_LONG).show()
        }
        viewModel.onUrlOpened()
    }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DalliPrimarySoft, DalliBackground)
                )
            )
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo — soyali, gradient fon
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = DalliPrimary.copy(alpha = 0.25f),
                        spotColor = DalliPrimary.copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DalliPrimarySoft, Color.White)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_great_wall),
                    contentDescription = "Dalli Shop",
                    tint = DalliPrimary,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Xush kelibsiz",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DalliText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Dalli Shop'ga Telegram orqali bir bosishda kiring",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DalliMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            when (val state = uiState) {
                is LoginUiState.Starting, is LoginUiState.Waiting -> {
                    // Kutish kartochkasi
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = DalliPrimary.copy(alpha = 0.1f),
                                spotColor = DalliPrimary.copy(alpha = 0.1f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(DalliSurface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DalliLoadingIndicator(
                                size = 56.dp,
                                logoTint = DalliPrimary,
                                dotColor = DalliPrimary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Telegramda tasdiqlang...",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DalliText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Bot xabaridagi \"✅ Kirishni tasdiqlash\" tugmasini bosing",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = DalliMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    TextButton(onClick = { viewModel.cancelAndRetry() }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Lucide.RefreshCw, null, tint = DalliPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Qaytadan urinish",
                                fontSize = 14.sp,
                                color = DalliPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                is LoginUiState.Timeout -> {
                    TimeoutHelp(
                        onRetryFromStart = { viewModel.startLogin() },
                        onReopenTelegram = { viewModel.reopenTelegram() }
                    )
                }

                else -> {
                    if (state is LoginUiState.Error) {
                        Text(
                            text = state.message,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DalliMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    GradientLoginButton(
                        text = if (state is LoginUiState.Error) "Qaytadan urinib ko'rish"
                               else "Telegram orqali kirish",
                        icon = if (state !is LoginUiState.Error) ({
                            Icon(Lucide.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }) else null,
                        onClick = { viewModel.startLogin() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeoutHelp(
    onRetryFromStart: () -> Unit,
    onReopenTelegram: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = DalliMuted,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Vaqt tugadi",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DalliText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "2 daqiqa ichida tasdiqlanmadi. Quyidagilardan birini sinab ko'ring:",
            fontSize = 13.5.sp,
            color = DalliMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(20.dp))

        HelpOption(
            icon = Icons.Default.OpenInNew,
            title = "Telegram ochilmadimi?",
            description = "Qayta urinib, Telegram havolasini ochishga harakat qiling",
            buttonText = "Telegramni ochish",
            onClick = onReopenTelegram
        )

        Spacer(Modifier.height(10.dp))

        HelpOption(
            icon = Icons.Default.TouchApp,
            title = "Botda tugmani bosdingizmi?",
            description = "Telegram'da @dalli_login_robot ga o'tib, \"✅ Kirishni tasdiqlash\" tugmasini bosing",
            buttonText = null,
            onClick = {}
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onRetryFromStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary)
        ) {
            Text(
                "Qaytadan boshlash",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun HelpOption(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DalliSurface)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = DalliPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, color = DalliText, fontSize = 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(description, color = DalliMuted, fontSize = 13.sp, lineHeight = 18.sp)
            if (buttonText != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, DalliPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DalliPrimary)
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun GradientLoginButton(
    text: String,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = DalliPrimary.copy(alpha = 0.35f),
                spotColor = DalliPrimary.copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(DalliPrimary, DalliPrimaryDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.invoke()
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}
