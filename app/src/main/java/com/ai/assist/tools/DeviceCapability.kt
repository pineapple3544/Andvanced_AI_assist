package com.ai.assist.tools

import android.app.ActivityManager
import android.content.Context
import android.os.Build

class DeviceCapability(private val context: Context) {
    fun describe(): String {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val totalGb = memory.totalMem / 1024.0 / 1024.0 / 1024.0
        return buildString {
            append("Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT}. ")
            append("Device ${Build.MANUFACTURER} ${Build.MODEL}. ")
            append("RAM %.1f GB. ".format(totalGb))
            append("ABIs ${Build.SUPPORTED_ABIS.joinToString()}. ")
            append("Local model mode is recommended only after a successful LiteRT-LM model load.")
        }
    }
}
