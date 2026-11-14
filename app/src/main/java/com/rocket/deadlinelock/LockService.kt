package com.rocket.deadlinelock

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder

class LockService : Service() {
    private lateinit var overlay: LockOverlay

    override fun onCreate() {
        super.onCreate()
        overlay = LockOverlay(this)
        startForeground(1, buildNotification())
        overlay.show()
    }

    private fun buildNotification(): Notification {
        val ch = "deadline_lock"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(ch, "Deadline Lock", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return Notification.Builder(this, ch)
            .setContentTitle("Deadline Lock активен")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onDestroy() { overlay.hide(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
