package com.example.messageapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.databinding.HeaderPhoneBookBinding
import com.example.messageapp.databinding.ItemPhoneBookOnlineBinding
import com.example.messageapp.model.Friend

class PhoneBookOnlineAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var friends: List<Friend> = emptyList()
    var onClickFriend: ((Friend) -> Unit)? = null
    var onClickFriendRequest: (() -> Unit)? = null
    var onFilterChanged: ((PhoneBookFilter) -> Unit)? = null
    var friendRequestCount: Int = 0
    var totalFriendCount: Int = 0
    var onlineCount: Int = 0
    var filter: PhoneBookFilter = PhoneBookFilter.ONLINE

    inner class HeaderViewHolder(val v: HeaderPhoneBookBinding) : RecyclerView.ViewHolder(v.root)

    inner class ItemViewHolder(val v: ItemPhoneBookOnlineBinding) : RecyclerView.ViewHolder(v.root)

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.header_phone_book,
                    parent,
                    false,
                ),
            )
        } else {
            ItemViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_phone_book_online,
                    parent,
                    false,
                ),
            )
        }
    }

    override fun getItemCount(): Int = if (friends.isEmpty()) 1 else friends.size + 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            PhoneBookHeaderHelper.bind(
                binding = holder.v,
                totalFriendCount = totalFriendCount,
                onlineCount = onlineCount,
                filter = filter,
                friendRequestCount = friendRequestCount,
                onFilterChanged = { onFilterChanged?.invoke(it) },
                onClickFriendRequest = { onClickFriendRequest?.invoke() },
            )
            return
        }

        val friend = friends[position - 1]
        holder as ItemViewHolder
        holder.itemView.setOnClickListener { onClickFriend?.invoke(friend) }
        holder.v.nameFriend.text = friend.name
        Glide.with(holder.v.root)
            .load(friend.avatar)
            .into(holder.v.avatarFriend)
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }
}
