package com.example.messageapp.fragment

import android.content.Intent
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.messageapp.PersonalActivity
import com.example.messageapp.R
import com.example.messageapp.adapter.SearchAdapter
import com.example.messageapp.adapter.SearchHistoryAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.custom.CustomHeaderView
import com.example.messageapp.databinding.FragmentSearchBinding
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.User
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.viewmodel.SearchFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : BaseFragment<FragmentSearchBinding, SearchFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_search

    private val searchHistoryAdapter = SearchHistoryAdapter().apply {
        onItemClick = { user -> openPersonalActivity(user) }
    }

    private val searchAdapter = SearchAdapter().apply {
        onAddFriend = { user -> viewModel?.sendFriendRequest(user) }
        onChat = { user ->
            val action = SearchFragmentDirections.actionSearchFragmentToChatFragment(
                Conversation(user)
            )
            findNavController().navigate(action)
        }
        onItemClick = { user ->
            viewModel?.saveSearchHistory(user)
            openPersonalActivity(user)
        }
    }

    override fun initView() {
        super.initView()
        FirebaseAnalyticsInstance.logSearchScreen()

        binding?.root?.post {
            binding?.header?.focusSearch()
        }

        binding?.rcvSearchHistory?.adapter = searchHistoryAdapter
        binding?.rcvSearchFriend?.adapter = searchAdapter

        binding?.header?.setOnTypeSearch(object : CustomHeaderView.OnTypeSearchListener {
            override fun callBackKeySearch(keySearch: String) {
                viewModel?.searchFriend(keySearch)
            }
        })

        viewModel?.getSearchHistory()
    }

    override fun bindData() {
        super.bindData()

        lifecycleScope.launch {
            viewModel?.users?.collect { list ->
                val hasResults = list.isNotEmpty()
                binding?.tvNumberFriend?.text =
                    String.format(getString(R.string.you_may_be_familiar_with), list.size.toString())
                binding?.tvNumberFriend?.isVisible = hasResults
                searchAdapter.updateUserDiff(list)
            }
        }

        lifecycleScope.launch {
            viewModel?.history?.collect { historyList ->
                searchHistoryAdapter.updateDiff(historyList)
                val show = historyList.isNotEmpty() && searchAdapter.items.isEmpty()
                binding?.tvSearchHistory?.isVisible = show
                binding?.rcvSearchHistory?.isVisible = show
                binding?.dividerHistory?.isVisible = show
            }
        }
    }

    private fun openPersonalActivity(user: User) {
        val intent = Intent(requireActivity(), PersonalActivity::class.java)
        intent.putExtra(PersonalActivity.FRIEND_ID_KEY, user.keyAuth)
        requireActivity().startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}
