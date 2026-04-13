package com.epione.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary             = EpioneGold,
    onPrimary           = EpioneMarbleDark,
    primaryContainer    = EpionePrimaryContainer,
    onPrimaryContainer  = EpioneMarbleDark,
    secondary           = EpioneGoldVariant,
    onSecondary         = EpioneMarbleDark,
    background          = EpioneSurface,
    onBackground        = EpioneOnSurface,
    surface             = EpioneSurface,
    onSurface           = EpioneOnSurface,
    outline             = EpioneOutline,
    error               = EpioneError,
)

private val DarkColors = darkColorScheme(
    primary             = EpioneGoldVariant,
    onPrimary           = EpioneMarbleDark,
    primaryContainer    = EpioneMarbleDark,
    onPrimaryContainer  = EpioneGoldVariant,
    secondary           = EpioneGold,
    onSecondary         = EpioneMarbleDark,
    background          = EpioneMarbleDark,
    onBackground        = EpioneMarbleLight,
    surface             = EpioneMarbleDark,
    onSurface           = EpioneMarbleLight,
    outline             = EpioneOutline,
    error               = EpioneError,
)

@Composable
fun EpioneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = EpioneTypography,
        content     = content,
    )
}
