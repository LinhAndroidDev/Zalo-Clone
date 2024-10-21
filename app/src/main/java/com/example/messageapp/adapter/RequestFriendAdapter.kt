package com.example.messageapp.adapter

import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemRequestFriendBinding
import com.example.messageapp.model.Friend

class RequestFriendAdapter : BaseAdapter<Friend, ItemRequestFriendBinding>() {

    override fun getLayout(): Int = R.layout.item_request_friend

    override fun onBindViewHolder(holder: BaseViewHolder<ItemRequestFriendBinding>, position: Int) {
        val friend = items[position]
        Glide.with(holder.v.root)
            .load(friend.avatar)
            .into(holder.v.avatarFriend)
        holder.v.nameFriend.text = friend.name
    }
}