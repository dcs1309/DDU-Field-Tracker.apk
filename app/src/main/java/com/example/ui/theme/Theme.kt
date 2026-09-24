package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = DduPrimaryDark,
    onPrimary = DduOnPrimaryDark,
    primaryContainer = DduPrimaryContainerDark,
    onPrimaryContainer = DduOnPrimaryContainerDark,
    secondary = DduSecondaryDark,
    onSecondary = DduOnSecondaryDark,
    secondaryContainer = DduSecondaryContainerDark,
    onSecondaryContainer = DduOnSecondaryContainerDark,
    tertiary = DduTertiaryDark,
    onTertiary = DduOnTertiaryDark,
    tertiaryContainer = DduTertiaryContainerDark,
    onTertiaryContainer = DduOnTertiaryContainerDark,
    background = DduBackgroundDark,
    onBackground = DduOnBackgroundDark,
    surface = DduSurfaceDark,
    onSurface = DduOnSurfaceDark,
    surfaceVariant = DduSurfaceVariantDark,
    onSurfaceVariant = DduOnSurfaceVariantDark,
    outline = DduOutlineDark,
    outlineVariant = DduOutlineVariantDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DduPrimary,
    onPrimary = DduOnPrimary,
    primaryContainer = DduPrimaryContainer,
    onPrimaryContainer = DduOnPrimaryContainer,
    secondary = DduSecondary,
    onSecondary = DduOnSecondary,
    secondaryContainer = DduSecondaryContainer,
    onSecondaryContainer = DduOnSecondaryContainer,
    tertiary = DduTertiary,
    onTertiary = DduOnTertiary,
    tertiaryContainer = DduTertiaryContainer,
    onTertiaryContainer = DduOnTertiaryContainer,
    background = DduBackground,
    onBackground = DduOnBackground,
    surface = DduSurface,
    onSurface = DduOnSurface,
    surfaceVariant = DduSurfaceVariant,
    onSurfaceVariant = DduOnSurfaceVariant,
    outline = DduOutline,
    outlineVariant = DduOutlineVariant,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep branded DDU theme consistent
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
