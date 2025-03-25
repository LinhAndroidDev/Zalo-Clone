package com.example.messageapp.adapter

import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemSearchFriendBinding
import com.example.messageapp.model.User
import com.example.messageapp.utils.loadImg

class SearchAdapter : BaseAdapter<User, ItemSearchFriendBinding>() {
    override fun getLayout(): Int = R.layout.item_search_friend

    override fun onBindViewHolder(holder: BaseViewHolder<ItemSearchFriendBinding>, position: Int) {
        val friend = items[position]
        holder.v.root.context.loadImg(
            friend.avatar.toString(),
            holder.v.avtFriend,
            R.drawable.bg_grey_equal
        )
        holder.v.nameFriend.text = friend.name
    }

    fun updateUserDiff(users: ArrayList<User>) {
        updateDiffList(users,
            compareItem = { old, new -> old.keyAuth == new.keyAuth },
            compareContent = { old, new -> old == new }
        )
    }
}