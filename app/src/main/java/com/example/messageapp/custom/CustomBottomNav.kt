package com.example.messageapp.custom

/**
 * Create By Nguyen Huu Linh on 16/09/2024
 * This class handle actions click item bottom navigation bar
 * Then send event to activity or fragment to update UI
 */

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.messageapp.R
import com.example.messageapp.databinding.CustomBottomNavBinding

data class ViewItem(
    val view: View?,
    val imgView: ImageView?,
    val textView: TextView?,
    val iconRes: Int
)

enum class ActionBottomBar(val rawValue: Int) {
    MESSAGE_VIEW(0),
    PHONE_BOOK_VIEW(1),
    DISCOVER_VIEW(2),
    DIARY_VIEW(3),
    PERSONAL_VIEW(4),
    CHAT_VIEW(5),
    SEARCH_VIEW(6);

    companion object {
        fun of(value: Int): ActionBottomBar {
            return entries.firstOrNull { it.rawValue == value }
                ?: throw IllegalArgumentException("Unknown Data")
        }
    }
}

class CustomBottomNav @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var binding: CustomBottomNavBinding? = null
    var actionBottomBar: ((ActionBottomBar) -> Unit)? = null

    init {
        binding = CustomBottomNavBinding.inflate(LayoutInflater.from(context))
        binding?.root?.layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        addView(binding?.root)
        onClickView()
    }

    private fun onClickView() {
        binding?.let { binding ->
            val itemViews = listOf(
                ViewItem(binding.messageView, binding.imgMessage, binding.tvMessage, R.drawable.ic_chat_fill),
                ViewItem(binding.phoneBookView, binding.imgPhoneBook, binding.tvPhoneBook, R.drawable.ic_phone_book_fill),
                ViewItem(binding.discoverView, binding.imgDiscover, binding.tvDiscover, R.drawable.ic_discover_fill),
                ViewItem(binding.diaryView, binding.imgDiary, binding.tvDiary, R.drawable.ic_diary_fill),
                ViewItem(binding.personalView, binding.imgPersonal, binding.tvPersonal, R.drawable.ic_person_fill)
            )

            for(index in itemViews.indices) {
                itemViews[index].view?.setOnClickListener {
                    unCheckAllView()
                    selectedView(
                        img = itemViews[index].imgView,
                        tv = itemViews[index].textView,
                        imgRes = itemViews[index].iconRes,
                        isChecked = true
                    )
                    actionBottomBar?.invoke(ActionBottomBar.of(index))
                }
            }
        }
    }

    private fun unCheckAllView() {
        selectedView(binding?.imgMessage, binding?.tvMessage, R.drawable.ic_chat)
        selectedView(binding?.imgPhoneBook, binding?.tvPhoneBook, R.drawable.ic_phone_book)
        selectedView(binding?.imgDiscover, binding?.tvDiscover, R.drawable.ic_discover)
        selectedView(binding?.imgDiary, binding?.tvDiary, R.drawable.ic_diary)
        selectedView(binding?.imgPersonal, binding?.tvPersonal, R.drawable.ic_person)
    }

    private fun selectedView(img: ImageView?, tv: TextView?, imgRes: Int, isChecked: Boolean = false) {
        img?.setImageResource(imgRes)
        val color = if(isChecked) R.color.text_icon else R.color.grey_1
        tv?.setTextColor(ContextCompat.getColor(context, color))
    }
}