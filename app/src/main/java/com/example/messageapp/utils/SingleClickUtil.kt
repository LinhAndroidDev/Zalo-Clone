package com.example.messageapp.utils

import android.view.View
import com.example.messageapp.R

private var View.lastClickTime: Long
    get() = getTag(R.id.last_click_time) as? Long ?: 0
    set(value) {
        setTag(R.id.last_click_time, value)
    }

fun View.setOnSingleClickListener(interval: Long = 500, onClick: (View) -> Unit) {
    setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= interval) {
            lastClickTime = currentTime
            onClick(it)
        }
    }
}
