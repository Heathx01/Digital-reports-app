package com.example.digital_reports_app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// =============================================================================
// Material 3 Color Schemes
// =============================================================================

val DocuFieldLightColorScheme = lightColorScheme(
    primary = OrangePrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = OrangeContainerLight,
    onPrimaryContainer = OnOrangeContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = OnSecondaryLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = Color.White
)

val DocuFieldDarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = OnBlueContainerDark,
    secondary = CyanSecondaryDark,
    onSecondary = OnSecondaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = Color(0xFF050B14)
)

// =============================================================================
// Liquid Glass Modifier (Nativo para Jetpack Compose)
// =============================================================================

/**
 * Aplica el efecto visual Liquid Glass / Vidrio Esmerilado Industrial a cualquier
 * componente Composable en Android:
 * - Recorte suave según el radio especificado.
 * - Fondo translúcido con tonalidad calibrada según modo claro u oscuro.
 * - Bisel especular perimetral suave con gradiente vertical.
 */
fun Modifier.liquidGlass(
    isDarkTheme: Boolean,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    prominent: Boolean = false,
    backgroundBlurRadius: Dp = 0.dp
): Modifier {
    val glassTokens = if (isDarkTheme) DarkLiquidGlassColors else LightLiquidGlassColors
    val surfaceColor = if (prominent) glassTokens.surfaceProminentColor else glassTokens.surfaceColor
    val shape = RoundedCornerShape(cornerRadius)

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            glassTokens.borderSpecularTop,
            glassTokens.borderSubtleBottom
        )
    )

    var modifier = this.clip(shape)

    if (backgroundBlurRadius > 0.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        modifier = modifier.blur(backgroundBlurRadius)
    }

    return modifier
        .background(color = surfaceColor, shape = shape)
        .border(width = borderWidth, brush = borderBrush, shape = shape)
}

/**
 * Sobrecarga que detecta automáticamente si el tema actual de la APP está en modo oscuro
 * utilizando la composición de tokens de DocuFieldTheme.
 */
@Composable
fun Modifier.liquidGlass(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    prominent: Boolean = false,
    backgroundBlurRadius: Dp = 0.dp
): Modifier {
    val isDark = LocalLiquidGlassColors.current == DarkLiquidGlassColors
    return this.liquidGlass(
        isDarkTheme = isDark,
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        prominent = prominent,
        backgroundBlurRadius = backgroundBlurRadius
    )
}

// =============================================================================
// Tema Principal: DocuFieldTheme
// =============================================================================

@Composable
fun DocuFieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DocuFieldDarkColorScheme
        else -> DocuFieldLightColorScheme
    }

    val liquidGlassColors = if (darkTheme) DarkLiquidGlassColors else LightLiquidGlassColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                windowInsetsController.isAppearanceLightStatusBars = !darkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalLiquidGlassColors provides liquidGlassColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DocuFieldTypography,
            content = content
        )
    }
}

@Composable
fun DigitalreportsappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    DocuFieldTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

// Extensión para acceso rápido a tokens Liquid Glass en Composables
object DocuFieldThemeTokens {
    val liquidGlass: LiquidGlassColors
        @Composable
        get() = LocalLiquidGlassColors.current
}
