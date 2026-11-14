package com.kernelsu.mgr.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class ScriptItem(
    val id: String,
    val uriString: String,
    var displayName: String
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
                        displayName = o.getString("name")
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

    /** 保留 uri 的持久读取权限 */
    fun takePermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { }
    }
}