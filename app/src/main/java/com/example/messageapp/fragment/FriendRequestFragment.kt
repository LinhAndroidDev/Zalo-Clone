package com.example.messageapp.fragment

import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.PersonalActivity
import com.example.messageapp.R
import com.example.messageapp.adapter.RequestFriendAdapter
import com.example.messageapp.adapter.SentRequestAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentFriendRequestBinding
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.viewmodel.FragmentFriendRequestViewModel
import com.example.messageapp.viewmodel.MainViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendRequestFragment :
    BaseFragment<FragmentFriendRequestBinding, FragmentFriendRequestViewModel>() {
    override val layoutResId: Int = R.layout.fragment_friend_request

    private val mainViewModel: MainViewModel by activityViewModels()

    private val receivedAdapter by lazy {
        RequestFriendAdapter().apply {
            onItemClick = { request -> openPersonalActivity(request.fromId) }
            onAccept = { request -> viewModel?.acceptRequest(request) }
            onReject = { requestId -> viewModel?.rejectRequest(requestId) }
        }
    }

    private val sentAdapter by lazy {
        SentRequestAdapter().apply {
            onItemClick = { request -> openPersonalActivity(request.toId) }
            onCancel = { request -> viewModel?.cancelSentRequest(request) }
        }
    }

    override fun initView() {
        super.initView()
        FirebaseAnalyticsInstance.logFriendRequestScreen()

        binding?.rcvReceivedRequests?.adapter = receivedAdapter
        binding?.rcvSentRequests?.adapter = sentAdapter

        binding?.tabLayout?.addTab(binding!!.tabLayout.newTab().setText(getString(R.string.tab_received)))
        binding?.tabLayout?.addTab(binding!!.tabLayout.newTab().setText(getString(R.string.tab_sent)))

        binding?.tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showReceivedTab()
                    1 -> showSentTab()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewModel?.getIncomingFriendRequests()
        viewModel?.getOutgoingFriendRequests()
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.markAsSeen()
    }

    override fun bindData() {
        super.bindData()

        lifecycleScope.launch {
            viewModel?.receivedRequests?.collect { requests ->
                receivedAdapter.updateDiff(requests)
                updateTabTitle(0, getString(R.string.tab_received), requests.size)
            }
        }

        lifecycleScope.launch {
            viewModel?.sentRequests?.collect { requests ->
                sentAdapter.updateDiff(requests)
                updateTabTitle(1, getString(R.string.tab_sent), requests.size)
            }
        }
    }

    private fun updateTabTitle(index: Int, label: String, count: Int) {
        val tab = binding?.tabLayout?.getTabAt(index) ?: return
        tab.text = "$label ($count)"
    }

    private fun showReceivedTab() {
        binding?.rcvReceivedRequests?.isVisible = true
        binding?.rcvSentRequests?.isVisible = false
    }

    private fun showSentTab() {
        binding?.rcvReceivedRequests?.isVisible = false
        binding?.rcvSentRequests?.isVisible = true
    }

    private fun openPersonalActivity(userId: String) {
        val intent = android.content.Intent(requireActivity(), PersonalActivity::class.java)
        intent.putExtra(PersonalActivity.FRIEND_ID_KEY, userId)
        requireActivity().startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}
