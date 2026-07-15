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
import com.example.messageapp.adapter.ForwardConversationAdapter
import com.example.messageapp.databinding.BottomSheetForwardMessageBinding
import com.example.messageapp.model.Message
import com.example.messageapp.viewmodel.ForwardMessageViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BottomSheetForwardMessage : BottomSheetDialogFragment() {

    private var binding: BottomSheetForwardMessageBinding? = null
    private val viewModel: ForwardMessageViewModel by viewModels()
    private var adapter: ForwardConversationAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = BottomSheetForwardMessageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        @Suppress("DEPRECATION")
        val sourceMessage = requireArguments().getParcelable<Message>(ARG_MESSAGE)
        val excludeId = requireArguments().getString(ARG_EXCLUDE_CONVERSATION_ID).orEmpty()
        if (sourceMessage == null) {
            dismiss()
            return
        }

        adapter = ForwardConversationAdapter(viewModel.groupAvatarLoader) {
            updateConfirmButton()
        }
        binding?.rcvConversations?.adapter = adapter
        updateConfirmButton()

        viewModel.loadConversations(excludeId)

        var emptyNotified = false
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.conversations.collect { conversations ->
                    adapter?.submitList(conversations)
                    if (conversations.isEmpty() && !emptyNotified) {
                        emptyNotified = true
                        Toast.makeText(
                            requireContext(),
                            R.string.forward_no_conversations,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.avatarMap.collect { map ->
                    adapter?.updateAvatarMap(map)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadingState.collect { loading ->
                    updateConfirmButton(loading)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.forwardComplete.collect { complete ->
                    if (!complete) return@collect
                    Toast.makeText(
                        requireContext(),
                        R.string.forward_success,
                        Toast.LENGTH_SHORT,
                    ).show()
                    viewModel.resetForwardComplete()
                    dismiss()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { error ->
                    if (error.isNotBlank()) {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }

        binding?.btnConfirm?.setOnClickListener {
            val selected = adapter?.selectedConversations().orEmpty()
            if (selected.isEmpty()) return@setOnClickListener
            updateConfirmButton(loading = true)
            viewModel.forward(sourceMessage, selected)
        }
    }

    private fun updateConfirmButton(loading: Boolean = viewModel.loadingState.value) {
        val count = adapter?.selectedConversations()?.size ?: 0
        binding?.btnConfirm?.isEnabled = !loading && count >= 1
        binding?.btnConfirm?.text = if (count > 0) {
            getString(R.string.forward_confirm_count, count)
        } else {
            getString(R.string.forward_confirm)
        }
    }

    override fun onDestroyView() {
        binding = null
        adapter = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_MESSAGE = "message"
        private const val ARG_EXCLUDE_CONVERSATION_ID = "exclude_conversation_id"
        const val TAG = "BottomSheetForwardMessage"

        fun newInstance(message: Message, excludeConversationId: String) =
            BottomSheetForwardMessage().apply {
                arguments = bundleOf(
                    ARG_MESSAGE to message,
                    ARG_EXCLUDE_CONVERSATION_ID to excludeConversationId,
                )
            }
    }
}
