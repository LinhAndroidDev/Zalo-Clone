package com.example.messageapp.adapter

import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemSuggestFriendBinding
import com.example.messageapp.model.Friend
import com.example.messageapp.model.User

class SuggestFriendAdapter : BaseAdapter<User, ItemSuggestFriendBinding>() {
    var onClickItem: ((User) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_suggest_friend

    override fun onBindViewHolder(holder: BaseViewHolder<ItemSuggestFriendBinding>, position: Int) {
        val friend = items[position]
        holder.v.tvNameFriend.text = friend.name
        Glide.with(holder.v.root)
            .load(friend.avatar)
            .error(R.mipmap.ic_launcher)
            .into(holder.v.avatarFriend)

        holder.itemView.setOnClickListener {
            onClickItem?.invoke(friend)
        }
    }
}