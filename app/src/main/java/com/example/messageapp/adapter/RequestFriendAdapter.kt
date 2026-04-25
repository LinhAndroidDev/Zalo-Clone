package com.example.messageapp.adapter

import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemRequestFriendBinding
import com.example.messageapp.model.FriendRequest

class RequestFriendAdapter : BaseAdapter<FriendRequest, ItemRequestFriendBinding>() {

    var onAccept: ((FriendRequest) -> Unit)? = null
    var onReject: ((String) -> Unit)? = null
    var onItemClick: ((FriendRequest) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_request_friend

    override fun onBindViewHolder(holder: BaseViewHolder<ItemRequestFriendBinding>, position: Int) {
        val request = items[position]
        Glide.with(holder.v.root)
            .load(request.fromAvatar)
            .placeholder(R.drawable.bg_grey_equal)
            .into(holder.v.avatarFriend)
        holder.v.nameFriend.text = request.fromName
        holder.v.root.setOnClickListener { onItemClick?.invoke(request) }
        holder.v.btnAgree.setOnClickListener { onAccept?.invoke(request) }
        holder.v.btnRefuse.setOnClickListener { onReject?.invoke(request.requestId) }
    }

    fun updateDiff(newList: List<FriendRequest>) {
        updateDiffList(
            newList,
            compareItem = { old, new -> old.requestId == new.requestId },
            compareContent = { old, new -> old == new }
        )
    }
}
