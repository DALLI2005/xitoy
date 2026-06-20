package com.commander.xitoy.presentation.payment

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.TriangleAlert
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSuccess
import com.commander.xitoy.ui.theme.DalliSuccessSoft
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText
import com.commander.xitoy.ui.theme.DalliTextSecondary

private fun groupSom(v: Long): String =
    v.toString().reversed().chunked(3).joinToString(" ").reversed()

@Composable
fun PaymentScreen(
    orderId:       String,
    totalSomm:     Long,
    telegramId:    String,
    onBack:        () -> Unit,
    onPaymentSent: () -> Unit
) {
    val viewModel: PaymentViewModel = hiltViewModel()
    val cardInfo   by viewModel.cardInfo.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val context    = LocalContext.current
    val clipboard  = LocalClipboardManager.current

    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedUri = uri }

    LaunchedEffect(Unit) { viewModel.loadCard() }

    LaunchedEffect(uploadState) {
        when (val s = uploadState) {
            is UploadState.Success -> onPaymentSent()
            is UploadState.Error   -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                viewModel.resetUpload()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DalliBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Orqaga tugmasi + sarlavha
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DalliSurface)
                        .border(1.dp, DalliLine, RoundedCornerShape(12.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.ChevronLeft, null, tint = DalliText, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "To'lov",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DalliText
                    )
                    Text(
                        text = orderId,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Karta kartochkasi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = DalliSurface),
                border = BorderStroke(1.dp, DalliLine)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quyidagi kartaga o'tkazing",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (cardInfo.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = DalliPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        // Karta raqami — katta va nusxalash tugmasi bilan
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DalliPrimarySoft)
                                .clickable {
                                    clipboard.setText(AnnotatedString(cardInfo.cardNumber.replace(" ", "")))
                                    Toast.makeText(context, "Karta raqami nusxalandi", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = cardInfo.cardNumber,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DalliPrimary,
                                    letterSpacing = 2.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Bosib nusxalash",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DalliPrimary.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = cardInfo.cardHolder,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DalliText
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DalliLine)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("O'tkazma summasi", fontSize = 13.sp, color = DalliMuted, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = groupSom(totalSomm),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DalliText
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("so'm", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DalliMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chek yuklash bo'limi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = DalliSurface),
                border = BorderStroke(1.dp, DalliLine)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "To'lov chekini yuklang",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DalliText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "O'tkazma skrinshoti yoki chek rasmini galereyadан tanlang",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedUri != null) {
                        // Tanlangan rasm preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, DalliLine, RoundedCornerShape(12.dp))
                                .clickable { imagePicker.launch("image/*") }
                        ) {
                            AsyncImage(
                                model = selectedUri,
                                contentDescription = "Chek rasmi",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("O'zgartirish", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        // Rasm tanlash tugmasi
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DalliPrimarySoft)
                                .border(1.dp, DalliPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Lucide.Send,
                                    contentDescription = null,
                                    tint = DalliPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Chek rasmini tanlash",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DalliPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Eslatma
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFFBEB))
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Lucide.TriangleAlert, null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "To'lovni amalga oshirgandan so'ng chekni yuboring. Admin tasdiqlashini kuting.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF92400E),
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Yuborish tugmasi
            Button(
                onClick = {
                    val uri = selectedUri ?: run {
                        Toast.makeText(context, "Iltimos, chek rasmini tanlang", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.uploadReceipt(orderId, telegramId, uri, context)
                },
                enabled = selectedUri != null && uploadState !is UploadState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DalliPrimary,
                    disabledContainerColor = DalliLine,
                    disabledContentColor = DalliMuted
                )
            ) {
                if (uploadState is UploadState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Lucide.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chekni yuborish", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}
