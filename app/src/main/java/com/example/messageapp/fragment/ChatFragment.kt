package com.example.messageapp.fragment

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
    }

    override fun onClickView() {
        super.onClickView()
    }
}