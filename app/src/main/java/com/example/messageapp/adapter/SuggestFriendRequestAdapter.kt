package com.example.messageapp.adapter

import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemSuggestRequestFriendBinding
import com.example.messageapp.model.Friend

class SuggestFriendRequestAdapter : BaseAdapter<Friend, ItemSuggestRequestFriendBinding>() {
    override fun getLayout(): Int = R.layout.item_suggest_request_friend

    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemSuggestRequestFriendBinding>,
        position: Int
    ) {
        val friend = items[position]
        Glide.with(holder.v.root)
            .load(friend.avatar)
            .into(holder.v.avatarFriend)
        holder.v.tvNameFriend.text = friend.name
    }
}