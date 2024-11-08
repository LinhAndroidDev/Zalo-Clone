package com.example.messageapp.adapter

import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemSuggestFriendBinding
import com.example.messageapp.model.User
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.loadImg

class SuggestFriendAdapter : BaseAdapter<User, ItemSuggestFriendBinding>() {
    var onClickItem: ((User) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_suggest_friend

    override fun onBindViewHolder(holder: BaseViewHolder<ItemSuggestFriendBinding>, position: Int) {
        val friend = items[position]
        holder.v.tvNameFriend.text = friend.name
        FireBaseInstance.getInfoUser(friend.keyAuth.toString()) { user ->
            holder.itemView.context.loadImg(user.avatar.toString(), holder.v.avatarFriend)
        }

        holder.itemView.setOnClickListener {
            onClickItem?.invoke(friend)
        }
    }
}