package com.example.messageapp.fragment

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.adapter.SearchAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.custom.CustomHeaderView
import com.example.messageapp.databinding.FragmentSearchBinding
import com.example.messageapp.model.Conversation
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.viewmodel.SearchFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : BaseFragment<FragmentSearchBinding, SearchFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_search

    private val searchAdapter = SearchAdapter().apply {
        onAddFriend = { user -> viewModel?.sendFriendRequest(user) }
        onChat = { user ->
            val action = SearchFragmentDirections.actionSearchFragmentToChatFragment(
                Conversation(user)
            )
            findNavController().navigate(action)
        }
    }

    override fun initView() {
        super.initView()
        FirebaseAnalyticsInstance.logSearchScreen()

        binding?.root?.post {
            binding?.header?.focusSearch()
        }

        binding?.header?.setOnTypeSearch(object : CustomHeaderView.OnTypeSearchListener {
            override fun callBackKeySearch(keySearch: String) {
                viewModel?.searchFriend(keySearch)
            }
        })

        binding?.rcvSearchFriend?.adapter = searchAdapter
    }

    override fun bindData() {
        super.bindData()
        lifecycleScope.launch {
            viewModel?.users?.collect { list ->
                binding?.tvNumberFriend?.text =
                    String.format(getString(R.string.you_may_be_familiar_with), list.size.toString())
                searchAdapter.updateUserDiff(list)
            }
        }
    }
}
