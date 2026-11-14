package com.kernelsu.mgr.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val GlassBlueTop = Color(0xFF1A2A5E)
val GlassBlueBottom = Color(0xFF0A0F2C)
val GlassCard = Color(0x22FFFFFF)
val GlassBorder = Color(0x40A8C8FF)
val AccentBlue = Color(0xFF5B9BFF)
val StatusGreenBg = Color(0xFFD6F5D6)
val StatusGreenText = Color(0xFF1E7B1E)
val StatusGreenIcon = Color(0xFF35C759)
val TextMuted = Color(0xFFB0C4DE)
val TextSub = Color(0xFFC0D0E8)

/** 深蓝渐变背景 */
@Composable
fun BlueGlassBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GlassBlueTop, GlassBlueBottom)))
    ) { content() }
}

/** 玻璃卡片：半透明 + 蓝色描边（Android 10+ 全兼容） */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Int = 24,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(radius.dp)),
        shape = RoundedCornerShape(radius.dp),
        color = GlassCard,
        border = BorderStroke(1.dp, GlassBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}