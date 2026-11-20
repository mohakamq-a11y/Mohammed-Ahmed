package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ArcticNavy,
    onPrimary = PureFrost,
    primaryContainer = GlacierTint,
    onPrimaryContainer = ArcticNavy,
    secondary = CryoCyan,
    onSecondary = PureFrost,
    secondaryContainer = ColorTokens.CyanContainerLight,
    onSecondaryContainer = ArcticNavy,
    tertiary = CopperAmber,
    onTertiary = PureFrost,
    tertiaryContainer = CopperAmberContainer,
    onTertiaryContainer = SlateInk,
    background = GlacierSurface,
    onBackground = SlateInk,
    surface = PureFrost,
    onSurface = SlateInk,
    surfaceVariant = ColorTokens.SurfaceVariantLight,
    onSurfaceVariant = SlateMuted,
    outline = MistBorder,
    outlineVariant = ColorTokens.OutlineVariantLight,
    error = CrimsonAlert,
    onError = PureFrost,
    errorContainer = CrimsonAlertContainer,
    onErrorContainer = CrimsonAlert
)

private val DarkColorScheme = darkColorScheme(
    primary = CryoCyanBright,
    onPrimary = ArcticNavy,
    primaryContainer = ArcticNavyLight,
    onPrimaryContainer = IceWhite,
    secondary = CryoCyan,
    onSecondary = PureFrost,
    secondaryContainer = DeepNightElevated,
    onSecondaryContainer = CryoCyanBright,
    tertiary = CopperAmber,
    onTertiary = PureFrost,
    tertiaryContainer = ColorTokens.AmberContainerDark,
    onTertiaryContainer = CopperAmberContainer,
    background = DeepNightSurface,
    onBackground = IceWhite,
    surface = DeepNightContainer,
    onSurface = IceWhite,
    surfaceVariant = DeepNightElevated,
    onSurfaceVariant = FrostSlate,
    outline = ColorTokens.OutlineDark,
    outlineVariant = DeepNightElevated,
    error = ColorTokens.ErrorDark,
    onError = DeepNightSurface,
    errorContainer = ColorTokens.ErrorContainerDark,
    onErrorContainer = CrimsonAlertContainer
)

private object ColorTokens {
    val CyanContainerLight = androidx.compose.ui.graphics.Color(0xFFD6EEFF)
    val SurfaceVariantLight = androidx.compose.ui.graphics.Color(0xFFEAF1F8)
    val OutlineVariantLight = androidx.compose.ui.graphics.Color(0xFFE2ECF4)
    val AmberContainerDark = androidx.compose.ui.graphics.Color(0xFF452806)
    val OutlineDark = androidx.compose.ui.graphics.Color(0xFF28466E)
    val ErrorDark = androidx.compose.ui.graphics.Color(0xFFFCA5A5)
    val ErrorContainerDark = androidx.compose.ui.graphics.Color(0xFF5C1515)
}

val NationalAcShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NationalAcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NationalAcTypography,
        shapes = NationalAcShapes,
        content = content
    )
}
