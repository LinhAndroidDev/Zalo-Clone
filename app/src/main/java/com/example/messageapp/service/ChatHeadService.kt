package com.example.messageapp.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.example.messageapp.MainActivity
import com.example.messageapp.R

class ChatHeadService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var chatHeadView: View
    private var touchStartTime: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        // Khởi tạo WindowManager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Tạo layout chat head
        chatHeadView = LayoutInflater.from(this).inflate(R.layout.layout_chat_head, null)

        // Định nghĩa thông số hiển thị cho chat head
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Đặt vị trí xuất hiện ban đầu
        layoutParams.gravity = Gravity.TOP or Gravity.START
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        layoutParams.x = screenWidth - 500
        layoutParams.y = 100

        // Thêm chat head vào màn hình
        windowManager.addView(chatHeadView, layoutParams)

        val icBubbleChat = chatHeadView.findViewById<CardView>(R.id.icChatHead)
        animateChatHeadToEdge(layoutParams, icBubbleChat.width)

        // Xử lý sự kiện kéo thả bong bóng
        icBubbleChat.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            @SuppressLint("ClickableViewAccessibility", "ObjectAnimatorBinding")
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                event ?: return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        touchStartTime = SystemClock.elapsedRealtime()
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val touchDuration = SystemClock.elapsedRealtime() - touchStartTime
                        if (touchDuration < 200) { // Nếu là click
                            view?.performClick()
                        }
                        else {
                            animateChatHeadToEdge(layoutParams, icBubbleChat.width) // Gọi animation để di chuyển về mép màn hình
                        }
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        try {
                            layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                            layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager.updateViewLayout(chatHeadView, layoutParams)
                        } catch (e: IllegalArgumentException) {
                            Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_SHORT).show()
                            e.printStackTrace() // Tránh lỗi nếu view đã bị remove
                        }
                        return true
                    }
                }
                return false
            }
        })

        icBubbleChat.setOnClickListener {
            val packageManager = applicationContext.packageManager
            val intent: Intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            } ?: Intent(this, MainActivity::class.java)

            startActivity(intent)
            stopSelf()
        }

        chatHeadView.findViewById<View>(R.id.closeChatHeader).setOnClickListener {
            stopSelf()
        }
    }

    private fun animateChatHeadToEdge(layoutParams: WindowManager.LayoutParams, widthBubble: Int) {
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val startX = layoutParams.x
        val endX = if (startX > screenWidth / 2 - widthBubble / 2) {
            screenWidth - widthBubble / 2 // Mép phải
        } else {
            0 // Mép trái
        }

        // Tạo animation
        val animator = ValueAnimator.ofInt(startX, endX)
        animator.duration = 400 // Thời gian animation 400ms
        animator.addUpdateListener { valueAnimator ->
            try {
                layoutParams.x = valueAnimator.animatedValue as Int
                windowManager.updateViewLayout(chatHeadView, layoutParams)
            } catch (e: IllegalArgumentException) {
                Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_SHORT).show()
                animator.cancel() // Hủy animation nếu view không còn tồn tại
            }
        }
        animator.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::windowManager.isInitialized) {
            windowManager.removeView(chatHeadView)
        }
    }
}