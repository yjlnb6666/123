package com.kernelsu.mgr.ui.theme

import android.os.Build
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kernelsu.mgr.data.AppSettings

// ===== 固定语义色（不随主题变） =====
val StatusGreenBg = Color(0x66D6F5D6)
val StatusGreenText = Color(0xFF1E7B1E)
val StatusGreenIcon = Color(0xFF35C759)
val TextMuted = Color(0xFFB0C4DE)
val TextSub = Color(0xFFC0D0E8)

// ===== 兼容旧引用（LoginScreen 等仍在用） =====
val AccentBlue: Color get() = AppSettings.accent
val GlassBorder: Color get() = AppSettings.accentBorder

/**
 * 兼容旧引用：玻璃卡片底色（已改名，避免与 GlassCard() 函数冲突）
 */
val GlassCardColor: Color get() = Color(0x1Affffff)

/** 全局动画时长换算：AppSettings.animScale 为 0 时直接返回 0（关动画） */
fun animDuration(baseMs: Int): Int = (baseMs * AppSettings.animScale).toInt().coerceAtLeast(0)

/** 生成 tween spec，统一受动画速度设置控制 */
fun <T> fastTween(baseMs: Int = 260): AnimationSpec<T> =
    tween(durationMillis = animDuration(baseMs))

/**
 * 动态背景：跟随 AppSettings 的背景预设 / 自定义色，
 * 叠加两团柔和光斑，让玻璃卡片有可"透出来"的层次。
 */
@Composable
fun BlueGlassBackground(content: @Composable () -> Unit) {
    val colors = AppSettings.bgColors
    val accent = AppSettings.accent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors))
    ) {
        // 左上角强调色光晕
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                        radius = 900f
                    )
                )
        )
        content()
    }
}

/**
 * 液态玻璃卡片。
 *
 * 实现方式（Android 10+ 全兼容）：
 * 1. 先画一层放大后的模糊底色 —— 12+ 走 RenderEffect 真实模糊，
 *    10/11 自动降级为半透明叠加（Compose 的 blur 在旧版会退化为无操作，所以额外补一层渐变）。
 * 2. 上面再叠一层从上到下由亮到淡的白色渐变，模拟玻璃的高光反射。
 * 3. 1dp 强调色描边 + 轻微内高光，形成"液态"边界感。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Int = 24,
    content: @Composable () -> Unit
) {
    val accent = AppSettings.accent
    val shape = RoundedCornerShape(radius.dp)
    val canRealBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = modifier.clip(shape)) {
        // 底层：模糊玻璃层
        Box(
            Modifier
                .matchParentSize()
                .then(if (canRealBlur) Modifier.blur(18.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                .background(Color(0x26FFFFFF))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF))
                    )
                )
        )
        // 顶层：内容 + 边框
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {}
        Box(Modifier.padding(16.dp)) { content() }
    }
}