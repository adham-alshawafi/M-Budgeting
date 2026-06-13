package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = NetYellow,
  onPrimary = Color.Black,
  primaryContainer = DarkSurface,
  onPrimaryContainer = TextPrimary,
  secondary = IncomeGreen,
  tertiary = ExpenseRed,
  background = DarkBg,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurface,
  onSurfaceVariant = TextSecondary,
  outline = SurfaceBorder
)

private val LightColorScheme = lightColorScheme(
  primary = NetYellow,
  onPrimary = Color.Black,
  primaryContainer = Color(0xFFF0F2F6),
  onPrimaryContainer = Color(0xFF0C0E14),
  secondary = IncomeGreen,
  tertiary = ExpenseRed,
  background = Color(0xFFF9FAFC),
  onBackground = Color(0xFF0C0E14),
  surface = Color.White,
  onSurface = Color(0xFF0C0E14),
  surfaceVariant = Color(0xFFF0F2F6),
  onSurfaceVariant = Color(0xFF5E6573),
  outline = Color(0xFFE2E5EC)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default for the premium dark finance app vibe
  dynamicColor: Boolean = false, // Disable dynamic colors to maintain the stunning brand style
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
