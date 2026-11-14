package com.kernelsu.mgr.root

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootManager {

    /**
     * 检测是否拥有 Root。
     *
     * 采用多路探测，任意一路成功即认定有 root：
     *  1. 实际执行 su 并检查 uid=0（最准确，但受 SELinux 跨域限制）
     *  2. 检查 KernelSU / Magisk 的管理目录是否可读（root 授权后才有权限）
     *  3. 检查 /data/adb 下的 root 痕迹
     */
    fun isRooted(): Boolean {
        // 1) 实际执行 su
        if (trySu("id")) return true
        if (trySu("su -c id")) return true
        if (trySu("/system/bin/su -c id")) return true

        // 2) KernelSU / Magisk 目录探测（普通 App 无权访问，能读到即说明已授权）
        if (canAccessRootPath("/data/adb/ksu/.allowlist")) return true
        if (canAccessRootPath("/data/adb/ksu/bin/ksud")) return true
        if (canAccessRootPath("/data/adb/magisk")) return true
        if (canAccessRootPath("/data/adb/modules")) return true

        // 3) 检查 su 二进制是否存在于常见路径（辅助判断）
        if (fileExists("/system/bin/su") && trySu("su 0 id")) return true

        return false
    }

    /** 判断某个只有 root 才能读的路径是否可访问 */
    private fun canAccessRootPath(path: String): Boolean {
        return try {
            val f = java.io.File(path)
            f.exists() && (f.canRead() || f.list() != null)
        } catch (e: Exception) {
            false
        }
    }

    private fun fileExists(path: String): Boolean = try {
        java.io.File(path).exists()
    } catch (e: Exception) {
        false
    }

    /** 执行一条 su 命令，判断输出是否包含 root 标识 */
    private fun trySu(cmd: String): Boolean {
        return try {
            val process = ProcessBuilder("sh", "-c", cmd)
                .redirectErrorStream(true)
                .start()

            val sb = StringBuilder()
            val reader = Thread {
                runCatching {
                    process.inputStream.bufferedReader().forEachLine { sb.appendLine(it) }
                }
            }
            reader.start()

            val finished = process.waitFor(8, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return false
            }
            reader.join(1500)

            val out = sb.toString()
            out.contains("uid=0") || out.contains("uid:0") ||
                out.contains("uid=0(root)") || out.contains("(root)")
        } catch (e: Exception) {
            false
        }
    }

    /** 获取内核版本 */
    fun getKernelVersion(): String = System.getProperty("os.version") ?: runCommand("uname -r")

    /** 获取 SELinux 状态 */
    fun getSelinuxStatus(): String {
        val s = runCommand("getenforce")
        return when {
            s.contains("Enforcing") -> "强制执行"
            s.contains("Permissive") -> "宽容模式"
            s.contains("Disabled") -> "已禁用"
            else -> "未知"
        }
    }

    /** 获取 Seccomp 状态 */
    fun getSeccompStatus(): String {
        val raw = runCommand("grep Seccomp /proc/self/status").replace("Seccomp:", "").trim()
        return when (raw) {
            "0" -> "未启用"
            "1" -> "严格模式"
            "2" -> "过滤模式"
            else -> if (raw.isBlank()) "过滤模式" else raw
        }
    }

    /** 以 Root 执行命令，返回输出 */
    fun execAsRoot(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes(command + "\n")
            os.writeBytes("exit\n")
            os.flush()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            val sb = StringBuilder()
            reader.forEachLine { sb.append(it).append("\n") }
            errReader.forEachLine { sb.append(it).append("\n") }
            process.waitFor()
            sb.toString().ifBlank { "执行完成（无输出）" }
        } catch (e: Exception) {
            "执行失败: ${e.message}"
        }
    }

    private fun runCommand(cmd: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val sb = StringBuilder()
            BufferedReader(InputStreamReader(p.inputStream)).forEachLine { sb.append(it).append("\n") }
            p.waitFor()
            sb.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }
}