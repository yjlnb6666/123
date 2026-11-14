package com.kernelsu.mgr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelsu.mgr.data.AccentPresets
import com.kernelsu.mgr.data.AppSettings
import com.kernelsu.mgr.data.BackgroundPresets
import com.kernelsu.mgr.ui.theme.GlassCard
import com.kernelsu.mgr.ui.theme.TextMuted
import kotlin.math.roundToInt

@Composable
fun AccountScreen(
    account: String,
    onExit: () -> Unit,
    onSettingsChanged: () -> Unit = {}
) {
    val accent = AppSettings.accent

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== 账号信息 =====
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text("当前账号", fontSize = 13.sp, color = TextMuted)
                Text(
                    account,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // ===== 外观设置 =====
        SectionTitle("外观设置", Icons.Filled.Palette)

        // --- 背景配色 ---
        GlassCard(Modifier.fillMaxWidth()) {
            Column {
                Text("背景配色", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BackgroundPresets.forEachIndexed { index, preset ->
                        val selected = AppSettings.bgIndex == index
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(preset.colors))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) accent else Color(0x33FFFFFF),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        AppSettings.applyBg(index)
                                        onSettingsChanged()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                preset.name,
                                fontSize = 9.sp,
                                color = if (selected) accent else TextMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                // 自定义背景：从强调色派生一组备选，避免引入复杂取色器
                Text("自定义背景", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CustomBgPresets.forEachIndexed { i, colors ->
                        val selected = AppSettings.bgIndex == -1 &&
                            AppSettings.customBgTop == colors[0] &&
                            AppSettings.customBgBottom == colors[1]
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.verticalGradient(colors))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) accent else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    AppSettings.applyCustomBg(colors[0], colors[1])
                                    onSettingsChanged()
                                }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 主题色 ---
        GlassCard(Modifier.fillMaxWidth()) {
            Column {
                Text("主题颜色", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentPresets.forEachIndexed { index, preset ->
                        val selected = AppSettings.accentIndex == index
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(preset.color)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    AppSettings.applyAccent(index)
                                    onSettingsChanged()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("自定义主题色", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CustomAccentColors.forEach { color ->
                        val selected = AppSettings.accentIndex == -1 &&
                            AppSettings.customAccent == color
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) Color.White else Color(0x33FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable {
                                    AppSettings.applyCustomAccent(color)
                                    onSettingsChanged()
                                }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- 过渡动画 ---
        GlassCard(Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Speed,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("过渡动画速度", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        animScaleLabel(AppSettings.animScale),
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = AppSettings.animScale,
                    onValueChange = { AppSettings.applyAnimScale(it) },
                    onValueChangeFinished = { onSettingsChanged() },
                    valueRange = 0f..1.5f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )
                Text(
                    "0 = 关闭动画，越小越快",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== 退出程序 =====
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
        ) {
            Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("退出程序", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SectionTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AppSettings.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

private fun animScaleLabel(v: Float): String = when {
    v <= 0.001f -> "关闭"
    v < 0.3f -> "极快"
    v < 0.6f -> "快"
    v < 0.95f -> "正常"
    v < 1.3f -> "稍慢"
    else -> "慢"
}

/** 自定义背景候选（上→下渐变） */
private val CustomBgPresets = listOf(
    listOf(Color(0xFF2B1055), Color(0xFF7597DE)),
    listOf(Color(0xFF41295A), Color(0xFF2F0743)),
    listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
    listOf(Color(0xFF232526), Color(0xFF414345)),
    listOf(Color(0xFF1D4350), Color(0xFFA43931))
)

/** 自定义强调色候选 */
private val CustomAccentColors = listOf(
    Color(0xFFFF4D6D),
    Color(0xFF00C2A8),
    Color(0xFF8E7CFF),
    Color(0xFF29B6F6),
    Color(0xFFFF8A3D),
    Color(0xFFB4E33D)
)