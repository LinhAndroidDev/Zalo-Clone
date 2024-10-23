package com.example.messageapp.adapter

import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemListChatBinding
import com.example.messageapp.model.Friend

class ListChatAdapter : BaseAdapter<Friend, ItemListChatBinding>() {
    var onClickView: (() -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_list_chat

    override fun onBindViewHolder(holder: BaseViewHolder<ItemListChatBinding>, position: Int) {
        val friend = items[position]
        holder.v.tvNameFriend.text = friend.name
        if (position != 0) {
            holder.v.tvMessage.text = "Hello Bro"
        }
        Glide.with(holder.itemView.context)
            .load(friend.avatar)
            .circleCrop()
            .into(holder.v.avatarFriend)
        holder.itemView.setOnClickListener {
            onClickView?.invoke()
        }
    }
}