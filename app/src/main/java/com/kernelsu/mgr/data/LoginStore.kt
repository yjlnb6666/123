package com.kernelsu.mgr.data

import android.content.Context

/**
 * 登录凭据与自动登录设置的持久化。
 *
 * - saveAccount  = 记住账号（保存用户名，下次自动填入）
 * - savePassword = 记住密码（保存密码，下次自动填入）
 * - autoLogin    = 自动登录（两项都勾选才有意义，启动时直接进主界面）
 *
 * 说明：这是纯离线本机应用，凭据存放在应用私有的 SharedPreferences 里，
 * 其他应用无法读取。但不做加密，root 环境下可被查看，属于预期内的取舍。
 */
object LoginStore {

    private const val PREF = "login_store"

    var savedAccount: String = ""
        private set
    var savedPassword: String = ""
        private set
    var saveAccount: Boolean = false
        private set
    var savePassword: Boolean = false
        private set
    var autoLogin: Boolean = false
        private set

    /** 是否具备自动登录条件：自动登录开启 + 账号密码都记住了 + 凭据非空 */
    val canAutoLogin: Boolean
        get() = autoLogin && savedAccount.isNotBlank() && savedPassword.isNotBlank()

    fun load(context: Context) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        savedAccount = sp.getString("account", "") ?: ""
        savedPassword = sp.getString("password", "") ?: ""
        saveAccount = sp.getBoolean("saveAccount", false)
        savePassword = sp.getBoolean("savePassword", false)
        autoLogin = sp.getBoolean("autoLogin", false)

        // 一致性修正：没勾"记住账号"就不该保留账号，反之亦然
        if (!saveAccount) savedAccount = ""
        if (!savePassword) savedPassword = ""
        // 自动登录必须有账号+密码
        if (savedAccount.isBlank() || savedPassword.isBlank()) autoLogin = false
    }

    fun save(
        context: Context,
        account: String,
        password: String,
        rememberAccount: Boolean,
        rememberPassword: Boolean,
        auto: Boolean
    ) {
        saveAccount = rememberAccount
        savePassword = rememberPassword
        savedAccount = if (rememberAccount) account else ""
        savedPassword = if (rememberPassword) password else ""
        // 只有两项都记住时才允许自动登录
        autoLogin = auto && rememberAccount && rememberPassword

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().apply {
            putString("account", savedAccount)
            putString("password", savedPassword)
            putBoolean("saveAccount", saveAccount)
            putBoolean("savePassword", savePassword)
            putBoolean("autoLogin", autoLogin)
            apply()
        }
    }

    /** 退出程序时清理（不清理记住的凭据，只清除本次自动登录） */
    fun clearAutoLogin(context: Context) {
        autoLogin = false
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean("autoLogin", false).apply()
    }
}