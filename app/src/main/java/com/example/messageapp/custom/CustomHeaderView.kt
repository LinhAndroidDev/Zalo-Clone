package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.databinding.CustomHeaderViewBinding
import com.example.messageapp.fragment.HomeFragment
import com.example.messageapp.model.ActionBottomBar
import com.example.messageapp.utils.getFragmentActivity
import com.example.messageapp.utils.showKeyboard

class CustomHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var binding: CustomHeaderViewBinding? = null
    private var mTypeSearchListener: OnTypeSearchListener? = null
    var showMenuOther: (() -> Unit)? = null

    interface OnTypeSearchListener {
        fun callBackKeySearch(keySearch: String)
    }

    fun setOnTypeSearch(typeSearchListener: OnTypeSearchListener) {
        this.mTypeSearchListener = typeSearchListener
    }

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

                ActionBottomBar.SEARCH_VIEW -> {
                    binding.viewCommon.isVisible = false
                    binding.viewChat.layout.isVisible = false
                    binding.viewSearch.layout.isVisible = true
                }

                else -> {
                    binding.headerTitle.layout.isVisible = true
                    binding.headerTitle.titleView.text = array.getText(R.styleable.CustomHeaderView_title_header)
                    binding.viewCommon.isVisible = false
                    binding.viewChat.layout.isVisible = false
                    binding.viewSearch.layout.isVisible = false
                }
            }
        }

        onClickView()
    }

    private fun onClickView() {
        binding?.viewChat?.backChat?.setOnClickListener { context.getFragmentActivity()?.onBackPressed() }
        binding?.viewSearch?.backSearch?.setOnClickListener { context.getFragmentActivity()?.onBackPressed() }
        binding?.headerTitle?.backHeader?.setOnClickListener { context.getFragmentActivity()?.onBackPressed() }
        binding?.searchView?.setOnClickListener {
            val navController =
                context.getFragmentActivity()?.supportFragmentManager?.findFragmentById(R.id.navHostFragment)
                    ?.findNavController()
            navController?.navigate(R.id.searchFragment)
        }

        binding?.setting?.setOnClickListener {
            val navController =
                context.getFragmentActivity()?.supportFragmentManager?.findFragmentById(R.id.navHostFragment)
                    ?.findNavController()
            navController?.navigate(R.id.action_personalFragment_to_settingFragment, null)
        }

        binding?.viewSearch?.edtSearch?.doOnTextChanged { text, _, _, _ ->
            mTypeSearchListener?.callBackKeySearch(text.toString())
        }

        binding?.qrCode?.setOnClickListener {
            val navController =
                context.getFragmentActivity()?.supportFragmentManager?.findFragmentById(R.id.navHostFragment)
                    ?.findNavController()
            val navHostFragment = context.getFragmentActivity()
                ?.supportFragmentManager
                ?.findFragmentById(R.id.navHostFragment) as? NavHostFragment
            val currentFragment = navHostFragment
                ?.childFragmentManager
                ?.fragments
                ?.firstOrNull()
            if (currentFragment is HomeFragment) {
                navController?.navigate(R.id.action_homeFragment_to_scanQRFragment)
            } else {
                navController?.navigate(R.id.action_discoverFragment_to_scanQRFragment)
            }
        }

        binding?.menuAdd?.setOnClickListener {
            showMenuOther?.invoke()
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
        binding?.viewSearch?.edtSearch?.showKeyboard()
    }

    fun setTitleChatView(title: String) {
        binding?.viewChat?.tvFriend?.text = title
    }
}