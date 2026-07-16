package com.example.messageapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.model.PinnedMessage
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.utils.FileUtils.loadImg

class PinnedMessageAdapter(
    private val onItemClick: (PinnedMessage) -> Unit,
    private val onItemLongClick: (PinnedMessage, View) -> Unit,
) : ListAdapter<PinnedMessage, PinnedMessageAdapter.ViewHolder>(DiffCallback) {

    private var sortMode = false

    fun setSortMode(enabled: Boolean) {
        if (sortMode == enabled) return
        sortMode = enabled
        notifyDataSetChanged()
    }

    fun isSortMode(): Boolean = sortMode

    fun moveItem(from: Int, to: Int) {
        if (from == to) return
        val current = currentList.toMutableList()
        val item = current.removeAt(from)
        current.add(to, item)
        submitList(current)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pinned_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), sortMode)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPreview: TextView = itemView.findViewById(R.id.tvPinnedPreview)
        private val tvPinnedBy: TextView = itemView.findViewById(R.id.tvPinnedBy)
        private val imgThumb: ImageView = itemView.findViewById(R.id.imgPinnedThumb)
        private val imgDragHandle: ImageView = itemView.findViewById(R.id.imgDragHandle)

        fun bind(pin: PinnedMessage, sortMode: Boolean) {
            tvPreview.text = pin.previewText
            val byName = pin.pinnedByName.ifBlank { pin.pinnedBy }
            tvPinnedBy.text = if (byName.isNotBlank()) {
                itemView.context.getString(R.string.pinned_by_label, byName)
            } else {
                ""
            }
            tvPinnedBy.isVisible = byName.isNotBlank()

            val photoUrl = pin.photoUrl
            if (!photoUrl.isNullOrBlank() && TypeMessage.of(pin.messageType) != TypeMessage.AUDIO) {
                imgThumb.isVisible = true
                itemView.context.loadImg(photoUrl, imgThumb)
            } else {
                imgThumb.isVisible = false
            }

            imgDragHandle.isVisible = sortMode
            itemView.setOnClickListener {
                if (!sortMode) onItemClick(pin)
            }
            itemView.setOnLongClickListener {
                if (!sortMode) {
                    onItemLongClick(pin, itemView)
                    true
                } else {
                    false
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PinnedMessage>() {
        override fun areItemsTheSame(oldItem: PinnedMessage, newItem: PinnedMessage): Boolean =
            oldItem.messageTime == newItem.messageTime

        override fun areContentsTheSame(oldItem: PinnedMessage, newItem: PinnedMessage): Boolean =
            oldItem == newItem
    }
}
