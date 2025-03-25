package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.messageapp.R
import com.example.messageapp.databinding.LayoutDiviverCommonBinding

class DividerCommon @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        val binding = LayoutDiviverCommonBinding.inflate(LayoutInflater.from(context))
        binding.root.layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context?.resources?.getDimensionPixelSize(R.dimen.margin_10) ?: 0)
        addView(binding.root)
    }
}