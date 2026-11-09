package com.maysk.cloner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.maysk.cloner.data.AppSettings
import com.maysk.cloner.data.ThemePreset
import com.maysk.design.Backdrop
import com.maysk.design.MayskColors
import com.maysk.design.MayskDesignTheme

val LocalUi = staticCompositionLocalOf { AppSettings() }

fun buildColors(s: AppSettings, systemDark: Boolean): MayskColors {
    val accent = Color(s.accent)
    val blue = Color(0xFF6D28D9)
    val cyan = Color(0xFF22D3EE)
    return when (s.theme) {
        ThemePreset.PURPLE -> MayskColors(
            isLight = false, bgTop = Color(0xFF1E0E4D), bgBottom = Color(0xFF0A0520), card = Color.White,
            glassAlpha = 0.09f, border = Color.White.copy(alpha = 0.10f), accent = accent, accent2 = blue, accent3 = cyan,
            text = Color.White, textDim = Color.White.copy(alpha = 0.66f), glow = true, corner = dp(s.corner),
            backdrop = Backdrop.AURORA, button = accent,
        )
        ThemePreset.AURORA -> MayskColors(
            false, Color(0xFF160C3A), Color(0xFF05030F), Color.White, s.cardAlpha, Color.White.copy(alpha = 0.10f),
            accent, blue, cyan, Color.White, Color.White.copy(alpha = 0.65f), true, dp(s.corner), Backdrop.AURORA_MOUNTAINS, accent,
        )
        ThemePreset.AMOLED -> MayskColors(
            false, Color.Black, Color.Black, Color.White, 0.06f, Color.White.copy(alpha = 0.07f),
            accent, blue, cyan, Color.White, Color.White.copy(alpha = 0.6f), false, dp(s.corner), Backdrop.SOLID, accent,
        )
        ThemePreset.LIGHT -> MayskColors(
            true, Color(0xFFF4F0FF), Color(0xFFEAE3FA), Color.White, 0.9f, Color(0x14000000),
            accent, blue, cyan, Color(0xFF17122E), Color(0xFF17122E).copy(alpha = 0.6f), false, dp(s.corner), Backdrop.SOLID, accent,
        )
    }
}

private fun dp(v: Int) = androidx.compose.ui.unit.Dp(v.toFloat())

@Composable
fun ClonerTheme(settings: AppSettings, colors: MayskColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalUi provides settings) {
        MayskDesignTheme(colors, settings.fontScale, content)
    }
}
