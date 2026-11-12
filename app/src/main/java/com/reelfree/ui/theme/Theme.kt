package com.reelfree.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E0FF),
    onPrimaryContainer = Color(0xFF1A0B6B),
    secondary = Color(0xFF5E5A71),
    secondaryContainer = Color(0xFFE4DFF9),
    onSecondaryContainer = Color(0xFF1B192C),
    tertiary = Color(0xFF14855D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC9F2DD),
    onTertiaryContainer = Color(0xFF00210F),
    error = Color(0xFFC4314B),
    onError = Color.White,
    errorContainer = Color(0xFFFFDADD),
    onErrorContainer = Color(0xFF40000D),
    background = Color(0xFFF7F6FB),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFF7F6FB),
    onSurface = Color(0xFF1B1B21),
    onSurfaceVariant = Color(0xFF5F5D6B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEFEDF6),
    surfaceContainerHighest = Color(0xFFE7E5F0),
    outline = Color(0xFF8E8C99),
    outlineVariant = Color(0xFFE2E0EA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC4BBFF),
    onPrimary = Color(0xFF2B1A8F),
    primaryContainer = Color(0xFF4332B8),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFFC8C3DC),
    secondaryContainer = Color(0xFF3A3650),
    onSecondaryContainer = Color(0xFFE4DFF9),
    tertiary = Color(0xFF6FDBA8),
    onTertiary = Color(0xFF003920),
    tertiaryContainer = Color(0xFF0B5236),
    onTertiaryContainer = Color(0xFFC9F2DD),
    error = Color(0xFFFFB3BC),
    onError = Color(0xFF67001E),
    errorContainer = Color(0xFF8F1530),
    onErrorContainer = Color(0xFFFFDADD),
    background = Color(0xFF111116),
    onBackground = Color(0xFFE5E3EC),
    surface = Color(0xFF111116),
    onSurface = Color(0xFFE5E3EC),
    onSurfaceVariant = Color(0xFFA9A6B6),
    surfaceContainerLowest = Color(0xFF0C0C10),
    surfaceContainerLow = Color(0xFF17171D),
    surfaceContainer = Color(0xFF1C1C23),
    surfaceContainerHigh = Color(0xFF25252D),
    surfaceContainerHighest = Color(0xFF303039),
    outline = Color(0xFF77748A),
    outlineVariant = Color(0xFF2E2D37),
)

/** Status colours that stay meaningful regardless of the base palette. */
@Immutable
data class StatusColors(val good: Color, val warn: Color, val bad: Color, val idle: Color)

private val LightStatus = StatusColors(Color(0xFF14855D), Color(0xFFB86E00), Color(0xFFC4314B), Color(0xFF8E8C99))
private val DarkStatus = StatusColors(Color(0xFF6FDBA8), Color(0xFFFFC26B), Color(0xFFFF8A9C), Color(0xFF8F8CA0))

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun ReelFreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Off by default: wallpaper-derived colours made status colours hard to read. */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors: ColorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus,
    ) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
