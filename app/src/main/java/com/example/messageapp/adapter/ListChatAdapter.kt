package com.example.messageapp.adapter

import android.annotation.SuppressLint
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemListChatBinding
import com.example.messageapp.model.Conversation
import com.example.messageapp.utils.DateUtils

class ListChatAdapter : BaseAdapter<Conversation, ItemListChatBinding>() {
    var onClickView: ((Conversation) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_list_chat

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BaseViewHolder<ItemListChatBinding>, position: Int) {
        val conversation = items[position]
        holder.v.tvNameFriend.text = conversation.name
        holder.v.tvMessage.text = "${conversation.person}: ${conversation.message}"
        holder.v.tvTime.text = DateUtils.convertTimeToHour(conversation.time)
        Glide.with(holder.itemView.context)
            .load(conversation.friendImage)
            .circleCrop()
            .error(R.mipmap.ic_launcher)
            .into(holder.v.avatarFriend)
        holder.itemView.setOnClickListener {
            onClickView?.invoke(conversation)
        }
    }
}