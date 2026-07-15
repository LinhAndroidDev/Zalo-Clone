package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemForwardConversationBinding
import com.example.messageapp.model.Conversation
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.GroupAvatarLoader

class ForwardConversationAdapter(
    private val groupAvatarLoader: GroupAvatarLoader,
    private val onSelectionChanged: () -> Unit = {},
) : RecyclerView.Adapter<ForwardConversationAdapter.VH>() {

    private val items = ArrayList<Conversation>()
    private val selected = mutableSetOf<String>()
    private var avatarMap: Map<String, String> = emptyMap()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(conversations: List<Conversation>) {
        items.clear()
        items.addAll(conversations)
        selected.clear()
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun selectedConversations(): List<Conversation> =
        items.filter { selected.contains(it.friendId) }

    @SuppressLint("NotifyDataSetChanged")
    fun updateAvatarMap(map: Map<String, String>) {
        if (avatarMap == map) return
        avatarMap = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemForwardConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: VH) {
        groupAvatarLoader.cancel(holder.binding.groupAvatar.tag as? String)
        holder.binding.groupAvatar.reset()
        super.onViewRecycled(holder)
    }

    inner class VH(val binding: ItemForwardConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            val id = conversation.friendId
            binding.tvName.text = conversation.name
            bindAvatar(conversation)
            binding.checkSelect.setOnCheckedChangeListener(null)
            binding.checkSelect.isChecked = selected.contains(id)
            binding.checkSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selected.add(id) else selected.remove(id)
                onSelectionChanged()
            }
            binding.root.setOnClickListener {
                binding.checkSelect.isChecked = !binding.checkSelect.isChecked
            }
        }

        private fun bindAvatar(conversation: Conversation) {
            val friendId = conversation.friendId
            if (conversation.isGroupThread()) {
                binding.avatarFriend.isVisible = false
                binding.groupAvatar.isVisible = true
                binding.groupAvatar.tag = friendId
                if (conversation.friendImage.isNotBlank()) {
                    binding.groupAvatar.showSinglePhoto(conversation.friendImage)
                } else {
                    binding.groupAvatar.showPlaceholder()
                    groupAvatarLoader.load(
                        groupId = friendId,
                        onReady = { data ->
                            if (binding.groupAvatar.tag != friendId) return@load
                            binding.groupAvatar.bindMemberAvatars(data.avatarUrls, data.totalCount)
                        },
                        onError = {
                            if (binding.groupAvatar.tag != friendId) return@load
                            binding.groupAvatar.showPlaceholder()
                        },
                    )
                }
                return
            }
            binding.groupAvatar.isVisible = false
            binding.groupAvatar.reset()
            binding.avatarFriend.isVisible = true
            val avatarUrl = avatarMap[friendId].orEmpty().ifBlank { conversation.friendImage }
            binding.root.context.loadImg(
                avatarUrl,
                binding.avatarFriend,
                R.drawable.bg_grey_equal,
            )
        }
    }
}
