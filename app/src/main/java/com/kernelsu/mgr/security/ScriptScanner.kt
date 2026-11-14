package com.kernelsu.mgr.security

import java.util.Base64

/**
 * 脚本安全静态扫描器。
 *
 * 目标：在执行任何脚本前，识别出可能导致设备损坏 / 逃逸沙盒的危险意图。
 * 判定结果分三档：
 *   - SAFE   : 未发现危险特征，可直接执行
 *   - WARN   : 有可疑特征但不致命，执行前提醒用户
 *   - DANGER : 明确危险（删分区、dd 设备节点、提权逃逸等），拒绝执行
 *
 * 注意：静态扫描不能 100% 保证安全，它只是第一道防线。
 * 真正的保底是"执行环境本身没有真 root、没有真设备节点"。
 */
object ScriptScanner {

    enum class Level { SAFE, WARN, DANGER }

    data class Finding(val level: Level, val keyword: String, val reason: String)

    data class Result(
        val level: Level,
        val findings: List<Finding>
    ) {
        val isDanger: Boolean get() = level == Level.DANGER
        val isSafe: Boolean get() = level == Level.SAFE
        val summary: String
            get() = when (level) {
                Level.SAFE -> "未发现危险特征，可直接执行"
                Level.WARN -> "发现 " + findings.size + " 处可疑特征，建议谨慎执行"
                Level.DANGER -> "发现 " + findings.size + " 处高危操作，已阻止执行"
            }
    }

    /** 致命级：直接拒绝执行 */
    private val DANGER_RULES: List<Triple<Regex, String, String>> = listOf(
        Triple(
            Regex("""\bdd\b[^\n]*\bof\s*=\s*/dev/""", RegexOption.IGNORE_CASE),
            "dd 写入块设备",
            "向设备节点写入数据，可覆盖分区导致变砖"
        ),
        Triple(
            Regex("""/dev/block/""", RegexOption.IGNORE_CASE),
            "/dev/block",
            "访问块设备节点，可读写物理分区"
        ),
        Triple(
            Regex("""/dev/mtd""", RegexOption.IGNORE_CASE),
            "/dev/mtd",
            "访问 MTD 分区设备"
        ),
        Triple(
            Regex("""\bmkfs(\.\w+)?\b""", RegexOption.IGNORE_CASE),
            "mkfs",
            "格式化分区，会清空数据"
        ),
        Triple(
            Regex("""\bmke2fs\b|\bmake_ext4fs\b|\bmkdosfs\b""", RegexOption.IGNORE_CASE),
            "mke2fs/make_ext4fs",
            "创建文件系统，会覆写分区"
        ),
        Triple(
            Regex("""\bfastboot\b""", RegexOption.IGNORE_CASE),
            "fastboot",
            "刷机命令"
        ),
        Triple(
            Regex("""\bflash\s+(boot|system|recovery|vbmeta|super)\b""", RegexOption.IGNORE_CASE),
            "flash 分区",
            "刷写系统分区"
        ),
        Triple(
            Regex("""\bwipe\b""", RegexOption.IGNORE_CASE),
            "wipe",
            "擦除数据/分区"
        ),
        Triple(
            Regex("""\berase\b.*\b(partition|block|boot|system)\b""", RegexOption.IGNORE_CASE),
            "erase 分区",
            "擦除分区数据"
        ),
        Triple(
            Regex("""\bmagisk\b|\bksud\b""", RegexOption.IGNORE_CASE),
            "magisk/ksud",
            "调用 root 管理工具，可能试图提权逃逸"
        ),
        Triple(
            Regex("""\breboot\s+recovery\b|\breboot\s+bootloader\b""", RegexOption.IGNORE_CASE),
            "reboot 特殊模式",
            "重启进入 recovery/bootloader，风险极高"
        ),
        Triple(
            Regex("""\brm\s+-[rRf]{1,3}\s+/(\s|$)""", RegexOption.IGNORE_CASE),
            "rm -rf /",
            "删除根目录"
        ),
        Triple(
            Regex("""\bsetprop\s+ro\.(secure|debuggable|adbd)\b""", RegexOption.IGNORE_CASE),
            "setprop ro.*",
            "修改系统只读属性"
        ),
        // ===== 精确拦截：覆写 / 删除系统分区（这才是真正会变砖的）=====
        Triple(
            Regex("""\bdd\b[^\n]*\bof\s*=\s*(/dev/|/system|/vendor|/boot|/recovery|/data/adb|/metadata)""", RegexOption.IGNORE_CASE),
            "dd 覆写系统区域",
            "向系统分区/设备节点写入，会破坏系统"
        ),
        Triple(
            Regex("""\brm\s+-[rR]{0,2}f?[rRf]*\s+(/system|/vendor|/boot|/recovery|/data/adb|/metadata|/dev/block)(\s|/|$)""", RegexOption.IGNORE_CASE),
            "删除系统目录",
            "递归删除系统分区内容"
        ),
        Triple(
            Regex("""\brm\s+-[rRf]{1,3}\s+/(\s|$)""", RegexOption.IGNORE_CASE),
            "rm -rf /",
            "删除根目录"
        ),
        Triple(
            Regex("""\bcp\b[^\n]*\s(/system|/vendor|/boot|/recovery)/[^\n]*""", RegexOption.IGNORE_CASE),
            "写入系统分区",
            "复制文件覆盖系统分区内容"
        ),
        Triple(
            Regex(""">\s*(/system|/vendor|/boot|/recovery|/dev/block)/""", RegexOption.IGNORE_CASE),
            "重定向写入系统分区",
            "通过 > 覆盖系统分区文件"
        ),
        Triple(
            Regex("""\btruncate\b[^\n]*(/system|/vendor|/boot|/dev/block)""", RegexOption.IGNORE_CASE),
            "truncate 系统文件",
            "截断系统文件大小，破坏系统"
        ),
        Triple(
            Regex("""\bchmod\s+[0-7]{3,4}\s+(/system|/vendor|/boot)(\s|/|$)""", RegexOption.IGNORE_CASE),
            "修改系统分区权限",
            "改动系统分区权限，可能导致无法启动"
        )
    )

    /** 警告级：提醒但允许执行 */
    private val WARN_RULES: List<Triple<Regex, String, String>> = listOf(
        Triple(
            Regex("""(^|[\s;|&(])su(\s|$|;|&|\|)""", RegexOption.MULTILINE),
            "su",
            "调用 su 提权（允许，但会实际以 root 执行）"
        ),
        Triple(Regex("""\breboot\b""", RegexOption.IGNORE_CASE), "reboot", "重启设备"),
        Triple(Regex("""\bpoweroff\b|\bshutdown\b""", RegexOption.IGNORE_CASE), "poweroff", "关机"),
        Triple(Regex("""\bmount\s+-o\s+rw\b""", RegexOption.IGNORE_CASE), "mount -o rw", "以读写方式重挂载"),
        Triple(Regex("""\bchmod\s+777\b""", RegexOption.IGNORE_CASE), "chmod 777", "开放全部权限"),
        Triple(Regex("""\biptables\b|\bnft\b""", RegexOption.IGNORE_CASE), "iptables", "修改防火墙规则"),
        Triple(Regex("""\bcurl\b|\bwget\b""", RegexOption.IGNORE_CASE), "curl/wget", "联网下载内容"),
        Triple(Regex("""\beval\b""", RegexOption.IGNORE_CASE), "eval", "动态执行代码，可被用于混淆"),
        Triple(Regex("""/data/data/""", RegexOption.IGNORE_CASE), "访问其他应用数据", "读取其他 app 私有数据"),
        Triple(Regex("""\bcrontab\b|\binit\.d\b""", RegexOption.IGNORE_CASE), "自启动挂载", "修改开机启动项"),
        Triple(
            Regex("""\bsetprop\b""", RegexOption.IGNORE_CASE),
            "setprop",
            "修改系统属性"
        )
    )

    /**
     * 扫描脚本内容。
     * 会先做一轮"去混淆"：还原 base64 片段、 stripping 注释与空行，然后再匹配规则。
     */
    fun scan(raw: String): Result {
        val findings = mutableListOf<Finding>()
        if (raw.isBlank()) {
            return Result(Level.WARN, listOf(Finding(Level.WARN, "空脚本", "脚本内容为空")))
        }

        // 去掉注释行，避免注释里的关键词误报
        val stripped = raw.lines()
            .filterNot { it.trimStart().startsWith("#") }
            .joinToString("\n")

        val candidates = buildList {
            add(raw to "原文")
            add(stripped to "去注释")
            decodeBase64Chunks(raw).forEach { (decoded, _) -> add(decoded to "base64解码") }
            add(unescapeHex(raw) to "十六进制还原")
        }

        val seen = mutableSetOf<String>()

        candidates.forEach { (text, source) ->
            DANGER_RULES.forEach { (regex, kw, reason) ->
                if (regex.containsMatchIn(text)) {
                    val key = "D:$kw"
                    if (seen.add(key)) {
                        findings.add(Finding(Level.DANGER, kw, "$reason（来源：$source）"))
                    }
                }
            }
        }

        // 没命中致命项时，才看警告项
        if (findings.none { it.level == Level.DANGER }) {
            candidates.forEach { (text, source) ->
                WARN_RULES.forEach { (regex, kw, reason) ->
                    if (regex.containsMatchIn(text)) {
                        val key = "W:$kw"
                        if (seen.add(key)) {
                            findings.add(Finding(Level.WARN, kw, "$reason（来源：$source）"))
                        }
                    }
                }
            }
        }

        val level = when {
            findings.any { it.level == Level.DANGER } -> Level.DANGER
            findings.isNotEmpty() -> Level.WARN
            else -> Level.SAFE
        }
        return Result(level, findings)
    }

    /** 找出脚本里像 base64 的长串并解码 */
    private fun decodeBase64Chunks(text: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val re = Regex("""[A-Za-z0-9+/]{40,}={0,2}""")
        re.findAll(text).take(8).forEach { m ->
            try {
                val decoded = String(Base64.getDecoder().decode(m.value))
                if (decoded.all { it.isLetterOrDigit() || it in " \t\n\r/._-$;#:|()&<>=" }) {
                    out.add(decoded to m.value.take(20))
                }
            } catch (_: Exception) { }
        }
        // 也处理 echo "xxx" | base64 -d 这种
        val pipeRe = Regex("""base64\s+-d|base64\s+--decode""")
        if (pipeRe.containsMatchIn(text)) {
            out.add("【检测到 base64 解码管道】" to "base64 -d")
        }
        return out
    }

    /** 还原 $'\x64\x64' 形式的十六进制转义 */
    private fun unescapeHex(text: String): String {
        val re = Regex("""\\x([0-9a-fA-F]{2})""")
        return try {
            re.replace(text) { m ->
                m.groupValues[1].toInt(16).toChar().toString()
            }
        } catch (_: Exception) { text }
    }
}