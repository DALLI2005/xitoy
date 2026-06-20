package com.commander.xitoy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.commander.xitoy.R

// Plus Jakarta Sans — butun ilova shrifti (home.jsx dagidek)
val PlusJakarta = FontFamily(
    Font(R.font.plus_jakarta_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_extrabold, FontWeight.ExtraBold),
)

val Typography = Typography(
    // Sarlavhalar — qalin va ixcham (manfiy letterSpacing)
    titleLarge = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakarta,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    ),
)
