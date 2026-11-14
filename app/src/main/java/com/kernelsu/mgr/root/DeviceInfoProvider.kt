package com.kernelsu.mgr.root

import android.os.Build

object DeviceInfoProvider {
    fun managerVersion() = "v1.0.0 (32680-5)"
    fun kernelVersion(): String = System.getProperty("os.version") ?: "unknown"
    fun deviceModel(): String = "${Build.BRAND.uppercase()} ${Build.MODEL}"
    fun systemFingerprint(): String = Build.FINGERPRINT
    fun selinux(): String = RootManager.getSelinuxStatus()
    fun seccomp(): String = RootManager.getSeccompStatus()
}