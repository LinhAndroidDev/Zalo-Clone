package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.adapter.ListChatAdapter
import com.example.messageapp.adapter.SuggestFriendAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentHomeBinding
import com.example.messageapp.model.User
import com.example.messageapp.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {
    override val layoutResId: Int = R.layout.fragment_home

    private val suggestFriendAdapter by lazy { SuggestFriendAdapter() }
    private val listChatAdapter by lazy { ListChatAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listChatAdapter.onClickView = { conversation ->
            val bundle = Bundle()
            val user = User(conversation)
            bundle.putParcelable(ChatFragment.FRIEND_DATA, user)
            findNavController().navigate(R.id.chatFragment, bundle)
        }
        binding?.rcvListChat?.adapter = listChatAdapter

        binding?.rcvSuggestFriend?.adapter = suggestFriendAdapter
        suggestFriendAdapter.onClickItem = { friend ->
            val bundle = Bundle()
            bundle.putParcelable(ChatFragment.FRIEND_DATA, friend)
            findNavController().navigate(R.id.chatFragment, bundle)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun bindData() {
        super.bindData()

        viewModel?.getListConversation()
        viewModel?.getSuggestFriend()

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.conversation?.collect { conversations ->
                conversations?.let {
                    listChatAdapter.items = conversations
                    listChatAdapter.notifyDataSetChanged()
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.friends?.collect { friends ->
                friends?.let {
                    suggestFriendAdapter.items = friends
                    suggestFriendAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}