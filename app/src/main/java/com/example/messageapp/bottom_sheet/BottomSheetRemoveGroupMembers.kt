package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.messageapp.R
import com.example.messageapp.adapter.GroupMemberRemoveAdapter
import com.example.messageapp.databinding.BottomSheetRemoveGroupMembersBinding
import com.example.messageapp.model.User
import com.example.messageapp.viewmodel.ChatFragmentViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BottomSheetRemoveGroupMembers : BottomSheetDialogFragment() {

    private var binding: BottomSheetRemoveGroupMembersBinding? = null
    private val chatViewModel: ChatFragmentViewModel by viewModels({ requireChatFragment() })
    private var adapter: GroupMemberRemoveAdapter? = null
    private var myUserId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = BottomSheetRemoveGroupMembersBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val groupId = requireArguments().getString(ARG_GROUP_ID).orEmpty()
        if (groupId.isBlank()) {
            dismiss()
            return
        }

        myUserId = chatViewModel.shared.getAuth()

        adapter = GroupMemberRemoveAdapter { user ->
            confirmRemoveMember(groupId, user)
        }
        binding?.rcvMembers?.adapter = adapter

        chatViewModel.refreshGroupMembersForManage(groupId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.groupMembersForManage.collect { members ->
                    val removable = members.filter { it.keyAuth.orEmpty().isNotBlank() && it.keyAuth != myUserId }
                    adapter?.submitList(removable)
                }
            }
        }
    }

    private fun confirmRemoveMember(groupId: String, user: User) {
        val memberId = user.keyAuth.orEmpty()
        if (memberId.isBlank()) return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.group_menu_remove_members)
            .setMessage(getString(R.string.group_remove_confirm_message, user.name.orEmpty()))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.group_remove_member) { _, _ ->
                chatViewModel.removeGroupMember(groupId, memberId) { }
            }
            .show()
    }

    override fun onDestroyView() {
        binding = null
        adapter = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_GROUP_ID = "group_id"
        const val TAG = "BottomSheetRemoveGroupMembers"

        fun newInstance(groupId: String) = BottomSheetRemoveGroupMembers().apply {
            arguments = bundleOf(ARG_GROUP_ID to groupId)
        }
    }
}
