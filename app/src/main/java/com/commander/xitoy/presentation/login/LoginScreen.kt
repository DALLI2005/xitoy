package com.commander.xitoy.presentation.login

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commander.xitoy.R
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliText
import androidx.compose.ui.text.font.FontWeight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.TimerOff

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val openUrl by viewModel.openUrl.collectAsState()
    val context = LocalContext.current

    // Telegram havolasini ochish (Intent)
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

    // Tasdiqlangach Home ga o'tish
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DalliBackground)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(DalliPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_great_wall),
                    contentDescription = "Dalli Shop",
                    tint = DalliPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
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
                    CircularProgressIndicator(color = DalliPrimary, strokeWidth = 3.dp)
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

                    // Ekran hech qachon qotib qolmasligi uchun — doim ko'rinadigan chiqish yo'li
                    Spacer(Modifier.height(24.dp))
                    TextButton(
                        onClick = { viewModel.cancelAndRetry() }
                    ) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Lucide.TimerOff, null, tint = DalliText, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Vaqt tugadi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DalliText,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tasdiqlash kelmadi. Qaytadan urinib ko'ring.",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = DalliMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.startLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Lucide.RefreshCw, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Qaytadan urinish",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
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
                    Button(
                        onClick = { viewModel.startLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state !is LoginUiState.Error) {
                                Icon(Lucide.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = if (state is LoginUiState.Error) "Qaytadan urinib ko'rish"
                                else "Telegram orqali kirish",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
