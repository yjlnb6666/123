package com.kernelsu.mgr.ui

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelsu.mgr.data.AppSettings
import kotlin.math.roundToInt

data class NavItem(
    val label: String,
    val icon: ImageVector
)

/**
 * KU 风格液态玻璃底部导航条（稳定版）。
 *
 * 交互：
 * - 点击任一项 → 切换（最可靠）
 * - 在导航条上左右拖动 → 滑块跟手，松手吸附并切换
 *
 * 关键点：整个导航条只有【一个】pointerInput，
 * 内部用 detectTapGestures + detectHorizontalDragGestures 分别挂载，
 * 并且拖动层放在点击层之上、高度受限，避免互相吞事件。
 */
@Composable
fun LiquidGlassNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = items.size.coerceAtLeast(1)
    val accent = AppSettings.accent
    val density = LocalDensity.current

    // 拖动过程中的临时位置（null 表示未拖动）
    var dragPos by remember { mutableFloatStateOf(-1f) }

    // 目标位置：拖动中跟手，否则跟随 selectedIndex
    val targetPos = if (dragPos >= 0f) dragPos else selectedIndex.toFloat()
    val animatedPos by animateFloatAsState(
        targetValue = targetPos,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "knob"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .height(64.dp)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(26.dp))
        ) {
            val totalWidth: Dp = maxWidth
            val totalWidthPx = with(density) { totalWidth.toPx() }
            val slotWidth = totalWidth / count
            val slotWidthPx = totalWidthPx / count
            val knobWidth = slotWidth - 10.dp

            // ---- 玻璃底座 ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            Modifier.blur(28.dp, BlurredEdgeTreatment.Unbounded)
                        else Modifier
                    )
                    .background(Color(0x2EFFFFFF))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x40FFFFFF), Color(0x0FFFFFFF))
                        )
                    )
            )
            // 顶部高光 + 边框
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x45FFFFFF), Color(0x06FFFFFF))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color(0x66FFFFFF), accent.copy(alpha = 0.22f))
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
            )

            // ---- 滑块 ----
            Box(
                Modifier
                    .offset(x = slotWidth * animatedPos + 5.dp, y = 5.dp)
                    .width(knobWidth)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.58f),
                                accent.copy(alpha = 0.30f)
                            )
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x40FFFFFF), Color(0x00FFFFFF))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x77FFFFFF), accent.copy(alpha = 0.20f))
                        ),
                        RoundedCornerShape(19.dp)
                    )
            )

            // ---- 图标 + 文字（可点击）----
            Row(
                Modifier.fillMaxWidth().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val tint = if (selected) Color.White else Color(0xFF9FB0CC)
                    Column(
                        Modifier
                            .width(slotWidth)
                            .fillMaxHeight()
                            .pointerInput(index) {
                                detectTapGestures(onTap = { onSelect(index) })
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            item.label,
                            fontSize = 11.sp,
                            color = tint,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // ---- 拖动层（只在水平位移超阈值时才消费，不影响点击）----
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(count, slotWidthPx) {
                        if (slotWidthPx <= 0f) return@pointerInput
                        var base = 0f
                        var acc = 0f
                        var dragging = false
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                base = selectedIndex.toFloat()
                                acc = 0f
                                dragging = false

                                var finished = false
                                while (!finished) {
                                    val event = awaitPointerEvent()
                                    val ch = event.changes.firstOrNull { it.id == down.id }
                                    if (ch == null) { finished = true; break }

                                    if (!ch.pressed) {
                                        if (dragging) {
                                            val target = (dragPos.takeIf { it >= 0f } ?: base)
                                                .roundToInt().coerceIn(0, count - 1)
                                            dragPos = -1f
                                            if (target != selectedIndex) onSelect(target)
                                        }
                                        finished = true
                                    } else {
                                        acc += ch.position.x - ch.previousPosition.x
                                        if (!dragging && kotlin.math.abs(acc) > 14f) {
                                            dragging = true
                                        }
                                        if (dragging) {
                                            val pos = (base + acc / slotWidthPx)
                                                .coerceIn(0f, (count - 1).toFloat())
                                            dragPos = pos
                                            ch.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
            )
        }
    }
}