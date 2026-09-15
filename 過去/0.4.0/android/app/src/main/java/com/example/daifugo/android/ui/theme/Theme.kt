package com.example.daifugo.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CasinoGreen = Color(0xFF176B45)
val CasinoGreenDark = Color(0xFF0E432B)
val CasinoGold = Color(0xFFC9982E)
val Felt = Color(0xFFF7F4EC)
val Ink = Color(0xFF172019)
val SoftGreen = Color(0xFFE8F3EC)
val SoftGold = Color(0xFFFFF3D4)
val Danger = Color(0xFFB3261E)

private val DaifugoColors = lightColorScheme(
    primary = CasinoGreen,
    onPrimary = Color.White,
    primaryContainer = SoftGreen,
    onPrimaryContainer = CasinoGreenDark,
    secondary = CasinoGold,
    onSecondary = Color.White,
    secondaryContainer = SoftGold,
    background = Felt,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    error = Danger,
)

@Composable
fun DaifugoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DaifugoColors,
        content = content,
    )
}
