package com.example.messageapp.utils

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible

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