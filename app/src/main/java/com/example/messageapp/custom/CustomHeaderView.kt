package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.example.messageapp.R
import com.example.messageapp.databinding.CustomHeaderViewBinding

class CustomHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var binding: CustomHeaderViewBinding? = null

    init {
        binding = CustomHeaderViewBinding.inflate(LayoutInflater.from(context))
        binding?.let { binding ->
            binding.root.layoutParams =
                LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            addView(binding.root)

            val array =
                context.theme.obtainStyledAttributes(attrs, R.styleable.CustomHeaderView, 0, 0)
            val type = array.getInt(R.styleable.CustomHeaderView_item_view, 0)
            when (ActionBottomBar.of(type)) {
                ActionBottomBar.MESSAGE_VIEW -> {
                    hideAllViews()
                    binding.qrCode.isVisible = true
                    binding.menuAdd.isVisible = true
                }

                ActionBottomBar.PHONE_BOOK_VIEW -> {
                    hideAllViews()
                    binding.addFriend.isVisible = true
                }

                ActionBottomBar.DISCOVER_VIEW -> {
                    hideAllViews()
                    binding.qrCode.isVisible = true
                }

                ActionBottomBar.DIARY_VIEW -> {
                    hideAllViews()
                    binding.layoutDiary.isVisible = true
                }

                ActionBottomBar.PERSONAL_VIEW -> {
                    hideAllViews()
                    binding.setting.isVisible = true
                }

                else -> {
                    binding.viewCommon.isVisible = false
                    binding.viewChat.isVisible = true
                }
            }
        }

        onClickView()
    }

    private fun onClickView() {
        binding?.backChat?.setOnClickListener { (context as FragmentActivity).onBackPressed() }
    }

    private fun hideAllViews() {
        binding?.let { binding ->
            binding.qrCode.isVisible = false
            binding.menuAdd.isVisible = false
            binding.addFriend.isVisible = false
            binding.layoutDiary.isVisible = false
            binding.setting.isVisible = false
        }
    }
}