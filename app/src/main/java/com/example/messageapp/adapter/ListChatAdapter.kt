package com.example.messageapp.adapter

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemListChatBinding
import com.example.messageapp.model.Conversation
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.loadImg

class ListChatAdapter :
    BaseAdapter<Conversation, ItemListChatBinding>() {

    var onClickView: ((Conversation) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_list_chat

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BaseViewHolder<ItemListChatBinding>, position: Int) {
        val conversation = items[position]
        holder.v.tvNameFriend.text = conversation.name
        holder.v.tvMessage.text = "${conversation.person}: ${conversation.message}"
        holder.v.tvTime.text = DateUtils.convertTimeToHour(conversation.time)
        holder.handleWhenConversationIsChanged(conversation)
        FireBaseInstance.getInfoUser(conversation.friendId) { user ->
            holder.itemView.context.loadImg(user.avatar.toString(), holder.v.avatarFriend)
            holder.itemView.context.loadImg(user.avatar.toString(), holder.v.avtSeen)
        }
        holder.itemView.setOnClickListener {
            onClickView?.invoke(conversation)
        }
    }

    /**
     * This function is used to handle the change in the conversation
     */
    private fun BaseViewHolder<ItemListChatBinding>.handleWhenConversationIsChanged(conversation: Conversation) {
        if (conversation.numberUnSeen > 0) {
            v.tvMessage.setTextColor(itemView.context.getColor(R.color.text_common))
        } else {
            v.tvMessage.setTextColor(itemView.context.getColor(R.color.grey_1))
        }

        if (conversation.isMessageFromFriend()) {
            v.newMessage.isVisible = !conversation.isSeenMessage()
            if (!conversation.isSeenMessage()) {
                showMultiMessage(conversation.numberUnSeen > 1)
                v.tvMultiMessage.text = conversation.numberUnSeen.toString()
            } else {
                hideNewMessage()
            }
            this.v.avtSeen.isVisible = false
        } else {
            hideNewMessage()
            v.avtSeen.isVisible = conversation.isSeenMessage()
        }
    }

    /**
     * This function is used to show or hide text multiple unread messages
     */
    private fun BaseViewHolder<ItemListChatBinding>.showMultiMessage(isShow: Boolean) {
        this.v.tvMultiMessage.isVisible = isShow
        this.v.singMessage.isVisible = !isShow
    }

    /**
     * This function is used to hide view new message
     */
    private fun BaseViewHolder<ItemListChatBinding>.hideNewMessage() {
        this.v.tvMultiMessage.isVisible = false
        this.v.singMessage.isVisible = false
    }
}