package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messageapp.R
import com.example.messageapp.adapter.DiaryCommentAdapter
import com.example.messageapp.databinding.BottomSheetDiaryCommentsBinding
import com.example.messageapp.viewmodel.BottomSheetDiaryCommentsViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BottomSheetDiaryComments : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDiaryCommentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<BottomSheetDiaryCommentsViewModel>()

    private val postId: String by lazy { requireArguments().getString(ARG_POST_ID).orEmpty() }
    private val adapter = DiaryCommentAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDiaryCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val sheetDialog = dialog as? BottomSheetDialog ?: return
        sheetDialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        val bottomSheet = sheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        BottomSheetBehavior.from(bottomSheet).apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (postId.isBlank()) {
            dismiss()
            return
        }
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComments.adapter = adapter

        viewModel.startComments(postId)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.comments.collect { adapter.submitList(it) }
        }

        binding.btnSendComment.setOnClickListener { sendComment() }
    }

    private fun sendComment() {
        val text = binding.edtComment.text?.toString().orEmpty().trim()
        if (text.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.diary_enter_comment_body),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (viewModel.currentUserId().isBlank()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.diary_not_logged_in),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        viewModel.sendComment(postId, text) {
            requireActivity().runOnUiThread {
                binding.edtComment.setText("")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_POST_ID = "postId"

        fun newInstance(postId: String) = BottomSheetDiaryComments().apply {
            arguments = bundleOf(ARG_POST_ID to postId)
        }
    }
}
