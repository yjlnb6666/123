package com.kernelsu.mgr.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelsu.mgr.data.AppSettings
import com.kernelsu.mgr.data.ScriptItem
import com.kernelsu.mgr.data.ScriptStore
import com.kernelsu.mgr.security.SandboxRunner
import com.kernelsu.mgr.security.ScriptScanner
import com.kernelsu.mgr.ui.theme.GlassCard
import com.kernelsu.mgr.ui.theme.StatusGreenIcon
import com.kernelsu.mgr.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 从 SAF Uri 查询真实文件名 */
private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ScriptScreen(hasRoot: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = AppSettings.accent
    var scripts by remember { mutableStateOf(ScriptStore.load(context).toList()) }
    var output by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ScriptItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showOutput by remember { mutableStateOf(false) }

    // 危险脚本拦截弹窗
    var dangerItem by remember { mutableStateOf<ScriptItem?>(null) }
    var dangerFindings by remember { mutableStateOf<List<ScriptScanner.Finding>>(emptyList()) }

    // 警告确认弹窗
    var warnItem by remember { mutableStateOf<ScriptItem?>(null) }
    var warnFindings by remember { mutableStateOf<List<ScriptScanner.Finding>>(emptyList()) }
    var warnContent by remember { mutableStateOf("") }

    fun persist(list: List<ScriptItem>) {
        scripts = list
        ScriptStore.save(context, list)
    }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        handlePickedUris(uris)
    }

    // 备用：单文件选择（某些设备的"多选"实现有问题时用这个）
    val pickSingleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) handlePickedUris(listOf(uri))
    }

    fun handlePickedUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val added = uris.mapNotNull { uri ->
            try {
                ScriptStore.takePermission(context, uri)
                val name = queryDisplayName(context, uri)
                    ?: uri.lastPathSegment?.substringAfterLast("/")?.substringAfterLast(":")
                    ?: "脚本"
                ScriptItem(
                    id = uri.toString().hashCode().toString(),
                    uriString = uri.toString(),
                    displayName = name
                )
            } catch (e: Exception) {
                null
            }
        }
        if (added.isEmpty()) {
            output = "❌ 无法读取所选文件，请换一个文件管理器再试"
            showOutput = true
            return
        }
        persist((scripts + added).distinctBy { it.uriString })
    }

    /** 真正执行（已通过安全校验） */
    fun execute(item: ScriptItem, content: String) {
        running = true
        showOutput = true
        output = "▶ ${item.displayName}\n正在执行…"
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                if (content.isBlank()) {
                    SandboxRunner.Outcome(-1, "❌ 脚本内容为空或无法读取", "-")
                } else if (hasRoot) {
                    // 有 root：已通过分区保护扫描，用真实 root 执行（su 可用）
                    SandboxRunner.runAsRealRoot(context, content)
                } else {
                    // 无 root：降级沙盒
                    SandboxRunner.run(context, content)
                }
            }
            output = buildString {
                append("▶ ").append(item.displayName).append("\n")
                append("环境：").append(outcome.mode).append("\n")
                append("退出码：").append(outcome.exitCode).append("\n")
                append("────────────────\n")
                append(outcome.output)
            }
            running = false
        }
    }

    /** 执行入口：先查信任区，再决定是否扫描 */
    fun requestRun(item: ScriptItem) {
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                ScriptStore.readScript(context, item.uriString)
            }
            if (content.isBlank()) {
                output = "❌ 无法读取脚本内容"
                showOutput = true
                return@launch
            }

            val newHash = ScriptStore.hash(content)

            // 已在信任区 → 直接执行（如果 hash 为空说明是手动加入的，补上 hash 即可）
            if (item.trusted) {
                if (item.contentHash.isEmpty()) {
                    persist(scripts.map {
                        if (it.id == item.id) it.copy(contentHash = newHash) else it
                    })
                }
                execute(item, content)
                return@launch
            }

            val result = withContext(Dispatchers.IO) { ScriptScanner.scan(content) }

            when (result.level) {
                ScriptScanner.Level.DANGER -> {
                    val updated = scripts.map {
                        if (it.id == item.id) it.copy(lastScan = "DANGER", trusted = false) else it
                    }
                    persist(updated)
                    dangerItem = item
                    dangerFindings = result.findings
                }
                ScriptScanner.Level.WARN -> {
                    warnItem = item
                    warnFindings = result.findings
                    warnContent = content
                }
                ScriptScanner.Level.SAFE -> {
                    // 扫描通过 → 自动进入信任区
                    val updated = scripts.map {
                        if (it.id == item.id) it.copy(
                            trusted = true,
                            contentHash = newHash,
                            lastScan = "SAFE"
                        ) else it
                    }
                    persist(updated)
                    execute(item, content)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text("脚本管理", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(
            "共 ${scripts.size} 个 · 信任 ${scripts.count { it.trusted }} 个 · ${if (hasRoot) "Root 模式（拦截分区操作）" else "沙盒模式（无 Root）"}",
            fontSize = 12.sp,
            color = TextMuted
        )
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                try {
                    pickLauncher.launch(arrayOf("*/*"))
                } catch (e: Exception) {
                    try {
                        pickSingleLauncher.launch(arrayOf("*/*"))
                    } catch (e2: Exception) {
                        output = "❌ 无法打开文件选择器：${e2.message}"
                        showOutput = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("添加脚本（可多选）", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        // 备用入口：逐个选文件，兼容多选不可用的设备
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .clickable { pickSingleLauncher.launch(arrayOf("*/*")) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "选不了文件？点这里逐个添加",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(12.dp))

        if (scripts.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        tint = Color(0xFF7F8CA8),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("还没有添加脚本", color = TextMuted, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scripts, key = { it.id }) { item ->
                    ScriptRow(
                        item = item,
                        accent = accent,
                        onRename = {
                            renameTarget = item
                            renameText = item.displayName
                        },
                        onDelete = { persist(scripts.filterNot { it.id == item.id }) },
                        onToggleTrust = {
                            val target = item
                            val newTrusted = !item.trusted
                            if (newTrusted) {
                                // 加入信任区：同步记录当前内容 hash
                                scope.launch {
                                    val content = withContext(Dispatchers.IO) {
                                        ScriptStore.readScript(context, target.uriString)
                                    }
                                    val h = ScriptStore.hash(content)
                                    persist(scripts.map {
                                        if (it.id == target.id)
                                            it.copy(trusted = true, contentHash = h, lastScan = "TRUSTED")
                                        else it
                                    })
                                }
                            } else {
                                persist(scripts.map {
                                    if (it.id == target.id)
                                        it.copy(trusted = false, contentHash = "")
                                    else it
                                })
                            }
                        },
                        onPlay = { requestRun(item) }
                    )
                }
            }
        }

        if (showOutput) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "输出",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (running) "执行中…" else "完成",
                    color = if (running) accent else StatusGreenIcon,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showOutput = false; output = "" }) {
                    Text("关闭", color = TextMuted)
                }
            }
            GlassCard(Modifier.fillMaxWidth().height(190.dp)) {
                Text(
                    text = output.ifBlank { "（暂无输出）" },
                    color = Color(0xFFC8FFC8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                )
            }
        }
    }

    // ===== 重命名 =====
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名脚本") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("显示名称") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    persist(scripts.map {
                        if (it.id == target.id) it.copy(
                            displayName = renameText.ifBlank { it.displayName }
                        ) else it
                    })
                    renameTarget = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            }
        )
    }

    // ===== 危险拦截 =====
    dangerItem?.let { target ->
        AlertDialog(
            onDismissRequest = { dangerItem = null },
            title = { Text("⛔ 已阻止执行") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "该脚本包含高危操作，已被拦截，不会执行。",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    dangerFindings.forEach { f ->
                        Text("• ${f.keyword}", color = Color(0xFFFF8A80), fontSize = 13.sp)
                        Text("   ${f.reason}", color = TextMuted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "建议删除该文件，避免误操作。",
                        color = Color(0xFFFFD180),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    ScriptStore.deleteFile(context, target.uriString)
                    persist(scripts.filterNot { it.id == target.id })
                    dangerItem = null
                }) { Text("删除该文件", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { dangerItem = null }) { Text("保留但永不执行") }
            }
        )
    }

    // ===== 警告确认 =====
    warnItem?.let { target ->
        AlertDialog(
            onDismissRequest = { warnItem = null },
            title = { Text("⚠ 发现可疑内容") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("该脚本包含以下可疑操作：", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    warnFindings.forEach { f ->
                        Text("• ${f.keyword}", color = Color(0xFFFFCC80), fontSize = 13.sp)
                        Text("   ${f.reason}", color = TextMuted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (hasRoot) "脚本将以真实 Root 执行（系统分区写入已被保护）。是否继续？"
                        else "脚本将在沙盒中运行，不会伤到真机。是否继续？",
                        color = Color(0xFFA5D6A7),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val content = warnContent
                    warnItem = null
                    execute(target, content)
                }) { Text("沙盒中执行") }
            },
            dismissButton = {
                TextButton(onClick = { warnItem = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ScriptRow(
    item: ScriptItem,
    accent: Color,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onToggleTrust: () -> Unit,
    onPlay: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Code,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.displayName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        item.uriString.substringAfterLast("/").take(28),
                        color = Color(0xFF8FA3C0),
                        fontSize = 10.sp
                    )
                }
                IconButton(onClick = onRename) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Color(0xFFFF8888),
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = StatusGreenIcon,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 信任状态条：仅展示状态，不再可点击（避免误触移除信任）
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (item.trusted) Color(0x3335C759) else Color(0x22FFFFFF)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            if (item.trusted)
                                listOf(Color(0x6635C759), Color(0x2235C759))
                            else
                                listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (item.trusted) Icons.Filled.VerifiedUser else Icons.Filled.Security,
                    contentDescription = null,
                    tint = if (item.trusted) StatusGreenIcon else TextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (item.trusted) "信任区 · 可直接执行" else "未信任 · 首次执行需扫描",
                    color = if (item.trusted) Color(0xFF8BE28B) else TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                if (item.lastScan == "DANGER") {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF8A80),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                // 独立的小按钮：手动切换信任状态
                Text(
                    if (item.trusted) "移出" else "信任",
                    color = if (item.trusted) Color(0xFFFFB0A0) else StatusGreenIcon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleTrust() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}