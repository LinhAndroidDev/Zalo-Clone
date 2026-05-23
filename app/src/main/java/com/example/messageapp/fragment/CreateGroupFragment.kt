package com.example.messageapp.fragment

import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.adapter.CreateGroupMemberAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentCreateGroupBinding
import com.example.messageapp.viewmodel.CreateGroupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateGroupFragment : BaseFragment<FragmentCreateGroupBinding, CreateGroupViewModel>() {
    override val layoutResId: Int = R.layout.fragment_create_group

    private var adapter: CreateGroupMemberAdapter? = null

    override fun initView() {
        super.initView()
        adapter = CreateGroupMemberAdapter {
            val n = adapter?.selectedFriendIds()?.size ?: 0
            binding?.btnCreate?.isEnabled = n >= 1
        }
        binding?.rcvMembers?.adapter = adapter
        binding?.btnCreate?.isEnabled = false
        binding?.btnBack?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun bindData() {
        super.bindData()
        viewModel?.loadFriends()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.friends?.collect { list ->
                    adapter?.submitList(list)
                }
            }
        }
    }

    override fun onClickView() {
        super.onClickView()
        binding?.btnCreate?.setOnClickListener {
            val name = binding?.edtGroupName?.text?.toString()?.trim().orEmpty()
            val selected = adapter?.selectedFriendIds().orEmpty()
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), R.string.select_group_members, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val groupTitle = name.ifBlank { getString(R.string.create_group_title) }
            val welcomeMessage = getString(R.string.group_welcome_message, groupTitle)
            val welcomePerson = getString(R.string.group_welcome_inbox_person)
            viewModel?.createGroup(
                displayName = groupTitle,
                welcomeMessage = welcomeMessage,
                welcomeInboxPerson = welcomePerson,
                otherMemberIds = selected,
                onSuccess = { conversation ->
                    findNavController().navigate(
                        R.id.action_createGroupFragment_to_chatFragment,
                        bundleOf("conversation" to conversation),
                    )
                },
                onFailure = { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}
