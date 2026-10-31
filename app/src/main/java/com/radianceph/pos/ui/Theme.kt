package com.radianceph.pos.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** POS V3's own look: stone with an amber primary, light and dark; everything rounded (nothing sharp) */
private val Light = lightColorScheme(
    primary = Color(0xFFD97706), onPrimary = Color.White,
    secondary = Color(0xFF0D9488), onSecondary = Color.White,
    background = Color(0xFFFAFAF9), onBackground = Color(0xFF1C1917),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFF5F5F4), onSurfaceVariant = Color(0xFF57534E),
    outline = Color(0xFFD6D3D1), error = Color(0xFFE11D48),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFFBBF24), onPrimary = Color(0xFF1C1917),
    secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF042F2E),
    background = Color(0xFF0C0A09), onBackground = Color(0xFFF5F5F4),
    surface = Color(0xFF1C1917), onSurface = Color(0xFFF5F5F4),
    surfaceVariant = Color(0xFF292524), onSurfaceVariant = Color(0xFFD6D3D1),
    outline = Color(0xFF57534E), error = Color(0xFFFDA4AF),
)

@Composable
fun PosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
        content = content,
    )
}
