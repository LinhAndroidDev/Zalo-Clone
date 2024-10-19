package com.example.messageapp.fragment

import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.example.messageapp.R
import com.example.messageapp.adapter.ChatAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentChatBinding
import com.example.messageapp.viewmodel.ChatFragmentViewModel

class ChatFragment : BaseFragment<FragmentChatBinding, ChatFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_chat

    override fun initView() {
        super.initView()

        binding?.rcvChat?.adapter = ChatAdapter()
        binding?.edtMessage?.doOnTextChanged { text, _, _, _ ->
            if (text?.isNotEmpty() == true) {
                binding?.viewOptions?.isVisible = false
                binding?.btnSend?.isVisible = true
            } else {
                binding?.viewOptions?.isVisible = true
                binding?.btnSend?.isVisible = false
            }
        }
    }

    override fun onClickView() {
        super.onClickView()
    }
}