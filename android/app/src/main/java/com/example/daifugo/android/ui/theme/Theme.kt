package com.example.daifugo.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Daifugo v0.4.4: 落ち着いたカジノテーブルを意識した共通カラーパレット。 */
val CasinoGreen = Color(0xFF0F6A48)
val CasinoGreenDark = Color(0xFF063827)
val CasinoGold = Color(0xFFB9892F)
val Felt = Color(0xFFF3EFE5)
val Ink = Color(0xFF191B18)
val SoftGreen = Color(0xFFE3F0E8)
val SoftGold = Color(0xFFF7EBC9)
val Danger = Color(0xFFB4232F)

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
    surface = Color(0xFFFFFEFA),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9E4D8),
    onSurfaceVariant = Color(0xFF5A574F),
    outline = Color(0xFFB7B0A2),
    error = Danger,
)

@Composable
fun DaifugoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DaifugoColors,
        content = content,
    )
}
