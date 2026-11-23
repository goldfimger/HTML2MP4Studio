package com.html2mp4.studio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val StudioBackground = Color(0xFF0F172A)
val StudioSurface = Color(0xFF1E293B)
val StudioSurfaceVariant = Color(0xFF334155)
val StudioPrimary = Color(0xFF3B82F6)
val StudioPrimaryDark = Color(0xFF2563EB)
val StudioOnSurface = Color(0xFFF1F5F9)
val StudioMuted = Color(0xFF94A3B8)
val StudioBorder = Color(0xFF334155)
val StudioSuccess = Color(0xFF10B981)
val StudioDanger = Color(0xFFEF4444)

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioPrimary,
    onPrimary = Color.White,
    primaryContainer = StudioPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = StudioSurfaceVariant,
    onSecondary = StudioOnSurface,
    background = StudioBackground,
    onBackground = StudioOnSurface,
    surface = StudioSurface,
    onSurface = StudioOnSurface,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioMuted,
    outline = StudioBorder,
    error = StudioDanger,
    onError = Color.White
)

private val StudioTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = StudioOnSurface
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = StudioOnSurface
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = StudioOnSurface
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = StudioMuted
    )
)

@Composable
fun HTML2MP4StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = StudioTypography,
        content = content
    )
}
