package com.example.messageapp.bottom_sheet

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.adapter.PinnedMessageAdapter
import com.example.messageapp.databinding.BottomSheetPinnedMessagesBinding
import com.example.messageapp.model.PinnedMessage
import com.example.messageapp.viewmodel.ChatFragmentViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BottomSheetPinnedMessages : BottomSheetDialogFragment() {

    private var binding: BottomSheetPinnedMessagesBinding? = null
    private val chatViewModel: ChatFragmentViewModel by viewModels({ requireChatFragment() })
    private var adapter: PinnedMessageAdapter? = null
    private var itemTouchHelper: ItemTouchHelper? = null
    private var sortMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = BottomSheetPinnedMessagesBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PinnedMessageAdapter(
            onItemClick = { pin ->
                requireChatFragment().scrollToPinnedMessage(pin.messageTime)
                dismiss()
            },
            onItemLongClick = { pin, anchor -> showPinnedMenu(pin, anchor) },
        )
        binding?.rcvPinnedMessages?.adapter = adapter
        setupDragHelper()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.pinnedMessages.collect { pins ->
                    if (!sortMode) {
                        adapter?.submitList(pins)
                    }
                }
            }
        }

        binding?.btnSortDone?.setOnClickListener { exitSortMode(saveOrder = true) }
    }

    private fun setupDragHelper() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0,
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                adapter?.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun isLongPressDragEnabled(): Boolean = sortMode

            override fun canDropOver(
                recyclerView: RecyclerView,
                current: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean = sortMode
        }
        itemTouchHelper = ItemTouchHelper(callback).also {
            binding?.rcvPinnedMessages?.let { rv -> it.attachToRecyclerView(rv) }
        }
    }

    private fun showPinnedMenu(pin: PinnedMessage, anchor: View) {
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_pinned_message_menu, null)
        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            elevation = 8f
        }

        popupView.findViewById<View>(R.id.btnUnpin).setOnClickListener {
            val conversation = requireChatFragment().currentConversation() ?: return@setOnClickListener
            chatViewModel.unpinMessage(conversation, pin.messageTime)
            Toast.makeText(requireContext(), R.string.unpin_success, Toast.LENGTH_SHORT).show()
            popup.dismiss()
        }
        popupView.findViewById<View>(R.id.btnSort).setOnClickListener {
            enterSortMode()
            popup.dismiss()
        }

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight
        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val x = (anchorLoc[0] + anchor.width - popupWidth).coerceAtLeast(0)
        val y = (anchorLoc[1] + anchor.height / 2 - popupHeight / 2).coerceAtLeast(0)
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
    }

    private fun enterSortMode() {
        sortMode = true
        adapter?.setSortMode(true)
        binding?.tvSortHint?.isVisible = true
        binding?.btnSortDone?.isVisible = true
    }

    private fun exitSortMode(saveOrder: Boolean) {
        if (saveOrder) {
            val conversation = requireChatFragment().currentConversation() ?: return
            val orderedTimes = adapter?.currentList?.map { it.messageTime }.orEmpty()
            chatViewModel.reorderPinnedMessages(conversation, orderedTimes)
        }
        sortMode = false
        adapter?.setSortMode(false)
        binding?.tvSortHint?.isVisible = false
        binding?.btnSortDone?.isVisible = false
        adapter?.submitList(chatViewModel.pinnedMessages.value)
    }

    override fun onDestroyView() {
        binding = null
        adapter = null
        itemTouchHelper = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "BottomSheetPinnedMessages"

        fun newInstance() = BottomSheetPinnedMessages()
    }
}
