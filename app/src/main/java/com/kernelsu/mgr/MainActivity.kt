package com.kernelsu.mgr

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
        setContent {
            AppRoot(this)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRoot(activity: Activity) {
    val context = LocalContext.current
    var loggedIn by remember { mutableStateOf(false) }
    var hasRoot by remember { mutableStateOf<Boolean?>(null) }
    var showNoRootDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            val root = withContext(Dispatchers.IO) { RootManager.isRooted() }
            hasRoot = root
            showNoRootDialog = !root
        }
    }

    if (!loggedIn) {
        BlueGlassBackground {
            LoginScreen(onLogin = { loggedIn = true })
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
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1
            ) { page ->
                Box(Modifier.fillMaxSize().padding(bottom = 84.dp)) {
                    when (page) {
                        0 -> DeviceScreen(hasRoot == true)
                        1 -> ScriptScreen(hasRoot == true)
                        else -> AccountScreen(
                            account = "donk",
                            onExit = { activity.finish() },
                            onSettingsChanged = { AppSettings.save(context) }
                        )
                    }
                }
            }

            // 底部渐变遮罩，让浮起的玻璃条有过渡层次
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.80f to Color.Transparent,
                            1f to AppSettings.bgColors.last().copy(alpha = 0.80f)
                        )
                    )
            )

            // 液态玻璃底部导航（浮在内容之上）
            LiquidGlassNavBar(
                items = navItems,
                selectedIndex = pagerState.currentPage,
                onSelect = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
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