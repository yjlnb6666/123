package com.kernelsu.mgr.root

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootManager {

    /** 检测是否拥有 Root（尝试执行 su 并判断 uid=0） */
    fun isRooted(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine() ?: ""
            process.waitFor()
            output.contains("uid=0")
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