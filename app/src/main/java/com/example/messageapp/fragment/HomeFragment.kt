package com.example.messageapp.fragment

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.adapter.ListChatAdapter
import com.example.messageapp.adapter.SuggestFriendAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentHomeBinding
import com.example.messageapp.helper.avatars
import com.example.messageapp.model.Friend
import com.example.messageapp.viewmodel.HomeViewModel

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {
    override val layoutResId: Int = R.layout.fragment_home

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val names = mutableListOf("An", "Bảo", "Brian", "Alex", "Aiden", "Finn", "Khánh", "Duy")
        val friends = arrayListOf<Friend>()
        friends.add(Friend("Thoi tiet", "https://c3.klipartz.com/pngpicture/514/476/sticker-png-ios-8-icons-weather.png"))
        names.forEachIndexed { index, s ->
            friends.add(Friend(s, avatars[index]))
        }
        val chatAdapter = ListChatAdapter()
        chatAdapter.items = friends
        chatAdapter.onClickView = {
            findNavController().navigate(R.id.chatFragment)
        }
        binding?.rcvListChat?.adapter = chatAdapter


        binding?.rcvSuggestFriend?.adapter = SuggestFriendAdapter()
    }
}