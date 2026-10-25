package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = PythonCyan,
  onPrimary = Color(0xFF003544),
  primaryContainer = PythonNavy,
  onPrimaryContainer = PythonCyan,
  secondary = PythonGold,
  onSecondary = Color(0xFF3E2C00),
  secondaryContainer = Color(0xFF332912),
  onSecondaryContainer = PythonYellow,
  tertiary = PythonAccentGreen,
  background = PythonBlueDark,
  onBackground = Color(0xFFF8FAFC),
  surface = PythonSurface,
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = PythonSurfaceVariantDark,
  onSurfaceVariant = Color(0xFFCBD5E1),
  error = PythonError
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF0284C7),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = Color(0xFF0369A1),
  secondary = Color(0xFFD97706),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFFEF3C7),
  onSecondaryContainer = Color(0xFF92400E),
  tertiary = Color(0xFF0D9488),
  background = Color(0xFFF8FAFC),
  onBackground = Color(0xFF0F172A),
  surface = Color.White,
  onSurface = Color(0xFF1E293B),
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = Color(0xFF475569),
  error = Color(0xFFDC2626)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent branding colors by default
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
