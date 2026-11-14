package com.rocket.deadlinelock

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout

class BlackoutOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var view: FrameLayout? = null
    private var onFinish: (() -> Unit)? = null

    private val durationMs = 30 * 60 * 1000L

    fun start(cb: () -> Unit) {
        onFinish = cb
        if (view != null) return

        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            isFocusable = true
            setOnTouchListener { _, _ -> true }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        )
        params.gravity = Gravity.TOP or Gravity.START

        wm.addView(root, params)
        view = root
        handler.postDelayed({ finish() }, durationMs)
    }

    private fun finish() {
        handler.removeCallbacksAndMessages(null)
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
        onFinish?.invoke()
        onFinish = null
    }

    fun cancel() {
        handler.removeCallbacksAndMessages(null)
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
    }
}
