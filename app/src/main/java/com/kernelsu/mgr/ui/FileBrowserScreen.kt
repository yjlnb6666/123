package com.kernelsu.mgr.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelsu.mgr.data.AppSettings
import com.kernelsu.mgr.data.FileBrowser
import com.kernelsu.mgr.data.FileEntry
import com.kernelsu.mgr.ui.theme.GlassCard
import com.kernelsu.mgr.ui.theme.StatusGreenIcon
import com.kernelsu.mgr.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 文件浏览界面：显示 /data/adb/JN 目录及子目录。
 *
 * 该目录是 root 私有（drwx------），普通权限读不到，
 * 所以所有读取都通过 su 执行 ls 完成。
 */
@Composable
fun FileBrowserScreen(hasRoot: Boolean) {
    val accent = AppSettings.accent
    val scope = rememberCoroutineScope()

    var currentPath by remember { mutableStateOf(FileBrowser.ROOT_PATH) }
    val entries = remember { mutableStateListOf<FileEntry>() }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(true) }

    var previewName by remember { mutableStateOf<String?>(null) }
    var previewText by remember { mutableStateOf("") }

    // 脚本执行确认 / 结果
    var pendingScript by remember { mutableStateOf<FileEntry?>(null) }
    var runningScript by remember { mutableStateOf(false) }
    var scriptResult by remember { mutableStateOf<String?>(null) }

    // APK 安装确认 / 结果
    var pendingApk by remember { mutableStateOf<FileEntry?>(null) }
    var installing by remember { mutableStateOf(false) }
    var installResult by remember { mutableStateOf<String?>(null) }

    fun load(path: String) {
        if (!hasRoot) {
            errorMsg = "需要 Root 权限才能访问 " + FileBrowser.ROOT_PATH
            entries.clear()
            return
        }
        loading = true
        errorMsg = ""
        scope.launch {
            val res = withContext(Dispatchers.IO) { FileBrowser.listDir(path) }
            res.onSuccess {
                entries.clear()
                entries.addAll(it)
                currentPath = path
            }.onFailure {
                errorMsg = it.message ?: "读取失败"
                entries.clear()
            }
            loading = false
        }
    }

    LaunchedEffect(hasRoot) {
        load(FileBrowser.ROOT_PATH)
    }

    val visible = if (showHidden) entries.toList() else entries.filterNot { it.isHidden }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("文件管理", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { load(currentPath) }) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = accent)
            }
            IconButton(onClick = { showHidden = !showHidden }) {
                Icon(
                    Icons.Filled.VisibilityOff,
                    contentDescription = "隐藏文件",
                    tint = if (showHidden) accent else Color(0xFF6B7A90)
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x22FFFFFF))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPath != FileBrowser.ROOT_PATH) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "返回上级",
                    tint = accent,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            val parent = currentPath.substringBeforeLast('/', "")
                            load(if (parent.isBlank() || parent == "/data/adb") FileBrowser.ROOT_PATH else parent)
                        }
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                currentPath,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFBFD2EA),
                modifier = Modifier.weight(1f)
            )
            Text("${visible.size} 项", fontSize = 11.sp, color = TextMuted)
        }

        Spacer(Modifier.height(10.dp))

        when {
            loading -> {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(32.dp))
                }
            }
            errorMsg.isNotEmpty() -> {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("⚠ 无法读取", color = Color(0xFFFFB0A0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(errorMsg, color = TextMuted, fontSize = 12.sp)
                        if (!hasRoot) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "该目录属于 root，请先在 KernelSU 中授权本应用",
                                color = Color(0xFFFFD180),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            visible.isEmpty() -> {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF7F8CA8),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("目录为空", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visible, key = { it.path }) { entry ->
                        FileRow(
                            entry = entry,
                            accent = accent,
                            onClick = {
                                if (entry.isDirectory) {
                                    load(entry.path)
                                    return@FileRow
                                }
                                when (FileBrowser.kindOf(entry)) {
                                    FileBrowser.Kind.SCRIPT -> pendingScript = entry
                                    FileBrowser.Kind.APK -> pendingApk = entry
                                    FileBrowser.Kind.ZIP -> {
                                        previewName = "${entry.name}  (压缩包内容)"
                                        previewText = "读取中…"
                                        scope.launch {
                                            val r = withContext(Dispatchers.IO) {
                                                FileBrowser.listZip(entry.path)
                                            }
                                            previewText = r.getOrElse { "❌ ${it.message}" }
                                        }
                                    }
                                    else -> {
                                        previewName = entry.name
                                        previewText = "读取中…"
                                        scope.launch {
                                            val c = withContext(Dispatchers.IO) {
                                                FileBrowser.readFile(entry.path)
                                            }
                                            previewText = when {
                                                c.isEmpty() -> "（空文件或无法读取）"
                                                FileBrowser.looksBinary(c) -> "（二进制文件，不预览）"
                                                else -> c
                                            }
                                        }
                                    }
                                }
                            },
                            onRun = if (FileBrowser.kindOf(entry) == FileBrowser.Kind.SCRIPT)
                                ({ pendingScript = entry }) else null,
                            onInstall = if (FileBrowser.kindOf(entry) == FileBrowser.Kind.APK)
                                ({ pendingApk = entry }) else null
                        )
                    }
                }
            }
        }
    }

    // ===== 脚本执行确认 =====
    pendingScript?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!runningScript) pendingScript = null },
            title = { Text("执行脚本") },
            text = {
                Column {
                    Text(target.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(target.path, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "将以 root 权限执行此脚本。",
                        color = Color(0xFFFFD180),
                        fontSize = 12.sp
                    )
                    if (runningScript) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = accent,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("执行中…", color = accent, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !runningScript,
                    onClick = {
                        runningScript = true
                        scope.launch {
                            val r = withContext(Dispatchers.IO) {
                                FileBrowser.execScript(target.path)
                            }
                            runningScript = false
                            pendingScript = null
                            scriptResult = r.getOrElse { "❌ ${it.message}" }
                        }
                    }
                ) { Text(if (runningScript) "请稍候" else "执行", color = Color(0xFF7CE38B)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !runningScript,
                    onClick = { pendingScript = null }
                ) { Text("取消") }
            }
        )
    }

    // ===== 脚本执行结果 =====
    scriptResult?.let { result ->
        AlertDialog(
            onDismissRequest = { scriptResult = null },
            title = { Text("执行结果") },
            text = {
                Text(
                    result,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { scriptResult = null }) { Text("关闭") }
            }
        )
    }

    // ===== APK 安装确认 =====
    pendingApk?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!installing) pendingApk = null },
            title = { Text("安装应用") },
            text = {
                Column {
                    Text(target.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        FileBrowser.formatSize(target.size),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "将通过 root 静默安装（-r 覆盖 -d 允许降级）。",
                        color = Color(0xFF7CD4E3),
                        fontSize = 12.sp
                    )
                    if (installing) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = accent,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("安装中…", color = accent, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !installing,
                    onClick = {
                        installing = true
                        scope.launch {
                            val r = withContext(Dispatchers.IO) {
                                FileBrowser.installApk(target.path)
                            }
                            installing = false
                            pendingApk = null
                            installResult = r.getOrElse { "❌ ${it.message}" }
                        }
                    }
                ) { Text(if (installing) "请稍候" else "安装", color = Color(0xFF7CD4E3)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !installing,
                    onClick = { pendingApk = null }
                ) { Text("取消") }
            }
        )
    }

    // ===== APK 安装结果 =====
    installResult?.let { result ->
        AlertDialog(
            onDismissRequest = { installResult = null },
            title = { Text("安装结果") },
            text = {
                Text(
                    result,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { installResult = null }) { Text("关闭") }
            }
        )
    }

    previewName?.let { name ->
        AlertDialog(
            onDismissRequest = { previewName = null },
            title = { Text(name, fontSize = 15.sp) },
            text = {
                Text(
                    previewText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { previewName = null }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun FileRow(
    entry: FileEntry,
    accent: Color,
    onClick: () -> Unit,
    onRun: (() -> Unit)? = null,
    onInstall: (() -> Unit)? = null
) {
    val kind = com.kernelsu.mgr.data.FileBrowser.kindOf(entry)
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when {
                    entry.isDirectory -> Icons.Filled.Folder
                    kind == com.kernelsu.mgr.data.FileBrowser.Kind.SCRIPT -> Icons.Filled.PlayArrow
                    kind == com.kernelsu.mgr.data.FileBrowser.Kind.APK -> Icons.Filled.Android
                    kind == com.kernelsu.mgr.data.FileBrowser.Kind.ZIP -> Icons.Filled.FolderZip
                    else -> Icons.Filled.Description
                },
                contentDescription = null,
                tint = when {
                    entry.isDirectory -> accent
                    kind == com.kernelsu.mgr.data.FileBrowser.Kind.SCRIPT -> Color(0xFF7CE38B)
                    kind == com.kernelsu.mgr.data.FileBrowser.Kind.APK -> Color(0xFF7CD4E3)
                    kind == com.kernelsu.mgr.data.FileBrowser.Kind.ZIP -> Color(0xFFE3C77C)
                    else -> StatusGreenIcon
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.name,
                    color = if (entry.isHidden) Color(0xFF8B9AB0) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (entry.isDirectory) FontWeight.SemiBold else FontWeight.Normal
                )
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        entry.permissions,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (entry.permissions.startsWith("d")) Color(0xFF8AB4F8) else Color(0xFF9AA7BC)
                    )
                    if (!entry.isDirectory) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            FileBrowser.formatSize(entry.size),
                            fontSize = 10.sp,
                            color = Color(0xFF9AA7BC)
                        )
                    }
                }
            }

            // 脚本：执行按钮
            if (onRun != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x447CE38B))
                        .clickable { onRun() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("执行", color = Color(0xFFB6FFC4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            // APK：安装按钮
            if (onInstall != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x447CD4E3))
                        .clickable { onInstall() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("安装", color = Color(0xFFB6EEFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}