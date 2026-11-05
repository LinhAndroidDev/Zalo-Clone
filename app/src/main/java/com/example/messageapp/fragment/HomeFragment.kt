package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.messageapp.MainActivity
import com.example.messageapp.R
import com.example.messageapp.adapter.ListChatAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetOptionConversation
import com.example.messageapp.databinding.FragmentHomeBinding
import com.example.messageapp.model.Conversation
import com.example.messageapp.permission.PermissionRequester
import com.example.messageapp.service.ChatHeadService
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.utils.GroupAvatarLoader
import com.example.messageapp.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {
    override val layoutResId: Int = R.layout.fragment_home

    @Inject lateinit var groupAvatarLoader: GroupAvatarLoader

    private var listChatAdapter: ListChatAdapter? = null
    private var updateJob: Job? = null
    private var menuOtherShowing = false

    private val permissionRequester = PermissionRequester(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listChatAdapter = ListChatAdapter(groupAvatarLoader)
        listChatAdapter?.onClickView = { conversation ->
            goToChatFragment(conversation)
        }
        listChatAdapter?.showOptionConversation = {
            val bottomSheetOptionConversation = BottomSheetOptionConversation()
            bottomSheetOptionConversation.show(parentFragmentManager, "")
        }
        binding?.rcvListChat?.adapter = listChatAdapter
        AnimatorUtils.fadeInItemRecyclerView(requireActivity(), binding?.rcvListChat)
    }

    override fun initView() {
        super.initView()
        FirebaseAnalyticsInstance.logHomeScreen()
    }

    override fun onClickView() {
        super.onClickView()

        binding?.menuOther?.findViewById<View>(R.id.rowCreateGroup)?.setOnClickListener {
            menuOtherShowing = false
            binding?.menuOther?.showView(false)
            findNavController().navigate(R.id.action_homeFragment_to_createGroupFragment)
        }

        binding?.btnFindMoreFriend?.setOnClickListener {
            permissionRequester.requestOverlay {
                activity?.startService(Intent(requireActivity(), ChatHeadService::class.java))
            }
        }

        binding?.header?.showMenuOther = {
            menuOtherShowing = !menuOtherShowing
            binding?.menuOther?.showView(menuOtherShowing)
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

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.conversation?.collect { conversations ->
                conversations?.let {
                    listChatAdapter?.updateDiffConversation(conversations)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.presenceMap?.collect { presenceMap ->
                listChatAdapter?.updatePresenceMap(presenceMap)
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.typingMap?.collect { typingMap ->
                listChatAdapter?.updateTypingMap(typingMap)
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.avatarMap?.collect { avatarMap ->
                listChatAdapter?.updateAvatarMap(avatarMap)
            }
        }

        viewModel?.getNumberUnSeen()
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.numberMsgUnSeen?.collect { num ->
                (activity as MainActivity).setUpNumberMessage(num)
            }
        }
    }

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
