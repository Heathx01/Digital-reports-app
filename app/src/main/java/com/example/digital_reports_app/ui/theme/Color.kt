package com.example.digital_reports_app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// DocuField Enterprise - Definición de Paleta de Colores
// =============================================================================

// Modo Claro (Diurno / Terreno / Alto Brillo)
val OrangePrimaryLight = Color(0xFFEA580C)
val OnPrimaryLight = Color(0xFFFFFFFF)
val OrangeContainerLight = Color(0xFFFFEDD5)
val OnOrangeContainerLight = Color(0xFF9A3412)
val SlateSecondaryLight = Color(0xFF0F172A)
val OnSecondaryLight = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF4F6FB)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFFCBD5E1)
val OutlineVariantLight = Color(0xFFE2E8F0)
val SuccessLight = Color(0xFF16A34A)
val WarningLight = Color(0xFFD97706)
val ErrorLight = Color(0xFFDC2626)

// Modo Oscuro (Titanio Pizarra / Nocturno / Socavón)
val BluePrimaryDark = Color(0xFF3B82F6)
val OnPrimaryDark = Color(0xFF050B14)
val BlueContainerDark = Color(0xFF1E3A8A)
val OnBlueContainerDark = Color(0xFFDBEAFE)
val CyanSecondaryDark = Color(0xFF06B6D4)
val OnSecondaryDark = Color(0xFF021721)
val BackgroundDark = Color(0xFF090D16)
val OnBackgroundDark = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF0F131C)
val OnSurfaceDark = Color(0xFFF1F5F9)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF1E293B)
val OutlineVariantDark = Color(0xFF334155)
val SuccessDark = Color(0xFF22C55E)
val WarningDark = Color(0xFFF59E0B)
val ErrorDark = Color(0xFFEF4444)

// Status Colors
val StatusSuccessGreen = SuccessLight
val StatusWarningAmber = WarningLight

// =============================================================================
// Tokens Específicos para el Efecto Liquid Glass Industrial
// =============================================================================

@Immutable
data class LiquidGlassColors(
    val surfaceColor: Color,
    val surfaceProminentColor: Color,
    val borderSpecularTop: Color,
    val borderSubtleBottom: Color,
    val innerGlow: Color,
    val accentTint: Color,
    val telemetryAccent: Color,
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusCritical: Color,
)

val LightLiquidGlassColors = LiquidGlassColors(
    surfaceColor = Color(0xD1FFFFFF),          // 82% Blanco translúcido
    surfaceProminentColor = Color(0xEBFFFFFF), // 92% Blanco translúcido
    borderSpecularTop = Color(0xE6FFFFFF),     // 90% Bisel reflectante superior limpio
    borderSubtleBottom = Color(0x40CBD5E1),    // 25% Contorno inferior
    innerGlow = Color(0xF2FFFFFF),             // Resplandor superior
    accentTint = Color(0xFFEA580C),            // Naranja industrial
    telemetryAccent = Color(0xFF0F172A),       // Pizarra oscuro
    statusSuccess = SuccessLight,
    statusWarning = WarningLight,
    statusCritical = ErrorLight,
)

val DarkLiquidGlassColors = LiquidGlassColors(
    surfaceColor = Color(0xC20F172A),          // 76% Carbón pizarra translúcido
    surfaceProminentColor = Color(0xD61E293B), // 84% Titanio translúcido
    borderSpecularTop = Color(0x663B82F6),     // 40% Bisel azul eléctrico
    borderSubtleBottom = Color(0x261E293B),    // 15% Bisel inferior ahumado
    innerGlow = Color(0x263B82F6),             // Aura perimetral azul tenue
    accentTint = Color(0xFF3B82F6),            // Azul cobalto
    telemetryAccent = Color(0xFF06B6D4),       // Cian fosforescente
    statusSuccess = SuccessDark,
    statusWarning = WarningDark,
    statusCritical = ErrorDark,
)

val LocalLiquidGlassColors = staticCompositionLocalOf { LightLiquidGlassColors }
