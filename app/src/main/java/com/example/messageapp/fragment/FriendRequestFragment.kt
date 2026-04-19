package com.example.messageapp.fragment

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.R
import com.example.messageapp.adapter.RequestFriendAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentFriendRequestBinding
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.viewmodel.FragmentFriendRequestViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendRequestFragment :
    BaseFragment<FragmentFriendRequestBinding, FragmentFriendRequestViewModel>() {
    override val layoutResId: Int = R.layout.fragment_friend_request

    private val requestFriendAdapter by lazy {
        RequestFriendAdapter().apply {
            onAccept = { request -> viewModel?.acceptRequest(request) }
            onReject = { requestId -> viewModel?.rejectRequest(requestId) }
        }
    }

    override fun initView() {
        super.initView()
        FirebaseAnalyticsInstance.logFriendRequestScreen()
        binding?.rcvRequestFriend?.adapter = requestFriendAdapter
        binding?.viewSeeMore?.isVisible = false
        viewModel?.getIncomingFriendRequests()
    }

    override fun bindData() {
        super.bindData()
        lifecycleScope.launch {
            viewModel?.requests?.collect { requests ->
                requestFriendAdapter.updateDiff(requests)
            }
        }
    }
}
