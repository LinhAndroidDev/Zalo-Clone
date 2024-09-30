package com.example.messageapp.fragment

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.adapter.ListChatAdapter
import com.example.messageapp.adapter.SuggestFriendAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentHomeBinding
import com.example.messageapp.viewmodel.HomeViewModel

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {
    override val layoutResId: Int = R.layout.fragment_home

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatAdapter = ListChatAdapter()
        chatAdapter.onClickView = {
            findNavController().navigate(R.id.chatFragment)
        }
        binding?.rcvListChat?.adapter = chatAdapter


        binding?.rcvSuggestFriend?.adapter = SuggestFriendAdapter()
    }
}