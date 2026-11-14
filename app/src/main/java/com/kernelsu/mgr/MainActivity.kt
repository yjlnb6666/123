package com.kernelsu.mgr

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kernelsu.mgr.root.RootManager
import com.kernelsu.mgr.ui.AccountScreen
import com.kernelsu.mgr.ui.DeviceScreen
import com.kernelsu.mgr.ui.LoginScreen
import com.kernelsu.mgr.ui.ScriptScreen
import com.kernelsu.mgr.ui.theme.AccentBlue
import com.kernelsu.mgr.ui.theme.BlueGlassBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot(this)
        }
    }
}

@Composable
fun AppRoot(activity: Activity) {
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
        LoginScreen(onLogin = { loggedIn = true })
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "device"

    val items = listOf(
        Triple("device", "设备", Icons.Filled.Home),
        Triple("script", "脚本", Icons.Filled.Terminal),
        Triple("account", "账户", Icons.Filled.Person)
    )

    BlueGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color(0xCC0A0F2C)) {
                    items.forEach { (route, label, icon) ->
                        val selected = currentRoute == route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (selected) AccentBlue else Color(0xFF7F8CA8)
                                )
                            },
                            label = {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    color = if (selected) AccentBlue else Color(0xFF7F8CA8)
                                )
                            }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "device",
                modifier = Modifier.padding(padding)
            ) {
                composable("device") { DeviceScreen(hasRoot == true) }
                composable("script") { ScriptScreen(hasRoot == true) }
                composable("account") { AccountScreen("donk", onExit = { activity.finish() }) }
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
}