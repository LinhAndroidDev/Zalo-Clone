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
import com.example.messageapp.model.Conversation
import com.example.messageapp.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {
    override val layoutResId: Int = R.layout.fragment_home

    private val suggestFriendAdapter by lazy { SuggestFriendAdapter() }
    private var listChatAdapter: ListChatAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listChatAdapter = ListChatAdapter(viewModel?.shared?.getAuth() ?: "")
        listChatAdapter?.onClickView = { conversation ->
            goToChatFragment(conversation)
        }
        binding?.rcvListChat?.adapter = listChatAdapter

        binding?.rcvSuggestFriend?.adapter = suggestFriendAdapter
        suggestFriendAdapter.onClickItem = { friend ->
            goToChatFragment(Conversation(friend))
        }
    }

    private fun goToChatFragment(conversation: Conversation) {
        val action = HomeFragmentDirections.actionHomeFragmentToChatFragment(conversation)
        findNavController().navigate(action)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun bindData() {
        super.bindData()

        viewModel?.generateToken()
        viewModel?.getListConversation()
        viewModel?.getSuggestFriend()

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.conversation?.collect { conversations ->
                conversations?.let {
                    listChatAdapter?.items = conversations
                    listChatAdapter?.notifyDataSetChanged()
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