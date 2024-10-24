package com.example.messageapp.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.adapter.ListChatAdapter
import com.example.messageapp.adapter.SuggestFriendAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentHomeBinding
import com.example.messageapp.helper.avatars
import com.example.messageapp.model.Friend
import com.example.messageapp.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {
    override val layoutResId: Int = R.layout.fragment_home

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val names = mutableListOf("An", "Bảo", "Brian", "Alex", "Aiden", "Finn", "Khánh", "Duy")
        val friends = arrayListOf<Friend>()
        friends.add(Friend("Thời Tiết", "https://cdn.jim-nielsen.com/ios/512/weather-2021-12-07.png?rf=1024"))
        names.forEachIndexed { index, s ->
            friends.add(Friend(s, avatars[index]))
        }
        val chatAdapter = ListChatAdapter()
        chatAdapter.items = friends
        chatAdapter.onClickView = {
            findNavController().navigate(R.id.chatFragment)
        }
        binding?.rcvListChat?.adapter = chatAdapter
    }

    override fun bindData() {
        super.bindData()

        viewModel?.getSuggestFriend()

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.friends?.collect { friends ->
                friends?.let {
                    val adapter = SuggestFriendAdapter()
                    adapter.items = friends
                    binding?.rcvSuggestFriend?.adapter = adapter
                }
            }
        }
    }
}