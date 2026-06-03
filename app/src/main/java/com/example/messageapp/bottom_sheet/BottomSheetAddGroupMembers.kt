package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.messageapp.R
import com.example.messageapp.adapter.CreateGroupMemberAdapter
import com.example.messageapp.databinding.BottomSheetAddGroupMembersBinding
import com.example.messageapp.viewmodel.ChatFragmentViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BottomSheetAddGroupMembers : BottomSheetDialogFragment() {

    private var binding: BottomSheetAddGroupMembersBinding? = null
    private val chatViewModel: ChatFragmentViewModel by viewModels({ requireChatFragment() })
    private var adapter: CreateGroupMemberAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = BottomSheetAddGroupMembersBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val groupId = requireArguments().getString(ARG_GROUP_ID).orEmpty()
        if (groupId.isBlank()) {
            dismiss()
            return
        }

        adapter = CreateGroupMemberAdapter {
            val n = adapter?.selectedFriendIds()?.size ?: 0
            binding?.btnConfirm?.isEnabled = n >= 1
        }
        binding?.rcvMembers?.adapter = adapter
        binding?.btnConfirm?.isEnabled = false

        chatViewModel.prepareAddMembersSheet(groupId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.friendsToAdd.collect { friends ->
                    adapter?.submitList(friends)
                    if (friends.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            R.string.group_no_friends_to_add,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }

        binding?.btnConfirm?.setOnClickListener {
            val selected = adapter?.selectedFriendIds().orEmpty()
            if (selected.isEmpty()) return@setOnClickListener
            binding?.btnConfirm?.isEnabled = false
            chatViewModel.addGroupMembers(groupId, selected) {
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        adapter = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_GROUP_ID = "group_id"
        const val TAG = "BottomSheetAddGroupMembers"

        fun newInstance(groupId: String) = BottomSheetAddGroupMembers().apply {
            arguments = bundleOf(ARG_GROUP_ID to groupId)
        }
    }
}
