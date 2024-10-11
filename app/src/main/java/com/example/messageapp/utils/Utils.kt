package com.example.messageapp.utils

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

fun EditText.showKeyboard() {
    this.isFocusable = true
    this.requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.showViewAboveKeyBoard(rootView: View) {
    rootView.viewTreeObserver.addOnGlobalLayoutListener {
        val r = Rect()
        rootView.getWindowVisibleDisplayFrame(r)
        val screenHeight = rootView.height
        val keypadHeight = screenHeight - r.bottom

        if (keypadHeight > screenHeight * 0.15) {
            // Bàn phím đã xuất hiện
            this.translationY = -keypadHeight.toFloat() - this.height - 15 // Đẩy view lên
        } else {
            // Bàn phím đã ẩn
            this.translationY = 0f
        }
    }
}

fun Context.vibratePhone(duration: Long = 100) {
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Dành cho Android 8.0 trở lên
        val vibrationEffect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)
    } else {
        // Dành cho các phiên bản Android cũ hơn
        vibrator.vibrate(duration)
    }
}