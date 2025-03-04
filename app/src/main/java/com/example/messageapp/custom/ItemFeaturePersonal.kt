package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemFeaturePersonalBinding

class ItemFeaturePersonal @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        val v = ItemFeaturePersonalBinding.inflate(LayoutInflater.from(context))
        addView(v.root)
        val array = context?.theme?.obtainStyledAttributes(attrs, R.styleable.ItemFeaturePersonal, 0, 0)
        v.title.text = array?.getText(R.styleable.ItemFeaturePersonal_title_feature)
        val iconId = array?.getResourceId(R.styleable.ItemFeaturePersonal_icon_feature, 0)
        v.icon.setImageResource(iconId ?: 0)
        val colorIconId = array?.getColor(R.styleable.ItemFeaturePersonal_color_icon_feature, 0) ?: 0
        v.icon.setColorFilter(colorIconId)
    }
}