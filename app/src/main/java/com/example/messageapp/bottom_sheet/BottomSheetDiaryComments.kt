package com.example.messageapp.bottom_sheet

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messageapp.R
import com.example.messageapp.adapter.DiaryCommentAdapter
import com.example.messageapp.databinding.BottomSheetDiaryCommentsBinding
import com.example.messageapp.model.DiaryPostComment
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
    private val commentId: String by lazy { requireArguments().getString(ARG_COMMENT_ID).orEmpty() }
    private val replyId: String by lazy { requireArguments().getString(ARG_REPLY_ID).orEmpty() }
    private var currentMentionToken: String? = null
    private var isStylingMention = false
    private val adapter by lazy {
        DiaryCommentAdapter(
            onToggleLike = { viewModel.toggleCommentLike(it) },
            onReply = { comment ->
                viewModel.startReplyToComment(comment)
                beginReply(comment.authorName)
            },
            onToggleReplyLike = { reply, parentCommentId ->
                viewModel.toggleReplyLike(reply, parentCommentId)
            },
            onReplyToReply = { reply, parentCommentId ->
                viewModel.startReplyToReply(reply, parentCommentId)
                beginReply(reply.authorName)
            },
            onToggleReplies = { viewModel.toggleReplies(it) },
            onLongClick = { comment, anchor -> showCommentMenu(comment, anchor) },
        )
    }

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
        if (commentId.isNotBlank()) {
            viewModel.focusTarget(commentId, replyId)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rows.collect { adapter.submitList(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scrollToRowId.collect { rowId ->
                if (rowId.isNullOrBlank()) return@collect
                val index = viewModel.rows.value.indexOfFirst { it.rowId == rowId }
                if (index >= 0) {
                    binding.rvComments.post {
                        binding.rvComments.smoothScrollToPosition(index)
                        viewModel.clearScrollTarget()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.replyingTo.collect { target ->
                binding.layoutReplyingTo.isVisible = target != null
                if (target != null) {
                    val name = target.mentionedName.ifBlank { getString(R.string.diary_default_user_name) }
                    binding.tvReplyingTo.text = getString(R.string.diary_replying_to, name)
                }
            }
        }

        binding.btnSendComment.setOnClickListener { sendComment() }
        binding.btnCancelReply.setOnClickListener { cancelReply() }

        binding.edtComment.addTextChangedListener(afterTextChanged = { editable ->
            if (isStylingMention || editable == null) return@addTextChangedListener
            isStylingMention = true
            editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
                .forEach { editable.removeSpan(it) }
            val token = currentMentionToken
            if (token != null && editable.toString().startsWith(token)) {
                editable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.color_link)),
                    0,
                    token.trimEnd().length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            isStylingMention = false
        })
    }

    /** Bắt đầu trả lời: chèn "@Tên " vào ô nhập để người dùng thấy mention như Facebook/Zalo. */
    private fun beginReply(authorName: String) {
        val name = authorName.ifBlank { getString(R.string.diary_default_user_name) }
        val token = "@$name "
        currentMentionToken = token
        binding.edtComment.setText(token)
        binding.edtComment.setSelection(token.length)
        focusCommentInput()
    }

    private fun cancelReply() {
        viewModel.clearReply()
        val token = currentMentionToken
        val current = binding.edtComment.text?.toString().orEmpty()
        if (token != null && current.startsWith(token)) {
            val rest = current.removePrefix(token)
            binding.edtComment.setText(rest)
            binding.edtComment.setSelection(rest.length)
        }
        currentMentionToken = null
    }

    private fun focusCommentInput() {
        binding.edtComment.requestFocus()
        requireContext().getSystemService<InputMethodManager>()
            ?.showSoftInput(binding.edtComment, InputMethodManager.SHOW_IMPLICIT)
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
        viewModel.send(text) {
            requireActivity().runOnUiThread {
                binding.edtComment.setText("")
                currentMentionToken = null
            }
        }
    }

    private fun showCommentMenu(comment: DiaryPostComment, anchor: View) {
        val isOwner = comment.authorId == viewModel.currentUserId()
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_diary_comment_menu, null)

        val btnCopy = popupView.findViewById<View>(R.id.btnCopy)
        val btnEdit = popupView.findViewById<View>(R.id.btnEdit)
        val btnDelete = popupView.findViewById<View>(R.id.btnDelete)

        btnEdit.isVisible = isOwner
        btnDelete.isVisible = isOwner

        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            elevation = 8f
        }

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight
        val density = resources.displayMetrics.density
        val marginEnd = (32 * density).toInt()

        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val screenWidth = resources.displayMetrics.widthPixels
        val anchorRight = anchorLoc[0] + anchor.width
        val xOnScreen = (anchorRight - popupWidth - marginEnd)
            .coerceIn(marginEnd, screenWidth - popupWidth - marginEnd)
        val yOnScreen = (anchorLoc[1] + anchor.height / 2 - popupHeight / 2)
            .coerceIn(0, resources.displayMetrics.heightPixels - popupHeight)

        btnCopy.setOnClickListener {
            copyText(comment.text)
            popup.dismiss()
        }
        btnEdit.setOnClickListener {
            showEditDialog(comment)
            popup.dismiss()
        }
        btnDelete.setOnClickListener {
            showDeleteConfirm(comment)
            popup.dismiss()
        }

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, xOnScreen, yOnScreen)
    }

    private fun copyText(text: String) {
        val clipboard = requireContext().getSystemService<ClipboardManager>() ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("comment", text))
        Toast.makeText(requireContext(), getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
    }

    private fun showEditDialog(comment: DiaryPostComment) {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(comment.text)
            setSelection(comment.text.length)
            setPadding(48, 24, 48, 24)
        }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.diary_comment_menu_edit))
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isBlank()) return@setPositiveButton
                if (comment.parentCommentId.isBlank()) {
                    viewModel.editComment(comment, newText) {}
                } else {
                    viewModel.editReply(comment, newText) {}
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirm(comment: DiaryPostComment) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.diary_comment_delete_title))
            .setMessage(getString(R.string.diary_comment_delete_message))
            .setPositiveButton(getString(R.string.diary_post_delete_confirm)) { _, _ ->
                if (comment.parentCommentId.isBlank()) {
                    viewModel.deleteComment(comment) {}
                } else {
                    viewModel.deleteReply(comment) {}
                }
            }
            .setNegativeButton(getString(R.string.diary_post_delete_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_POST_ID = "postId"
        private const val ARG_COMMENT_ID = "commentId"
        private const val ARG_REPLY_ID = "replyId"

        fun newInstance(
            postId: String,
            commentId: String = "",
            replyId: String = "",
        ) = BottomSheetDiaryComments().apply {
            arguments = bundleOf(
                ARG_POST_ID to postId,
                ARG_COMMENT_ID to commentId,
                ARG_REPLY_ID to replyId,
            )
        }
    }
}
