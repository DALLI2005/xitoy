package com.commander.xitoy.presentation.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.commander.xitoy.ui.theme.DalliPrimary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val animationAsset: String,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        animationAsset = "onboarding/rishton.json",
        title = "Xitoydan to'g'ridan-to'g'ri Rishtonga",
        description = "Faqat rishtonliklar uchun — eng arzon narxlarda, vositachisiz"
    ),
    OnboardingPage(
        animationAsset = "onboarding/woman-shopping-online.json",
        title = "Minglab mahsulot, bitta ilova",
        description = "Kiyim-kechakdan elektronikagacha — kerakli narsangizni osongina toping"
    ),
    OnboardingPage(
        animationAsset = "onboarding/tracking-order-online.json",
        title = "Har bir qadamni kuzating",
        description = "Buyurtma berildi → tasdiqlandi → yo'lda → yetkazildi — barchasi real vaqtda ko'rinadi"
    ),
    OnboardingPage(
        animationAsset = "onboarding/delivery-service.json",
        title = "Bir bosishda buyurtma",
        description = "Rang va o'lchamni tanlang, savatga qo'shing, ilova ichida xavfsiz to'lov qiling"
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    // Barcha 4 ta animatsiyani parallel oldindan yuklash
    val compositions = onboardingPages.map { page ->
        rememberLottieComposition(LottieCompositionSpec.Asset(page.animationAsset))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Skip tugmasi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { onFinish() }) {
                Text("O'tkazib yuborish", color = Color.Gray, fontSize = 14.sp)
            }
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val fraction = 1f - pageOffset.coerceIn(0f, 1f)
            val pageScale = 0.85f + (1f - 0.85f) * fraction
            val pageAlpha = 0.4f + (1f - 0.4f) * fraction

            val pageData = onboardingPages[page]
            val composition by compositions[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .scale(pageScale)
                    .alpha(pageAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (composition != null) {
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(280.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            color = DalliPrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    text = pageData.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1A1A2E)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = pageData.description,
                    fontSize = 15.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        // Nuqta indikatorlar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(onboardingPages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (isSelected) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) DalliPrimary else Color(0xFFD0D0D0))
                )
            }
        }

        // Keyingisi / Boshlash tugmasi
        Button(
            onClick = {
                if (pagerState.currentPage < onboardingPages.size - 1) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (pagerState.currentPage < onboardingPages.size - 1) "Keyingisi" else "Boshlash",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}
