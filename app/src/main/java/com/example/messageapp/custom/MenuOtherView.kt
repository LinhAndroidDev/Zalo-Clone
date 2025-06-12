package com.example.messageapp.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.example.messageapp.databinding.MenuOtherViewBinding
import com.example.messageapp.utils.AnimatorUtils

@SuppressLint("ClickableViewAccessibility")
class MenuOtherView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var binding: MenuOtherViewBinding? = null

    init {
        binding = MenuOtherViewBinding.inflate(LayoutInflater.from(context))
        binding?.root?.layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        addView(binding?.root)

        binding?.bgView?.setOnTouchListener { _, _ ->
            binding?.bgView?.isVisible = false
            true
        }
    }

    fun showView(isVisible: Boolean) {
        if (isVisible) {
            binding?.viewMenu?.let { AnimatorUtils.expandView(it) }
        } else {
            binding?.viewMenu?.let { AnimatorUtils.collapseView(it) }
        }
        binding?.bgView?.isVisible = isVisible
    }
}