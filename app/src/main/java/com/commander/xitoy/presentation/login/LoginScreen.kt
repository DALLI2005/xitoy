package com.commander.xitoy.presentation.login

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commander.xitoy.R
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliError
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimaryDark
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Phone
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.User

private const val ADMIN_TELEGRAM_USERNAME = "dalli_shop_admin"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showForgotDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoginSuccess()
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = DalliSurface,
            title = {
                Text(
                    "Parolni tiklash",
                    fontWeight = FontWeight.ExtraBold,
                    color = DalliText
                )
            },
            text = {
                Text(
                    "Parolni tiklash uchun admin bilan bog'laning.",
                    color = DalliMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showForgotDialog = false
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://t.me/$ADMIN_TELEGRAM_USERNAME")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Telegram ilovasini ochib bo'lmadi", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Lucide.Send, null, tint = DalliPrimary, modifier = Modifier.size(16.dp))
                        Text("Telegram orqali yozish", color = DalliPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Yopish", color = DalliMuted)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DalliPrimarySoft, DalliBackground)
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.Center
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
                Image(
                    painter = painterResource(id = R.drawable.logo_dalli_new),
                    contentDescription = "Dalli Shop",
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Xush kelibsiz",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DalliText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (uiState.mode == AuthMode.LOGIN)
                    "Hisobingizga kiring"
                else
                    "Yangi hisob yarating",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DalliMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            ModeTabs(
                mode = uiState.mode,
                enabled = !uiState.isLoading,
                onModeChange = { viewModel.switchMode(it) }
            )

            Spacer(Modifier.height(16.dp))

            // Forma kartochkasi
            Column(
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
                    .padding(20.dp)
            ) {
                if (uiState.mode == AuthMode.REGISTER) {
                    AuthTextField(
                        value = uiState.fullname,
                        onValueChange = viewModel::onFullnameChange,
                        label = "Ism familiya",
                        leadingIcon = { Icon(Lucide.User, null, tint = DalliMuted, modifier = Modifier.size(20.dp)) },
                        error = uiState.fullnameError,
                        enabled = !uiState.isLoading
                    )
                    Spacer(Modifier.height(14.dp))
                }

                AuthTextField(
                    value = uiState.phoneDigits,
                    onValueChange = viewModel::onPhoneChange,
                    label = "Telefon raqam",
                    leadingIcon = { Icon(Lucide.Phone, null, tint = DalliMuted, modifier = Modifier.size(20.dp)) },
                    prefix = {
                        Text("+998 ", color = DalliText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    },
                    keyboardType = KeyboardType.Number,
                    error = uiState.phoneError,
                    enabled = !uiState.isLoading
                )

                Spacer(Modifier.height(14.dp))

                PasswordField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = if (uiState.mode == AuthMode.REGISTER) "Parol (kamida 6 belgi)" else "Parol",
                    error = uiState.passwordError,
                    enabled = !uiState.isLoading
                )

                if (uiState.mode == AuthMode.REGISTER) {
                    Spacer(Modifier.height(14.dp))
                    PasswordField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label = "Parolni tasdiqlang",
                        error = uiState.confirmError,
                        enabled = !uiState.isLoading
                    )
                }

                if (uiState.generalError != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = uiState.generalError!!,
                        color = DalliError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(20.dp))

                GradientSubmitButton(
                    text = if (uiState.mode == AuthMode.LOGIN) "Kirish" else "Ro'yxatdan o'tish",
                    isLoading = uiState.isLoading,
                    onClick = { viewModel.submit() }
                )

                if (uiState.mode == AuthMode.LOGIN) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showForgotDialog = true },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "Parolni unutdingizmi?",
                            color = DalliPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTabs(
    mode: AuthMode,
    enabled: Boolean,
    onModeChange: (AuthMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DalliSurface.copy(alpha = 0.7f))
            .padding(5.dp)
    ) {
        ModeTab("Kirish", mode == AuthMode.LOGIN, enabled, Modifier.weight(1f)) {
            onModeChange(AuthMode.LOGIN)
        }
        ModeTab("Ro'yxatdan o'tish", mode == AuthMode.REGISTER, enabled, Modifier.weight(1f)) {
            onModeChange(AuthMode.REGISTER)
        }
    }
}

@Composable
private fun ModeTab(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) DalliPrimary else Color.Transparent,
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else DalliMuted,
        label = "tabText"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 13.5.sp) },
            leadingIcon = leadingIcon,
            prefix = prefix,
            trailingIcon = trailingIcon,
            singleLine = true,
            enabled = enabled,
            isError = error != null,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DalliPrimary,
                unfocusedBorderColor = DalliLine,
                errorBorderColor = DalliError,
                focusedLabelColor = DalliPrimary,
                unfocusedLabelColor = DalliMuted,
                errorLabelColor = DalliError,
                cursorColor = DalliPrimary,
                focusedTextColor = DalliText,
                unfocusedTextColor = DalliText
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(
                text = error,
                color = DalliError,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    enabled: Boolean = true
) {
    var visible by remember { mutableStateOf(false) }
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        leadingIcon = { Icon(Lucide.Lock, null, tint = DalliMuted, modifier = Modifier.size(20.dp)) },
        keyboardType = KeyboardType.Password,
        error = error,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Lucide.EyeOff else Lucide.Eye,
                    contentDescription = if (visible) "Parolni yashirish" else "Parolni ko'rsatish",
                    tint = DalliMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

@Composable
private fun GradientSubmitButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = DalliPrimary.copy(alpha = 0.35f),
                spotColor = DalliPrimary.copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
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
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
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
