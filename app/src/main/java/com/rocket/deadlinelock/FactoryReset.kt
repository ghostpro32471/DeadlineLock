package com.rocket.deadlinelock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log

object FactoryReset {
    const val REAL_WIPE = false

    fun wipe(context: Context): Boolean {
        if (!REAL_WIPE) {
            Log.w("DeadlineLock", "DEMO reset")
            return false
        }
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as DevicePolicyManager
        return try {
            val admin = ComponentName(context, DeviceAdminReceiverImpl::class.java)
            if (dpm.isDeviceOwnerApp(context.packageName) || dpm.isAdminActive(admin)) {
                @Suppress("DEPRECATION") dpm.wipeData(0)
                true
            } else false
        } catch (e: SecurityException) {
            Log.e("DeadlineLock", "wipeData denied: ${e.message}")
            false
        }
    }
}
