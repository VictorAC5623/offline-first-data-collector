package com.pre.capturadatos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// === ESQUEMAS DE COLOR ===
// (Lee los colores desde Color.kt)
private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = BackgroundWhite,
    secondary = BrandBlueLight,
    onSecondary = BackgroundWhite,
    background = BackgroundWhite,
    onBackground = TextBlack,
    surface = BackgroundWhite,
    onSurface = TextBlack,
    outline = NeutralGray,
    error = Color(0xFFB00020),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextWhite,
    surface = SurfaceDark,
    onSurface = TextWhite,
    outline = NeutralGray,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

// === TIPOGRAFÍA PERSONALIZADA ===
private val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

// === TEMA PRINCIPAL ===
@Composable
fun PreuCapturaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Forzamos 'false'
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // --- Controlar el color de la barra de estado ---
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // <-- CORREGIDO (Volvemos a la sintaxis de 'propiedad')
            // Esto dejará solo la advertencia de "obsoleto"
            // pero eliminará la advertencia de "estilo de Kotlin".
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()

            // Ajusta el color de los iconos (reloj, batería)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }
    // --- Fin del bloque de barra de estado ---

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}