package com.kernelsu.mgr.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * 脚本条目。
 *
 * trusted 语义：
 *   - true  : 已在"信任区"。扫描通过过的脚本会进入信任区，再次执行时跳过扫描，直接运行。
 *   - false : 未受信任，每次执行前都要走安全扫描。
 *
 * contentHash 用于校验：如果脚本文件内容被改动过，hash 变化则信任自动失效。
 */
data class ScriptItem(
    val id: String,
    val uriString: String,
    var displayName: String,
    var trusted: Boolean = false,
    var contentHash: String = "",
    /** 上次扫描结论：SAFE / WARN / DANGER / "" */
    var lastScan: String = ""
)

object ScriptStore {
    private const val PREF = "script_store"
    private const val KEY = "scripts"

    fun load(context: Context): MutableList<ScriptItem> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, null) ?: return mutableListOf()
        val list = mutableListOf<ScriptItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ScriptItem(
                        id = o.getString("id"),
                        uriString = o.getString("uri"),
                        displayName = o.getString("name"),
                        trusted = o.optBoolean("trusted", false),
                        contentHash = o.optString("hash", ""),
                        lastScan = o.optString("scan", "")
                    )
                )
            }
        } catch (_: Exception) { }
        return list
    }

    fun save(context: Context, list: List<ScriptItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("uri", it.uriString)
                put("name", it.displayName)
                put("trusted", it.trusted)
                put("hash", it.contentHash)
                put("scan", it.lastScan)
            })
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    /** 从 Uri 读取脚本内容 */
    fun readScript(context: Context, uriString: String): String {
        return try {
            context.contentResolver.openInputStream(Uri.parse(uriString))
                ?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /** 内容指纹，用于判断信任是否还有效 */
    fun hash(content: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            md.digest(content.toByteArray()).joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            content.hashCode().toString()
        }
    }

    /** 保留 uri 的持久读取权限 */
    fun takePermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { }
    }

    /** 删除脚本文件本体（SAF 提供删除能力时） */
    fun deleteFile(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "content") {
                // DocumentsContract.deleteDocument 更可靠，但需要 tree 权限；
                // 这里退化为 contentResolver.delete
                context.contentResolver.delete(uri, null, null) > 0
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}