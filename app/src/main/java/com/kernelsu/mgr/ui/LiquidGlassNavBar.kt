package com.kernelsu.mgr.ui

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelsu.mgr.data.AppSettings
import kotlin.math.roundToInt

data class NavItem(
    val label: String,
    val icon: ImageVector
)

/**
 * 液态玻璃底部导航条 —— 最终稳定版。
 *
 * 关键改动：
 *   1. 每一项都是独立的 clickable，点哪儿切哪儿（不依赖坐标换算）
 *   2. 拖动用 detectHorizontalDragGestures 挂在「整条」上，
 *      但它的 onDragStart 只有真正开始拖才会触发，不会影响点击
 *   3. 滑块位置用 animateFloatAsState 平滑跟随
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

    var dragging by remember { mutableFloatStateOf(-1f) }

    val targetPos = if (dragging >= 0f) dragging else selectedIndex.toFloat()
    val pos by animateFloatAsState(
        targetValue = targetPos,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
        label = "knobPos"
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
            val totalWidth = maxWidth
            val slotWidth = totalWidth / count
            val knobWidth = slotWidth - 10.dp

            // 玻璃底座
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            Modifier.blur(26.dp, BlurredEdgeTreatment.Unbounded)
                        else Modifier
                    )
                    .background(Color(0x2EFFFFFF))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x40FFFFFF), Color(0x0FFFFFFF))
                        )
                    )
            )
            // 高光描边
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x42FFFFFF), Color(0x06FFFFFF))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x66FFFFFF), accent.copy(alpha = 0.22f))
                        ),
                        RoundedCornerShape(26.dp)
                    )
            )

            // 滑块
            Box(
                Modifier
                    .offset(x = slotWidth * pos + 5.dp, y = 5.dp)
                    .width(knobWidth)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.58f), accent.copy(alpha = 0.30f))
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

            // 图标 + 文字（每项独立可点）
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
                            .clickable(
                                interactionSource = null,
                                indication = null
                            ) { onSelect(index) },
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

            // 拖动层（最上层，但只在水平滑动时才消费事件 → 不影响点击）
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(count, slotWidth) {
                        val slotPx = with(density) { slotWidth.toPx() }
                        if (slotPx <= 0f) return@pointerInput
                        var startPos = 0f
                        var acc = 0f
                        var isDrag = false
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                startPos = selectedIndex.toFloat()
                                acc = 0f
                                isDrag = false
                                dragging = -1f

                                var done = false
                                while (!done) {
                                    val ev = awaitPointerEvent()
                                    val ch = ev.changes.firstOrNull { it.id == down.id }
                                    if (ch == null) { done = true; break }

                                    if (!ch.pressed) {
                                        if (isDrag) {
                                            val t = (if (dragging < 0f) startPos else dragging)
                                                .roundToInt().coerceIn(0, count - 1)
                                            dragging = -1f
                                            if (t != selectedIndex) onSelect(t)
                                        }
                                        done = true
                                    } else {
                                        acc += ch.position.x - ch.previousPosition.x
                                        if (!isDrag && kotlin.math.abs(acc) > 16f) isDrag = true
                                        if (isDrag) {
                                            dragging = (startPos + acc / slotPx)
                                                .coerceIn(0f, (count - 1).toFloat())
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