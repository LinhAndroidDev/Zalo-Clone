package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import com.example.messageapp.R

class RadioCommon : ImageView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var isChecked: Boolean = false

    init {
        setImageResource(R.drawable.ic_un_checked)
    }

    fun setChecked() {
        isChecked = true
        setImageResource(R.drawable.ic_checked)
    }

    fun setUnChecked() {
        isChecked = false
        setImageResource(R.drawable.ic_un_checked)
    }

}