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

class ListChatAdapter(private val userId: String) : BaseAdapter<Conversation, ItemListChatBinding>() {

    var onClickView: ((Conversation) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_list_chat

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BaseViewHolder<ItemListChatBinding>, position: Int) {
        val conversation = items[position]
        holder.v.tvNameFriend.text = conversation.name
        if (conversation.numberUnSeen > 0) {
            holder.v.tvMessage.setTextColor(holder.itemView.context.getColor(R.color.text_common))
        } else {
            holder.v.tvMessage.setTextColor(holder.itemView.context.getColor(R.color.grey_1))
        }
        holder.v.tvMessage.text = "${conversation.person}: ${conversation.message}"
        holder.v.tvTime.text = DateUtils.convertTimeToHour(conversation.time)
        if (conversation.friendId == conversation.sender) {
            holder.v.newMessage.isVisible = !conversation.isSeenMessage()
            if (!conversation.isSeenMessage()) {
                holder.showMultiMessage(conversation.numberUnSeen > 1)
                holder.v.tvMultiMessage.text = conversation.numberUnSeen.toString()
            } else {
                holder.hideNewMessage()
            }
            holder.v.avtSeen.isVisible = false
        } else {
            if (conversation.sender == userId) {
                holder.hideNewMessage()
                holder.v.avtSeen.isVisible = conversation.isSeenMessage()
            }
        }
        FireBaseInstance.getInfoUser(conversation.friendId) { user ->
            holder.itemView.context.loadImg(user.avatar.toString(), holder.v.avatarFriend)
            holder.itemView.context.loadImg(user.avatar.toString(), holder.v.avtSeen)
        }
        holder.itemView.setOnClickListener {
            onClickView?.invoke(conversation)
        }
    }

    private fun BaseViewHolder<ItemListChatBinding>.showMultiMessage(isShow: Boolean) {
        this.v.tvMultiMessage.isVisible = isShow
        this.v.singMessage.isVisible = !isShow
    }

    private fun BaseViewHolder<ItemListChatBinding>.hideNewMessage() {
        this.v.tvMultiMessage.isVisible = false
        this.v.singMessage.isVisible = false
    }
}