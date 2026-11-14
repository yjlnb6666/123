package com.kernelsu.mgr.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelsu.mgr.root.DeviceInfoProvider
import com.kernelsu.mgr.ui.theme.AccentBlue
import com.kernelsu.mgr.ui.theme.GlassCard
import com.kernelsu.mgr.ui.theme.StatusGreenBg
import com.kernelsu.mgr.ui.theme.StatusGreenIcon
import com.kernelsu.mgr.ui.theme.StatusGreenText
import com.kernelsu.mgr.ui.theme.TextSub

@Composable
fun DeviceScreen(hasRoot: Boolean) {
    val kernel = remember { DeviceInfoProvider.kernelVersion() }
    val model = remember { DeviceInfoProvider.deviceModel() }
    val fingerprint = remember { DeviceInfoProvider.systemFingerprint() }
    val selinux = remember { DeviceInfoProvider.selinux() }
    val seccomp = remember { DeviceInfoProvider.seccomp() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text("少羽牛逼", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(16.dp))

        // 状态卡片（KSU 风格）
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (hasRoot) StatusGreenBg else Color(0xFFFFE0E0)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (hasRoot) "工作中" else "未授权",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasRoot) StatusGreenText else Color(0xFFB71C1C)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("版本：32680-5", fontSize = 14.sp, color = Color(0xFF444444))
                    Spacer(Modifier.height(6.dp))
                    Text("LKM", fontSize = 14.sp, color = Color(0xFF444444))
                }
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (hasRoot) StatusGreenIcon else Color(0xFFE57373),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        InfoItem("管理器版本", DeviceInfoProvider.managerVersion(), Icons.Filled.Numbers)
        InfoItem("内核版本", kernel, Icons.Filled.Memory)
        InfoItem("设备型号", model, Icons.Filled.Smartphone)
        InfoItem("系统指纹", fingerprint, Icons.Filled.Fingerprint)
        InfoItem("SELinux 状态", selinux, Icons.Filled.Security)
        InfoItem("Seccomp 状态", seccomp, Icons.Filled.FilterList)
    }
}

@Composable
private fun InfoItem(title: String, value: String, icon: ImageVector) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(value, fontSize = 13.sp, color = TextSub)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}