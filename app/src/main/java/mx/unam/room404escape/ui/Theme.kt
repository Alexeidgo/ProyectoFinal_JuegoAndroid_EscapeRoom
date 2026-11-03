package mx.unam.room404escape.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun palette(alternate: Boolean) = darkColorScheme(
    primary = if (alternate) Color(0xFFD9C1A2) else Color(0xFFFFB4AB),
    onPrimary = Color(0xFF380B0D),
    secondary = if (alternate) Color(0xFFB7B0A1) else Color(0xFFC7A4A0),
    background = Color(0xFF0C0B0D), surface = Color(0xFF171416),
    surfaceVariant = Color(0xFF292124), onSurface = Color(0xFFE8DFD6),
    onBackground = Color(0xFFE8DFD6), outline = Color(0xFF736267)
)
private val Type = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 46.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 25.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
)
@Composable
fun HorrorTheme(alternate: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = palette(alternate), typography = Type, content = content)
}
