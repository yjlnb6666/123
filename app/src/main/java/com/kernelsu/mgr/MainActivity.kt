package com.kernelsu.mgr

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kernelsu.mgr.data.AppSettings
import com.kernelsu.mgr.data.LoginStore
import com.kernelsu.mgr.root.RootManager
import com.kernelsu.mgr.ui.AccountScreen
import com.kernelsu.mgr.ui.DeviceScreen
import com.kernelsu.mgr.ui.LiquidGlassNavBar
import com.kernelsu.mgr.ui.LoginScreen
import com.kernelsu.mgr.ui.NavItem
import com.kernelsu.mgr.ui.ScriptScreen
import com.kernelsu.mgr.ui.theme.BlueGlassBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.load(this)
        LoginStore.load(this)
        setContent {
            AppRoot(this)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRoot(activity: Activity) {
    val context = LocalContext.current
    // 启动即判断：满足自动登录条件就直接算已登录，避免登录页闪现
    var loggedIn by remember { mutableStateOf(LoginStore.canAutoLogin) }
    var hasRoot by remember { mutableStateOf<Boolean?>(null) }
    var showNoRootDialog by remember { mutableStateOf(false) }
    var rootRetry by remember { mutableStateOf(0) }

    LaunchedEffect(loggedIn, rootRetry) {
        if (loggedIn) {
            // 首次检测失败时自动再试一次（KSU 授权弹窗有时需要时间）
            var root = withContext(Dispatchers.IO) { RootManager.isRooted() }
            if (!root) {
                kotlinx.coroutines.delay(1200)
                root = withContext(Dispatchers.IO) { RootManager.isRooted() }
            }
            hasRoot = root
            // 只在首次进入时弹窗，手动重试不重复打扰
            if (rootRetry == 0) showNoRootDialog = !root
        }
    }

    if (!loggedIn) {
        BlueGlassBackground {
            LoginScreen(
                onLogin = { loggedIn = true },
                onAutoLogin = { loggedIn = true }
            )
        }
        return
    }

    val navItems = listOf(
        NavItem("设备", Icons.Filled.Home),
        NavItem("脚本", Icons.Filled.Terminal),
        NavItem("账户", Icons.Filled.Person)
    )

    val pagerState = rememberPagerState(initialPage = 0) { navItems.size }
    val scope = rememberCoroutineScope()

    BlueGlassBackground {
        Box(Modifier.fillMaxSize()) {
            // 内容区：手指左右滑动切页
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp
            ) { page ->
                Box(Modifier.fillMaxSize().padding(bottom = 84.dp)) {
                    when (page) {
                        0 -> DeviceScreen(
                            hasRoot = hasRoot == true,
                            onRetryRoot = { rootRetry++ }
                        )
                        1 -> ScriptScreen(hasRoot == true)
                        else -> AccountScreen(
                            account = LoginStore.savedAccount.ifBlank { "donk" },
                            onExit = {
                                // 退出时清掉自动登录 → 下次启动回到登录页（记住的账号密码仍保留）
                                LoginStore.clearAutoLogin(context)
                                activity.finish()
                            },
                            onSettingsChanged = { AppSettings.save(context) }
                        )
                    }
                }
            }

            // 底部渐变遮罩：纯视觉，放在导航条「下层」，且不覆盖导航条区域
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                AppSettings.bgColors.last().copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // 液态玻璃底部导航（浮在内容之上）
            LiquidGlassNavBar(
                items = navItems,
                selectedIndex = pagerState.currentPage,
                onSelect = { index ->
                    scope.launch {
                        if (pagerState.currentPage != index) {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }

    if (showNoRootDialog) {
        AlertDialog(
            onDismissRequest = { showNoRootDialog = false },
            title = { Text("权限提示") },
            text = { Text("未检测到 Root 权限，部分功能将无法使用。\n请先通过 KernelSU 授权本应用。") },
            confirmButton = {
                TextButton(onClick = { showNoRootDialog = false }) { Text("我知道了") }
            }
        )
    }
}