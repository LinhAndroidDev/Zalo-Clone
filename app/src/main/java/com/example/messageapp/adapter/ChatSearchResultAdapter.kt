package com.example.messageapp.adapter

import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemChatSearchResultBinding
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.SearchHighlightHelper
import com.example.messageapp.viewmodel.ChatSearchResultItem

class ChatSearchResultAdapter : BaseAdapter<ChatSearchResultItem, ItemChatSearchResultBinding>() {

    var onItemClick: ((ChatSearchResultItem) -> Unit)? = null
    var onMissingAvatar: ((String, () -> Unit) -> Unit)? = null
    var resolveSenderAvatar: ((String) -> String)? = null

    override fun getLayout(): Int = R.layout.item_chat_search_result

    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemChatSearchResultBinding>,
        position: Int,
    ) {
        val item = items[position]
        val context = holder.v.root.context

        holder.v.tvSenderName.text = item.senderName
        holder.v.tvMessageContent.text = SearchHighlightHelper.buildHighlightedContent(
            context = context,
            messageText = item.messageText,
            matchStart = item.matchStart,
            matchEnd = item.matchEnd,
            displayTime = item.displayTime,
        )

        bindAvatar(holder, item)
        holder.v.root.setOnClickListener { onItemClick?.invoke(item) }
    }

    private fun bindAvatar(
        holder: BaseViewHolder<ItemChatSearchResultBinding>,
        item: ChatSearchResultItem,
    ) {
        val avatar = item.senderAvatar
        if (avatar.isNotBlank()) {
            holder.v.root.context.loadImg(avatar, holder.v.imgSenderAvatar, R.drawable.bg_grey_equal)
            return
        }
        holder.v.root.context.loadImg("", holder.v.imgSenderAvatar, R.drawable.bg_grey_equal)
        onMissingAvatar?.invoke(item.senderId) {
            val loadedAvatar = resolveSenderAvatar?.invoke(item.senderId).orEmpty()
            if (loadedAvatar.isNotBlank()) {
                holder.v.root.context.loadImg(loadedAvatar, holder.v.imgSenderAvatar, R.drawable.bg_grey_equal)
            }
        }
    }

    fun updateResults(newList: List<ChatSearchResultItem>) {
        updateDiffList(
            newList = newList,
            compareItem = { old, new -> old.messageTime == new.messageTime },
            compareContent = { old, new ->
                old.senderName == new.senderName &&
                    old.messageText == new.messageText &&
                    old.displayTime == new.displayTime &&
                    old.matchStart == new.matchStart &&
                    old.matchEnd == new.matchEnd &&
                    old.senderAvatar == new.senderAvatar
            },
        )
    }
}
