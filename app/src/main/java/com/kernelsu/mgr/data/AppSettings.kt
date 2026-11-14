package com.kernelsu.mgr.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 全局外观设置：背景配色 / 主题强调色 / 过渡动画速度。
 * 用 Compose 的 mutableStateOf 保存，改一次全局立即重组生效。
 *
 * 注意：所有对外修改都走 applyXxx() 方法，避免与属性 setter 产生 JVM 签名冲突。
 */
object AppSettings {

    /** 过渡动画倍率：0 = 关闭，0.55 = 快，1 = 正常，1.5 = 慢 */
    var animScale by mutableStateOf(0.55f)
        private set

    /** 背景预设索引，-1 表示使用自定义 */
    var bgIndex by mutableStateOf(0)
        private set

    /** 强调色预设索引，-1 表示使用自定义 */
    var accentIndex by mutableStateOf(0)
        private set

    var customBgTop by mutableStateOf(Color(0xFF1A2A5E))
        private set
    var customBgBottom by mutableStateOf(Color(0xFF0A0F2C))
        private set
    var customAccent by mutableStateOf(Color(0xFF5B9BFF))
        private set

    /** 当前生效的强调色 */
    val accent: Color
        get() = if (accentIndex < 0) customAccent
        else AccentPresets[accentIndex.coerceIn(0, AccentPresets.lastIndex)].color

    /** 当前生效的背景渐变两端 */
    val bgColors: List<Color>
        get() = if (bgIndex < 0) listOf(customBgTop, customBgBottom)
        else BackgroundPresets[bgIndex.coerceIn(0, BackgroundPresets.lastIndex)].colors

    /** 由强调色派生的描边色 */
    val accentBorder: Color
        get() = accent.copy(alpha = 0.35f)

    fun applyAnimScale(v: Float) { animScale = v }

    fun applyBg(index: Int) { bgIndex = index }

    fun applyAccent(index: Int) { accentIndex = index }

    fun applyCustomBg(top: Color, bottom: Color) {
        customBgTop = top
        customBgBottom = bottom
        bgIndex = -1
    }

    fun applyCustomAccent(c: Color) {
        customAccent = c
        accentIndex = -1
    }

    // ---------------- 持久化 ----------------
    private const val PREF = "app_settings"

    fun load(context: Context) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        animScale = sp.getFloat("anim", 0.55f)
        bgIndex = sp.getInt("bg", 0)
        accentIndex = sp.getInt("accent", 0)
        customBgTop = Color(sp.getLong("bgTop", 0xFF1A2A5E))
        customBgBottom = Color(sp.getLong("bgBottom", 0xFF0A0F2C))
        customAccent = Color(sp.getLong("accentCustom", 0xFF5B9BFF))
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().apply {
            putFloat("anim", animScale)
            putInt("bg", bgIndex)
            putInt("accent", accentIndex)
            putLong("bgTop", customBgTop.value.toLong())
            putLong("bgBottom", customBgBottom.value.toLong())
            putLong("accentCustom", customAccent.value.toLong())
            apply()
        }
    }
}

data class ColorPreset(val name: String, val color: Color)

data class BgPreset(val name: String, val colors: List<Color>)

/** 主题强调色预设 */
val AccentPresets = listOf(
    ColorPreset("星蓝", Color(0xFF5B9BFF)),
    ColorPreset("紫罗兰", Color(0xFFA77BFF)),
    ColorPreset("青色", Color(0xFF3DD9D6)),
    ColorPreset("翠绿", Color(0xFF3FD07C)),
    ColorPreset("玫红", Color(0xFFFF6FA5)),
    ColorPreset("琥珀", Color(0xFFFFB03B))
)

/** 背景渐变预设 */
val BackgroundPresets = listOf(
    BgPreset("深蓝星空", listOf(Color(0xFF1A2A5E), Color(0xFF0A0F2C))),
    BgPreset("暗夜紫", listOf(Color(0xFF2A1B45), Color(0xFF0D0818))),
    BgPreset("纯黑", listOf(Color(0xFF16181D), Color(0xFF000000))),
    BgPreset("深海青", listOf(Color(0xFF0E3B4A), Color(0xFF04141C))),
    BgPreset("墨绿", listOf(Color(0xFF123327), Color(0xFF03100B))),
    BgPreset("暖褐", listOf(Color(0xFF3A2418), Color(0xFF140D08)))
)