package com.kernelsu.mgr.root

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Root 检测与执行。
 *
 * 检测策略（从最可靠到兜底，任一成功即判定有 Root）：
 *   1. Runtime.exec("su -c id")  —— 最通用，KernelSU/Magisk 都支持
 *   2. 写 stdin 的交互式 su      —— 兼容只接受 stdin 命令的实现
 *   3. 多个 su 路径兜底
 */
object RootManager {

    @Volatile
    private var cachedResult: Boolean? = null

    /** 带缓存的检测（App 启动时调用一次） */
    fun isRooted(useCache: Boolean = false): Boolean {
        if (useCache) cachedResult?.let { return it }
        val r = detect()
        cachedResult = r
        return r
    }

    fun clearCache() {
        cachedResult = null
    }

    /** 完整诊断：把每种检测方式的原始输出都列出来 */
    fun diagnose(): String {
        val sb = StringBuilder()
        sb.appendLine("=== uid ===")
        sb.appendLine(runCommand("id"))
        sb.appendLine()
        sb.appendLine("=== which su ===")
        sb.appendLine(runCommand("which su").ifBlank { "未找到" })
        sb.appendLine()
        sb.appendLine("=== su -c id ===")
        sb.appendLine(runSuArgs("-c", "id").ifBlank { "（空输出）" })
        sb.appendLine()
        sb.appendLine("=== su (stdin) ===")
        sb.appendLine(runSuStdin("id").ifBlank { "（空输出）" })
        sb.appendLine()
        sb.appendLine("=== /system/bin/su ===")
        sb.appendLine(runCommand("ls -la /system/bin/su").ifBlank { "不存在" })
        sb.appendLine()
        sb.appendLine("=== allowlist 含本包? ===")
        sb.appendLine(runCommand("grep -c com.kernelsu.mgr /data/adb/ksu/.allowlist 2>/dev/null").ifBlank { "无法读取" })
        sb.appendLine()
        sb.appendLine("=== 判定结果 ===")
        sb.appendLine(if (detect()) "✅ 有 Root" else "❌ 无 Root")
        return sb.toString()
    }

    private fun detect(): Boolean {
        // 方案 1：su -c id（最常用）
        if (matchRoot(runSuArgs("-c", "id"))) return true
        // 方案 2：交互式（写 stdin）
        if (matchRoot(runSuStdin("id"))) return true
        // 方案 3：不同路径
        for (p in listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su")) {
            if (matchRoot(runSuArgsAt(p, "-c", "id"))) return true
        }
        // 方案 4：Magisk/KSU 主动授权接口
        if (matchRoot(runSuArgs("--help"))) return false // 能跑通但输出了帮助，不算
        return false
    }

    private fun matchRoot(out: String): Boolean {
        if (out.isBlank()) return false
        return out.contains("uid=0") || out.contains("uid:0") || out.contains("(root)")
    }

    /** su -c xxx（直接 exec 数组，避免 shell 解析问题） */
    private fun runSuArgs(vararg args: String): String =
        execCollect(arrayOf("su") + args, 8000)

    private fun runSuArgsAt(path: String, vararg args: String): String =
        execCollect(arrayOf(path) + args, 8000)

    /** 交互式：启动 su，往 stdin 写命令 */
    private fun runSuStdin(cmd: String): String {
        return try {
            val p = Runtime.getRuntime().exec("su")
            Thread {
                runCatching {
                    val os = DataOutputStream(p.outputStream)
                    os.writeBytes(cmd + "\n")
                    os.writeBytes("exit\n")
                    os.flush()
                    os.close()
                }
            }.start()

            val sb = StringBuilder()
            val t = Thread {
                runCatching {
                    BufferedReader(InputStreamReader(p.inputStream)).forEachLine {
                        sb.appendLine(it)
                    }
                }
            }
            t.start()

            val ok = p.waitFor(8, java.util.concurrent.TimeUnit.SECONDS)
            if (!ok) { p.destroyForcibly(); return "" }
            t.join(1200)
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    /** 通用：执行命令并收集 stdout+stderr */
    private fun execCollect(command: Array<String>, timeoutMs: Long): String {
        return try {
            val p = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()

            val sb = StringBuilder()
            val t = Thread {
                runCatching {
                    BufferedReader(InputStreamReader(p.inputStream)).forEachLine {
                        sb.appendLine(it)
                    }
                }
            }
            t.start()

            val ok = p.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!ok) {
                p.destroyForcibly()
                return sb.toString()
            }
            t.join(1200)
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    // ================= 设备信息（不依赖 root） =================

    fun getKernelVersion(): String =
        runCommand("uname -r").ifBlank { System.getProperty("os.version") ?: "未知" }

    fun getSelinuxStatus(): String {
        val s = runCommand("getenforce")
        return when {
            s.contains("Enforcing") -> "强制执行"
            s.contains("Permissive") -> "宽容模式"
            s.contains("Disabled") -> "已禁用"
            else -> "未知"
        }
    }

    fun getSeccompStatus(): String {
        val raw = runCommand("grep Seccomp /proc/self/status").replace("Seccomp:", "").trim()
        return when (raw) {
            "0" -> "未启用"
            "1" -> "严格模式"
            "2" -> "过滤模式"
            else -> if (raw.isBlank()) "过滤模式" else raw
        }
    }

    /** 以 Root 执行命令，返回输出（脚本执行用） */
    fun execAsRoot(command: String): String {
        val out = execCollect(arrayOf("su", "-c", command), 60000)
        return out.ifBlank { "执行完成（无输出）" }
    }

    private fun runCommand(cmd: String): String =
        execCollect(arrayOf("sh", "-c", cmd), 6000).trim()
}