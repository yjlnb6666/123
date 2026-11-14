package com.kernelsu.mgr.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelsu.mgr.data.AppSettings
import com.kernelsu.mgr.ui.theme.animDuration
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class NavItem(
    val label: String,
    val icon: ImageVector
)

/**
 * KU 风格的液态玻璃底部导航条。
 *
 * 交互：
 * - 手指在导航条上左右拖动 → 玻璃滑块跟手移动，页面也轻微跟手
 * - 松手后吸附到最近的一项并触发切换
 * - 也可以直接点击某项
 *
 * 视觉：
 * - 外层：圆角胶囊 + 半透明模糊底 + 顶部高光 + 渐变描边
 * - 内层：跟随移动的高亮滑块（强调色 + 内部高光）
 */
@Composable
fun LiquidGlassNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDragProgress: (Float) -> Unit = {}
) {
    val count = items.size
    val accent = AppSettings.accent
    val density = LocalDensity.current

    // 滑块位置（单位为“项”，0f .. count-1f）
    val offset = remember { Animatable(selectedIndex.toFloat()) }
    val scope = rememberCoroutineScope()

    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(selectedIndex.toFloat()) }

    // 外部切换（点击/滑页）时同步滑块
    LaunchedEffect(selectedIndex) {
        if (!dragging) {
            offset.animateTo(selectedIndex.toFloat(), tween(animDuration(200)))
        }
    }

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
            // 滑块宽度比格子略窄，留出边距
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
            // 顶部高光 + 渐变边框
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
            val knobLeftDp = slotWidth * offset.value + 5.dp
            Box(
                Modifier
                    .offset(x = knobLeftDp, y = 5.dp)
                    .width(knobWidth)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.55f),
                                accent.copy(alpha = 0.28f)
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

            // ---- 图标 + 文字 ----
            Row(
                Modifier.fillMaxWidth().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val tint = if (selected) Color.White else Color(0xFF9FB0CC)
                    Column(
                        Modifier.width(slotWidth),
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

            // ---- 手势层（单一 pointerInput 统一处理拖动 + 点击，避免多手势竞争）----
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(count, slotWidthPx) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startX = down.position.x
                                val startOffset = offset.value
                                var isDrag = false
                                var moved = false
                                down.consume()

                                // 跟踪本次触摸
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                    if (change.pressed) {
                                        val dx = change.position.x - startX
                                        if (!isDrag && kotlin.math.abs(dx) > 12f) {
                                            isDrag = true
                                            moved = true
                                            dragging = true
                                            dragPos = startOffset
                                        }
                                        if (isDrag) {
                                            val base = startOffset * slotWidthPx
                                            val newPos =
                                                ((base + dx) / slotWidthPx)
                                                    .coerceIn(0f, (count - 1).toFloat())
                                            dragPos = newPos
                                            scope.launch { offset.snapTo(newPos) }
                                            onDragProgress(newPos)
                                            change.consume()
                                        }
                                    } else {
                                        // 手指抬起
                                        if (isDrag) {
                                            dragging = false
                                            val target =
                                                dragPos.roundToInt().coerceIn(0, count - 1)
                                            scope.launch {
                                                offset.animateTo(
                                                    target.toFloat(),
                                                    spring(dampingRatio = 0.8f, stiffness = 620f)
                                                )
                                            }
                                            if (target != selectedIndex) onSelect(target)
                                        } else if (!moved) {
                                            // 认定为点击
                                            val idx =
                                                (startX / slotWidthPx).toInt()
                                                    .coerceIn(0, count - 1)
                                            if (idx != selectedIndex) onSelect(idx)
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    }
            )
        }
    }
}