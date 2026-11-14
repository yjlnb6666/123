package com.kernelsu.mgr.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelsu.mgr.data.LoginStore
import com.kernelsu.mgr.ui.theme.AccentBlue
import com.kernelsu.mgr.ui.theme.BlueGlassBackground
import com.kernelsu.mgr.ui.theme.GlassBorder
import com.kernelsu.mgr.ui.theme.GlassCard
import com.kernelsu.mgr.ui.theme.TextMuted

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    /** 自动登录命中时回调，MainActivity 用它直接跳过登录 */
    onAutoLogin: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // 初始值来自已保存的凭据：记住账号/密码就预填
    var account by remember {
        mutableStateOf(if (LoginStore.saveAccount) LoginStore.savedAccount else "")
    }
    var password by remember {
        mutableStateOf(if (LoginStore.savePassword) LoginStore.savedPassword else "")
    }
    var rememberAccount by remember { mutableStateOf(LoginStore.saveAccount) }
    var rememberPassword by remember { mutableStateOf(LoginStore.savePassword) }
    var autoLogin by remember { mutableStateOf(LoginStore.autoLogin) }
    var error by remember { mutableStateOf("") }

    // 启动时若满足自动登录条件，直接进主界面
    LaunchedEffect(Unit) {
        if (LoginStore.canAutoLogin) {
            onAutoLogin(LoginStore.savedAccount)
        }
    }

    BlueGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("少羽牛逼", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(36.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Column {
                    OutlinedTextField(
                        value = account,
                        onValueChange = { account = it; error = "" },
                        label = { Text("账号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = AccentBlue,
                            unfocusedLabelColor = TextMuted
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = "" },
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = AccentBlue,
                            unfocusedLabelColor = TextMuted
                        )
                    )

                    // ===== 记住账号 / 记住密码 =====
                    Spacer(Modifier.height(14.dp))
                    CheckRow(
                        text = "保存用户账号",
                        checked = rememberAccount,
                        onCheckedChange = { checked ->
                            rememberAccount = checked
                            // 关掉记住账号时，自动登录也失效
                            if (!checked) autoLogin = false
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    CheckRow(
                        text = "保存密码",
                        checked = rememberPassword,
                        onCheckedChange = { checked ->
                            rememberPassword = checked
                            if (!checked) autoLogin = false
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    CheckRow(
                        text = "自动登录（无需再登录）",
                        checked = autoLogin,
                        // 两项都勾选后才能开自动登录
                        enabled = rememberAccount && rememberPassword,
                        onCheckedChange = { autoLogin = it }
                    )
                    if (!(rememberAccount && rememberPassword)) {
                        Text(
                            "勾选上面两项后可开启自动登录",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = Color(0xFFFF6B6B), fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (account == "donk" && password == "yjlnb666") {
                                // 登录成功：先落盘凭据与勾选状态
                                LoginStore.save(
                                    context = context,
                                    account = account,
                                    password = password,
                                    rememberAccount = rememberAccount,
                                    rememberPassword = rememberPassword,
                                    auto = autoLogin
                                )
                                onLogin()
                            } else {
                                error = "账号或密码错误"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("登 录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(
    text: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.size(32.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = AccentBlue,
                uncheckedColor = TextMuted,
                checkmarkColor = Color.White,
                disabledCheckedColor = Color(0x555B9BFF),
                disabledUncheckedColor = Color(0x447F8CA8)
            )
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            color = if (enabled) Color(0xFFDDE7F7) else Color(0x66DDE7F7),
            fontSize = 14.sp
        )
    }
}