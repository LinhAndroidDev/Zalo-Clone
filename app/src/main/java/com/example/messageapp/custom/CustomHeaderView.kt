package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.example.messageapp.MainActivity
import com.example.messageapp.R
import com.example.messageapp.databinding.CustomHeaderViewBinding
import com.example.messageapp.utils.showKeyboard

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

                ActionBottomBar.CHAT_VIEW -> {
                    binding.viewCommon.isVisible = false
                    binding.viewChat.layout.isVisible = true
                    binding.viewSearch.layout.isVisible = false
                }

                else -> {
                    binding.viewCommon.isVisible = false
                    binding.viewChat.layout.isVisible = false
                    binding.viewSearch.layout.isVisible = true
                }
            }
        }

        onClickView()
    }

    private fun onClickView() {
        binding?.viewChat?.backChat?.setOnClickListener { (context as FragmentActivity).onBackPressed() }
        binding?.viewSearch?.backSearch?.setOnClickListener { (context as FragmentActivity).onBackPressed() }
        binding?.searchView?.setOnClickListener {
            val navController = (context as MainActivity).supportFragmentManager.findFragmentById(R.id.navHostFragment)?.findNavController()
            navController?.navigate(R.id.searchFragment)
        }
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

    fun focusSearch() {
        binding?.viewSearch?.edtSearch?.isFocusable = true
        binding?.viewSearch?.edtSearch?.showKeyboard()
    }
}