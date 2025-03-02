package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.messageapp.MainActivity
import com.example.messageapp.R
import com.example.messageapp.adapter.ListChatAdapter
import com.example.messageapp.adapter.SuggestFriendAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetOptionConversation
import com.example.messageapp.databinding.FragmentHomeBinding
import com.example.messageapp.model.Conversation
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {
    override val layoutResId: Int = R.layout.fragment_home

    private val suggestFriendAdapter by lazy { SuggestFriendAdapter() }
    private var listChatAdapter: ListChatAdapter? = null
    private var updateJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listChatAdapter = ListChatAdapter()
        listChatAdapter?.onClickView = { conversation ->
            goToChatFragment(conversation)
        }
        listChatAdapter?.showOptionConversation = {
            val bottomSheetOptionConversation = BottomSheetOptionConversation()
            bottomSheetOptionConversation.show(parentFragmentManager, "")
        }
        binding?.rcvListChat?.adapter = listChatAdapter
        AnimatorUtils.fadeInItemRecyclerView(requireActivity(), binding?.rcvListChat)

        binding?.rcvSuggestFriend?.adapter = suggestFriendAdapter
        AnimatorUtils.fadeInItemRecyclerView(requireActivity(), binding?.rcvSuggestFriend)
        suggestFriendAdapter.onClickItem = { friend ->
            goToChatFragment(Conversation(friend))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        listChatAdapter?.saveStates(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        listChatAdapter?.restoreStates(savedInstanceState)
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

        viewModel?.getNumberUnSeen()
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.numberMsgUnSeen?.collect { num ->
                (activity as MainActivity).setUpNumberMessage(num)
            }
        }
    }

    // Update chat time every minute
    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        updateJob = lifecycleScope.launch {
            while (isActive) {
                listChatAdapter?.notifyDataSetChanged()
                delay(60_000L)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        updateJob?.cancel()
    }
}