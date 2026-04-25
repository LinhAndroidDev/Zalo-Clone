package com.example.messageapp.adapter

import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemSentRequestFriendBinding
import com.example.messageapp.model.FriendRequest

class SentRequestAdapter : BaseAdapter<FriendRequest, ItemSentRequestFriendBinding>() {

    var onCancel: ((FriendRequest) -> Unit)? = null
    var onItemClick: ((FriendRequest) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_sent_request_friend

    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemSentRequestFriendBinding>,
        position: Int
    ) {
        val request = items[position]
        Glide.with(holder.v.root)
            .load(request.toAvatar)
            .placeholder(R.drawable.bg_grey_equal)
            .into(holder.v.avatarFriend)
        holder.v.nameFriend.text = request.toName
        holder.v.root.setOnClickListener { onItemClick?.invoke(request) }
        holder.v.btnCancel.setOnClickListener { onCancel?.invoke(request) }
    }

    fun updateDiff(newList: List<FriendRequest>) {
        updateDiffList(
            newList,
            compareItem = { old, new -> old.requestId == new.requestId },
            compareContent = { old, new -> old == new }
        )
    }
}
