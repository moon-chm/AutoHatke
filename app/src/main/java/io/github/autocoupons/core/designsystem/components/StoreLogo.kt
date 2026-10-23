package io.github.autocoupons.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.autocoupons.core.designsystem.Jakarta

/**
 * Store tile artwork. Original monogram tiles in each store's colours; no third-party logo
 * artwork is bundled.
 */
private data class StoreArt(val brush: Brush, val glyph: String, val style: TextStyle)

private fun artFor(storeId: String): StoreArt = when (storeId) {
    "demo-store" -> StoreArt(
        Brush.linearGradient(listOf(Color(0xFF111827), Color(0xFF3F3F46))),
        "m",
        TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = Color(0xFFF5D0A9)),
    )
    "myntra" -> StoreArt(
        Brush.linearGradient(listOf(Color(0xFFFF3F6C), Color(0xFFFF905A))),
        "M",
        TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, color = Color.White),
    )
    else -> StoreArt(
        Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF94A3B8))),
        storeId.take(1).uppercase(),
        TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, color = Color.White),
    )
}

@Composable
fun StoreLogo(storeId: String, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val art = artFor(storeId)
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(art.brush),
        contentAlignment = Alignment.Center,
    ) {
        Text(art.glyph, style = art.style.copy(fontSize = (size.value * 0.52f).sp, lineHeight = (size.value * 0.6f).sp))
    }
}
