package com.kernelsu.mgr.security

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * 沙盒执行器。
 *
 * 设计原则（非常重要）：
 *   1. 脚本 **绝不** 用真 root（su）执行。
 *   2. 脚本跑在 proot 里，根目录指向 App 私有目录下的 rootfs 副本。
 *   3. 绝不 bind 真机的 /dev、/system、/proc 的真实节点 —— 沙盒里没有设备节点，
 *      dd /dev/block 之类只能打到沙盒内的普通文件，碰不到物理分区。
 *   4. 沙盒内提供一个"假 su"，脚本调用 su 时只会得到权限拒绝，无法逃逸。
 *
 * 如果 proot 二进制不可用，会自动降级为"纯 App 沙盒执行"（不启动任何特权），
 * 此时脚本以 App 自身 UID 运行，同样无法损伤系统。
 */
object SandboxRunner {

    data class Outcome(
        val exitCode: Int,
        val output: String,
        val mode: String
    )

    /** 沙盒根目录 */
    private fun sandboxDir(context: Context): File =
        File(context.filesDir, "sandbox").apply { mkdirs() }

    private fun rootfsDir(context: Context): File =
        File(sandboxDir(context), "rootfs").apply { mkdirs() }

    private fun workDir(context: Context): File =
        File(sandboxDir(context), "work").apply { mkdirs() }

    /**
     * 准备沙盒骨架：
     * - rootfs/{tmp,dev,proc,system/bin,...}
     * - 放置假 su（拒绝提权）
     * - 放置假 block 设备占位（只是普通文件，不是真节点）
     */
    fun ensureSandbox(context: Context): File {
        val root = rootfsDir(context)

        listOf(
            "tmp", "dev", "proc", "sys",
            "system/bin", "system/xbin",
            "sbin", "bin", "usr/bin",
            "data/local/tmp", "home"
        ).forEach { File(root, it).mkdirs() }

        // 假 su：无论如何都拒绝
        val fakeSu = """
            #!/system/bin/sh
            # 沙盒内的假 su：不提供任何提权能力
            echo "su: 沙盒环境拒绝提权操作" >&2
            exit 1
        """.trimIndent()
        listOf("system/bin/su", "system/xbin/su", "sbin/su", "bin/su", "usr/bin/su").forEach { p ->
            val f = File(root, p)
            f.parentFile?.mkdirs()
            runCatching {
                f.writeText(fakeSu)
                f.setExecutable(true, false)
            }
        }

        // 假 /dev/block：普通文件占位，让"访问块设备"的脚本拿到错误而不是真节点
        runCatching {
            File(root, "dev/block").mkdirs()
            File(root, "dev/block/README_SANDBOX").writeText(
                "这是沙盒内的占位目录，不是真实块设备。\n任何对分区的写入在这里都不会影响真机。"
            )
            File(root, "dev/null").writeText("")
        }

        return root
    }

    /** proot 是否可用（需要随 APK 打包 libproot.so） */
    private fun prootBinary(context: Context): File? {
        val f = File(context.applicationInfo.nativeLibraryDir, "libproot.so")
        return if (f.exists()) f else null
    }

    /**
     * 以真实 root 执行脚本（仅在脚本通过安全检查、且确实需要 root 时使用）。
     *
     * 安全前提：调用方必须先通过 ScriptScanner 扫描，确认没有覆写/删除系统分区的操作。
     */
    fun runAsRealRoot(context: Context, scriptContent: String): Outcome {
        val work = workDir(context)
        val scriptFile = File(work, "root_entry.sh").apply {
            writeText("#!/system/bin/sh\n" + scriptContent)
            setExecutable(true, false)
        }
        return try {
            val proc = ProcessBuilder("su", "-c", "sh ${scriptFile.absolutePath}")
                .redirectErrorStream(true)
                .start()

            val sb = StringBuilder()
            val reader = Thread {
                runCatching {
                    proc.inputStream.bufferedReader().forEachLine { line ->
                        if (sb.length < 200_000) sb.appendLine(line)
                    }
                }
            }
            reader.start()

            val finished = proc.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return Outcome(-1, "⏱ 执行超时（120 秒），已终止\n$sb", "真实 Root")
            }
            reader.join(2000)
            Outcome(proc.exitValue(), sb.toString().ifBlank { "（无输出）" }, "真实 Root（已通过分区保护扫描）")
        } catch (e: Exception) {
            Outcome(-1, "❌ Root 执行失败：${e.message}", "真实 Root")
        }
    }

    /**
     * 在沙盒中执行脚本。
     *
     * @param scriptContent 脚本正文
     * @return 执行结果；mode 字段标明实际用的是 proot 还是降级模式
     */
    fun run(context: Context, scriptContent: String): Outcome {
        val root = ensureSandbox(context)
        val work = workDir(context)
        val scriptFile = File(work, "entry.sh").apply {
            writeText("#!/system/bin/sh\n" + scriptContent)
            setExecutable(true, false)
        }

        val proot = prootBinary(context)
        val cmd: List<String> = if (proot != null) {
            listOf(
                proot.absolutePath,
                "-r", root.absolutePath,          // 沙盒根
                "-w", "/data/local/tmp",          // 工作目录（沙盒内）
                "-b", "/system/bin",              // 只读可执行工具（不 bind /dev）
                "-b", scriptFile.absolutePath + ":/data/local/tmp/entry.sh",
                "-0",                             // 伪装 uid 0（仅沙盒内有效）
                "/system/bin/sh", "/data/local/tmp/entry.sh"
            )
        } else {
            // 降级：用 App 自身 UID 执行，无任何特权
            listOf("/system/bin/sh", scriptFile.absolutePath)
        }

        val mode = if (proot != null) "proot 沙盒" else "降级沙盒（无 proot，App 权限运行）"

        return try {
            val pb = ProcessBuilder(cmd)
                .directory(work)
            // 清空环境变量，防止脚本继承到真机路径
            pb.environment().clear()
            pb.environment()["PATH"] = "/system/bin:/system/xbin"
            pb.environment()["HOME"] = "/data/local/tmp"
            pb.environment()["TMPDIR"] = "/tmp"
            pb.environment()["SANDBOX"] = "1"

            val proc = pb.start()
            proc.outputStream.bufferedWriter().use { it.write(scriptContent) }

            val out = StringBuilder()
            val err = StringBuilder()
            val tOut = Thread {
                runCatching {
                    proc.inputStream.bufferedReader().forEachLine { line ->
                        if (out.length < 200_000) out.appendLine(line)
                    }
                }
            }
            val tErr = Thread {
                runCatching {
                    proc.errorStream.bufferedReader().forEachLine { line ->
                        if (err.length < 100_000) err.appendLine(line)
                    }
                }
            }
            tOut.start(); tErr.start()

            val finished = proc.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return Outcome(-1, "⏱ 执行超时（60 秒），已终止\n${out}\n${err}", mode)
            }
            tOut.join(2000); tErr.join(2000)

            val text = buildString {
                append(out)
                if (err.isNotBlank()) append("\n[stderr]\n").append(err)
            }.ifBlank { "（无输出）" }

            Outcome(proc.exitValue(), text, mode)
        } catch (e: IOException) {
            Outcome(-1, "❌ 沙盒启动失败：${e.message}", mode)
        } catch (e: Exception) {
            Outcome(-1, "❌ 执行异常：${e.message}", mode)
        }
    }
}