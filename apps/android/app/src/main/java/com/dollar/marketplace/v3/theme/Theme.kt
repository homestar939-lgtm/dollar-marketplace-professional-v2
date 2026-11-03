package com.dollar.marketplace.v3.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * سمات V3: نسخة داكنة (الافتراضية) ونسخة فاتحة.
 * حزمة v3 مستقلة تماماً عن `com.dollar.marketplace.Pal` في الإصدار القديم.
 */
private val V3DarkColors = darkColorScheme(
    primary = RoyalGold,
    onPrimary = RoyalBlack,
    primaryContainer = RoyalGoldLight,
    onPrimaryContainer = RoyalBlack,
    secondary = RoyalGoldLight,
    onSecondary = RoyalBlack,
    secondaryContainer = RoyalBlackLight,
    onSecondaryContainer = TextPrimary,
    background = RoyalBlack,
    onBackground = TextPrimary,
    surface = RoyalBlackLight,
    onSurface = TextPrimary,
    surfaceVariant = RoyalBlackLight,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF3A3A41),
    outlineVariant = Color(0xFF26262C),
    error = ErrorRed,
    onError = TextPrimary
)

private val V3LightColors = lightColorScheme(
    primary = RoyalGold,
    onPrimary = RoyalBlack,
    primaryContainer = RoyalGoldLight,
    onPrimaryContainer = RoyalBlack,
    secondary = RoyalGoldLight,
    onSecondary = RoyalBlack,
    background = Color(0xFFFDFBF5),
    onBackground = RoyalBlack,
    surface = Color(0xFFFFFFFF),
    onSurface = RoyalBlack,
    surfaceVariant = Color(0xFFF1EDE2),
    onSurfaceVariant = Color(0xFF5A554D),
    outline = Color(0xFFCFC7B6),
    error = ErrorRed,
    onError = TextPrimary
)

private val V3Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

/** الخطوط الافتراضية من material3 — لا نحتاج أي خط مخصّص. */
private val V3Typography = Typography()

@Composable
fun DollarMarketplaceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) V3DarkColors else V3LightColors,
        typography = V3Typography,
        shapes = V3Shapes,
        content = content
    )
}
