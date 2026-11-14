package com.rocket.deadlinelock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#050000"))
            setPadding(50, 50, 50, 50)
        }

        val title = TextView(this).apply {
            text = "DEADLINE LOCK"
            setTextColor(Color.WHITE)
            textSize = 32f
            setShadowLayer(15f, 0f, 0f, Color.RED)
            gravity = Gravity.CENTER
        }

        val desc = TextView(this).apply {
            text = "Приложение заблокирует экран на 5 минут.\nУ вас будет 5 попыток ввести код.\nПри неудаче телефон уйдет в черный экран на 30 минут.\n\nВы готовы?"
            setTextColor(Color.LTGRAY)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }

        val btnActivate = Button(this).apply {
            text = "АКТИВИРОВАТЬ ЗАЩИТУ"
            setTextColor(Color.WHITE)
            
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#AA0000"))
                cornerRadius = 20f
            }
            background = bg
            setPadding(30, 30, 30, 30)

            setOnClickListener {
                requestPermissionsAndStart()
            }
        }

        root.addView(title)
        root.addView(desc)
        root.addView(btnActivate)

        setContentView(root)
    }

    private fun requestPermissionsAndStart() {
        // 1. Оверлей
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            return // После выдачи разрешения пользователь вернется и нажмет кнопку снова
        }

        // 2. Уведомления
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // 3. Батарея
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")))
            }
        }

        // 4. Device Admin
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(this, DeviceAdminReceiverImpl::class.java)
        if (!dpm.isAdminActive(admin)) {
            startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Требуется для сброса")
            })
        }

        // Всё готово — запускаем!
        ContextCompat.startForegroundService(this, Intent(this, LockService::class.java))
        finish()
    }
}
