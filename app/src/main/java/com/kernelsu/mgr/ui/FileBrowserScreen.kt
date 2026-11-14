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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
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
                                } else {
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
                        )
                    }
                }
            }
        }
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
    onClick: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                contentDescription = null,
                tint = if (entry.isDirectory) accent else StatusGreenIcon,
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
                        color = if (entry.isDirectory) Color(0xFF8AB4F8) else Color(0xFF9AA7BC)
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
        }
    }
}