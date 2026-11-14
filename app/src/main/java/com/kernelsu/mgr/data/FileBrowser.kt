package com.kernelsu.mgr.data

import java.util.Locale

/**
 * 文件浏览数据模型与读取逻辑。
 *
 * 目标目录 /data/adb/JN 权限是 drwx------ (root only)，
 * 普通 App 读不到，必须通过 su 执行 ls 来获取列表。
 */
data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val modified: String,
    val permissions: String,
    val isHidden: Boolean = name.startsWith(".")
)

object FileBrowser {

    const val ROOT_PATH = "/data/adb/JN"

    /**
     * 通过 su 列出目录内容。
     * 用 `ls -l` 的稳定输出解析；文件名可能含空格/中文，用固定分隔符方案。
     */
    fun listDir(dirPath: String): Result<List<FileEntry>> {
        val cmd = "ls -la " + shellQuote(dirPath) + " 2>/dev/null"
        val raw = runRoot(cmd)
        if (raw.isBlank()) {
            return Result.failure(Exception("无法读取目录（权限不足或路径不存在）"))
        }

        val entries = mutableListOf<FileEntry>()
        raw.lines().forEach { line ->
            val entry = parseLsLine(line, dirPath) ?: return@forEach
            if (entry.name == "." || entry.name == "..") return@forEach
            entries.add(entry)
        }

        // 排序：文件夹在前，然后按名称
        return Result.success(
            entries.sortedWith(
                compareByDescending<FileEntry> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
        )
    }

    /**
     * 解析 `ls -l` 单行。
     * 实际格式（8 段）：
     *   权限 链接数 属主 属组 大小 日期 时间 名称
     * 例：drwxr-xr-x  9 root root 3440 2026-10-24 22:43 内核辅助
     */
    private fun parseLsLine(line: String, parent: String): FileEntry? {
        if (line.isBlank()) return null
        val trimmed = line.trimStart()
        if (trimmed.length < 10) return null
        // 文件/目录行必须以权限字符开头；"total 31" 这类会被过滤掉
        if (trimmed[0] !in "dlbscp-") return null

        // 严格 8 段正则，最后一段用 (.+) 吃下含空格/中文的名称
        val re = Regex(
            """^([dlbcps-][rwxsStT-]{9}[.+@]?)\s+(\d+)\s+(\S+)\s+(\S+)\s+(\d+)\s+(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})\s+(.+)$"""
        )
        val m = re.find(trimmed)
        if (m != null) {
            val perms = m.groupValues[1]
            val size = m.groupValues[5].toLongOrNull() ?: 0L
            val date = m.groupValues[6] + " " + m.groupValues[7]
            val name = m.groupValues[8]
            return build(perms, size, date, name, parent)
        }

        // 退化方案：按空白切 8 段（limit=8），最后一段保留原样
        val parts = trimmed.split(Regex("\\s+"), limit = 8)
        if (parts.size < 8) return null
        val perms = parts[0]
        if (perms.isEmpty() || perms[0] !in "dlbscp-") return null
        val size = parts.getOrNull(4)?.toLongOrNull() ?: 0L
        val date = (parts.getOrNull(5) ?: "") + " " + (parts.getOrNull(6) ?: "")
        val name = parts.getOrNull(7) ?: return null
        return build(perms, size, date, name, parent)
    }

    private fun build(
        perms: String,
        size: Long,
        date: String,
        name: String,
        parent: String
    ): FileEntry {
        val isDir = perms.startsWith("d")
        val path = if (parent.endsWith("/")) "$parent$name" else "$parent/$name"
        return FileEntry(
            name = name,
            path = path,
            isDirectory = isDir,
            size = size,
            modified = date,
            permissions = perms
        )
    }

    /** 读取文件内容（用于查看文本文件），带大小上限 */
    fun readFile(path: String, maxBytes: Int = 200_000): String {
        val cmd = "head -c $maxBytes " + shellQuote(path) + " 2>/dev/null"
        return runRoot(cmd)
    }

    /** 判断是否像二进制文件 */
    fun looksBinary(content: String): Boolean {
        if (content.isEmpty()) return false
        val sample = content.take(2000)
        val ctrl = sample.count { it.code < 9 || (it.code in 14..31) }
        return ctrl > sample.length / 10
    }

    private fun shellQuote(s: String): String =
        "'" + s.replace("'", "'\\''") + "'"

    /** 通过 su 执行命令并收集输出 */
    private fun runRoot(cmd: String): String {
        return try {
            val p = ProcessBuilder("su", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val sb = StringBuilder()
            val t = Thread {
                runCatching {
                    p.inputStream.bufferedReader().forEachLine { sb.appendLine(it) }
                }
            }
            t.start()
            val ok = p.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)
            if (!ok) { p.destroyForcibly() }
            t.join(1500)
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    /** 大小格式化 */
    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        return String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }
}