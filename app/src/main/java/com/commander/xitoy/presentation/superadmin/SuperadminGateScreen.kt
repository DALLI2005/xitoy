package com.commander.xitoy.presentation.superadmin

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliError
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliText

// Push-bildirishnoma orqali kelganda, sessiya muddati tugagan bo'lsa ko'rsatiladigan
// to'liq ekranli parol so'rovi — muvaffaqiyatli kirishdan so'ng kutilayotgan
// buyurtma tafsiloti (yoki ro'yxat) sahifasiga o'tkaziladi.
@Composable
fun SuperadminGateScreen(
    onUnlocked: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: SuperadminLoginViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) onUnlocked()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DalliBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga", tint = DalliText)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DalliPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.Lock, null, tint = DalliPrimary, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text("Superadmin panel", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = DalliText)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Buyurtmani ko'rish uchun parolni kiriting",
                fontSize = 13.5.sp,
                color = DalliMuted
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = { Text("Parol") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                enabled = !state.isLoading,
                isError = state.error != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DalliPrimary,
                    unfocusedBorderColor = DalliLine,
                    focusedTextColor = DalliText,
                    unfocusedTextColor = DalliText,
                    cursorColor = DalliPrimary
                )
            )
            state.error?.let { errorText ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorText, fontSize = 12.5.sp, color = DalliError)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.submit() },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Kirish", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
