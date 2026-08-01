package com.commander.xitoy.ui.theme

import android.app.Activity
import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary            = DalliPrimary,
    primaryContainer   = DalliPrimarySoft,
    onPrimary          = DalliSurface,
    onPrimaryContainer = DalliPrimary,
    secondary          = DalliAccent,
    secondaryContainer = DalliAccentSoft,
    onSecondary        = DalliSurface,
    onSecondaryContainer = DalliAccent,
    background         = DalliBackground,
    surface            = DalliSurface,
    surfaceVariant     = DalliSurfaceAlt,
    onBackground       = DalliText,
    onSurface          = DalliText,
    onSurfaceVariant   = DalliTextSecondary,
    outline            = DalliLine,
    error              = DalliError,
    onError            = DalliSurface
)

@Composable
fun XitoyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // DIQQAT: dynamicColor'ni 'false' qilib qo'yamiz! Shunda telefon o'zining rangini tiqishtirmaydi.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Hozircha ilovamiz doim yorug' (Light) rejimda ishlashi uchun

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Zamonaviy usul: WindowCompat orqali status/nav bar rangini boshqarish
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        // Default shrift: explicit fontSize ishlatadigan Text'lar ham Plus Jakarta'ni olsin
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = PlusJakarta),
            content = content
        )
    }
}
