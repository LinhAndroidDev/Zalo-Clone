package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemViewProfileBinding

class ItemViewProfile @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val v = ItemViewProfileBinding.inflate(LayoutInflater.from(context))

    init {
        v.root.layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(v.root)
        val array =
            context?.theme?.obtainStyledAttributes(attrs, R.styleable.ItemViewProfile, 0, 0)
        val iconId = array?.getResourceId(R.styleable.ItemViewProfile_icon, 0)
        iconId?.let { v.iconView.setImageResource(it) }
        v.titleView.text = array?.getText(R.styleable.ItemViewProfile_title)
        val detail = array?.getText(R.styleable.ItemViewProfile_detail)
        v.detailView.isVisible = detail != null
        v.detailView.text = detail
        v.iconAccording.isVisible = array?.getBoolean(R.styleable.ItemViewProfile_show_according, true) ?: true
        v.divider.isVisible = array?.getBoolean(R.styleable.ItemViewProfile_show_divider, false) ?: false

        val type = array?.getInt(R.styleable.ItemViewProfile_end_icon, 0)
        when (type) {
            1 -> {
                hideAllIconEnd()
                v.warning.isVisible = true
            }

            2 -> {
                hideAllIconEnd()
                v.contact.isVisible = true
            }

            else -> {
                hideAllIconEnd()
            }
        }
    }

    private fun hideAllIconEnd() {
        v.warning.isVisible = false
        v.contact.isVisible = false
    }
}