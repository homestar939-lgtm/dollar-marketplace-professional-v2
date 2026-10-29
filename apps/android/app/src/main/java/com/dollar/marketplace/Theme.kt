package com.dollar.marketplace

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dollar Marketplace — الهوية البصرية
 * أسود ملكي + ذهب، مع طبقات تدرّج تُستخدم في الأزرار العائمة والقوائم المنسدلة.
 */
object Pal {
    val Black = Color(0xFF09090B)
    val Onyx = Color(0xFF0D0D10)
    val Surface = Color(0xFF141416)
    val SurfaceAlt = Color(0xFF1C1C21)
    val SurfaceHigh = Color(0xFF24242A)
    val Stroke = Color(0xFF2B2B32)
    val Gold = Color(0xFFD4AF37)
    val GoldSoft = Color(0xFFEFD98B)
    val GoldDeep = Color(0xFF9C7B22)
    val Text = Color(0xFFF7F3E8)
    val Muted = Color(0xFFA9A39A)
    val Danger = Color(0xFFE4645C)
    val Success = Color(0xFF43C08A)
    val Info = Color(0xFF64A0DA)

    val goldBrush: Brush get() = Brush.linearGradient(listOf(GoldDeep, Gold, GoldSoft))
    val goldFade: Brush get() = Brush.verticalGradient(listOf(Gold.copy(alpha = 0.22f), Color.Transparent))
    val heroBrush: Brush get() = Brush.linearGradient(
        listOf(Color(0xFF231A08), Color(0xFF14141A), Color(0xFF0A0A0C))
    )
    val cardBrush: Brush get() = Brush.verticalGradient(listOf(SurfaceAlt, Surface))
    val dangerBrush: Brush get() = Brush.linearGradient(listOf(Color(0xFF7C2A25), Danger))
}

private val DollarColors = darkColorScheme(
    primary = Pal.Gold,
    onPrimary = Pal.Black,
    primaryContainer = Pal.GoldDeep,
    onPrimaryContainer = Pal.Text,
    secondary = Pal.GoldSoft,
    onSecondary = Pal.Black,
    background = Pal.Black,
    onBackground = Pal.Text,
    surface = Pal.Surface,
    onSurface = Pal.Text,
    surfaceVariant = Pal.SurfaceAlt,
    onSurfaceVariant = Pal.Muted,
    outline = Pal.Stroke,
    error = Pal.Danger,
    onError = Pal.Text
)

private val DollarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val DollarTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 27.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

@Composable
fun DollarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DollarColors,
        shapes = DollarShapes,
        typography = DollarTypography,
        content = content
    )
}

/** تنسيق سعر موحّد عبر التطبيق. */
fun money(value: Double, currency: String = "USD"): String {
    val rounded = if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.2f", value)
    return "$rounded $currency"
}
