package com.vinh.dyvat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = NearBlack,
    primaryContainer = SpotifyGreenBorder,
    onPrimaryContainer = TextWhite,
    secondary = TextSilver,
    onSecondary = NearBlack,
    secondaryContainer = MidDark,
    onSecondaryContainer = TextWhite,
    tertiary = AnnouncementBlue,
    onTertiary = NearBlack,
    tertiaryContainer = AnnouncementBlue,
    onTertiaryContainer = NearBlack,
    error = NegativeRed,
    onError = NearBlack,
    errorContainer = NegativeRed,
    onErrorContainer = NearBlack,
    background = NearBlack,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = MidDark,
    onSurfaceVariant = TextSilver,
    outline = BorderGray,
    outlineVariant = LightBorder,
    inverseSurface = TextWhite,
    inverseOnSurface = NearBlack,
    inversePrimary = SpotifyGreen,
    surfaceTint = SpotifyGreen
)

@Composable
fun DyvatTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
