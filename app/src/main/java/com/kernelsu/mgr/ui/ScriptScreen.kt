package com.kernelsu.mgr.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelsu.mgr.data.ScriptItem
import com.kernelsu.mgr.data.ScriptStore
import com.kernelsu.mgr.root.RootManager
import com.kernelsu.mgr.ui.theme.AccentBlue
import com.kernelsu.mgr.ui.theme.GlassCard
import com.kernelsu.mgr.ui.theme.StatusGreenIcon
import com.kernelsu.mgr.ui.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScriptScreen(hasRoot: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scripts by remember { mutableStateOf(ScriptStore.load(context).toList()) }
    var output by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ScriptItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showOutput by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val added = uris.map {
                ScriptStore.takePermission(context, it)
                val name = it.lastPathSegment?.substringAfterLast("/") ?: "脚本"
                ScriptItem(
                    id = it.toString().hashCode().toString(),
                    uriString = it.toString(),
                    displayName = name
                )
            }
            val merged = (scripts + added).distinctBy { it.uriString }
            scripts = merged
            ScriptStore.save(context, merged)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text("脚本管理", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text("共 ${scripts.size} 个脚本", fontSize = 13.sp, color = TextMuted)
        Spacer(Modifier.height(14.dp))

        Button(
            onClick = { pickLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("添加脚本（可多选）", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        if (scripts.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = Color(0xFF7F8CA8), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("还没有添加脚本", color = TextMuted, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(scripts, key = { it.id }) { item ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Code, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    item.uriString.substringAfterLast("/").take(30),
                                    color = Color(0xFF8FA3C0), fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = {
                                renameTarget = item
                                renameText = item.displayName
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                val updated = scripts.filterNot { it.id == item.id }
                                scripts = updated
                                ScriptStore.save(context, updated)
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF8888), modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                enabled = !running,
                                onClick = {
                                    if (!hasRoot) {
                                        output = "❌ 无 Root 权限，无法执行"
                                        showOutput = true
                                        return@IconButton
                                    }
                                    running = true
                                    showOutput = true
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            val content = ScriptStore.readScript(context, item.uriString)
                                            if (content.isBlank()) "❌ 脚本内容为空或无法读取"
                                            else RootManager.execAsRoot(content)
                                        }
                                        output = "▶ ${item.displayName}\n\n$result"
                                        running = false
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = StatusGreenIcon, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showOutput) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("输出", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(if (running) "执行中…" else "完成", color = if (running) AccentBlue else StatusGreenIcon, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showOutput = false; output = "" }) {
                    Text("关闭", color = TextMuted)
                }
            }
            GlassCard(Modifier.fillMaxWidth().height(180.dp)) {
                Text(
                    text = output.ifBlank { "（暂无输出）" },
                    color = Color(0xFFC8FFC8),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                )
            }
        }
    }

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
                    val updated = scripts.map {
                        if (it.id == target.id) it.copy(displayName = renameText.ifBlank { it.displayName }) else it
                    }
                    scripts = updated
                    ScriptStore.save(context, updated)
                    renameTarget = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            }
        )
    }
}