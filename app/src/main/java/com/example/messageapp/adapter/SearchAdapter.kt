package com.example.messageapp.adapter

import android.graphics.Color
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemSearchFriendBinding
import com.example.messageapp.model.User
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.viewmodel.UserWithStatus

class SearchAdapter : BaseAdapter<UserWithStatus, ItemSearchFriendBinding>() {

    var onAddFriend: ((User) -> Unit)? = null
    var onCancelFriend: ((User) -> Unit)? = null
    var onChat: ((User) -> Unit)? = null
    var onItemClick: ((User) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_search_friend

    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemSearchFriendBinding>,
        position: Int
    ) {
        val (user, status) = items[position]
        holder.v.root.context.loadImg(
            user.avatar.orEmpty(),
            holder.v.avtFriend,
            R.drawable.bg_grey_equal
        )
        holder.v.nameFriend.text = user.name
        holder.v.root.setOnClickListener { onItemClick?.invoke(user) }

        val ctx = holder.v.root.context
        when (status) {
            "friend" -> {
                holder.v.btnAction.text = ctx.getString(R.string.message)
                holder.v.btnAction.setTextColor(ctx.getColor(R.color.blue1))
                holder.v.btnAction.setBackgroundResource(R.drawable.bg_corner_25_blue_light)
                holder.v.btnAction.backgroundTintList = null
                holder.v.btnAction.setOnClickListener { onChat?.invoke(user) }
            }
            "pending_sent" -> {
                holder.v.btnAction.text = ctx.getString(R.string.cancel_friend_request)
                holder.v.btnAction.setTextColor(ctx.getColor(R.color.text_grey))
                holder.v.btnAction.setBackgroundResource(R.drawable.bg_corner_25_stroke_grey)
                holder.v.btnAction.backgroundTintList = null
                holder.v.btnAction.setOnClickListener { onCancelFriend?.invoke(user) }
            }
            "pending_received" -> {
                holder.v.btnAction.text = ctx.getString(R.string.agree)
                holder.v.btnAction.setTextColor(ctx.getColor(R.color.blue1))
                holder.v.btnAction.setBackgroundResource(R.drawable.bg_corner_25_blue_light)
                holder.v.btnAction.backgroundTintList = null
                holder.v.btnAction.setOnClickListener { onAddFriend?.invoke(user) }
            }
            else -> {
                holder.v.btnAction.text = ctx.getString(R.string.add_friend)
                holder.v.btnAction.setTextColor(ctx.getColor(R.color.blue1))
                holder.v.btnAction.setBackgroundResource(R.drawable.bg_corner_25_blue_light)
                holder.v.btnAction.backgroundTintList = null
                holder.v.btnAction.setOnClickListener { onAddFriend?.invoke(user) }
            }
        }
    }

    fun updateUserDiff(newList: List<UserWithStatus>) {
        updateDiffList(
            newList,
            compareItem = { old, new -> old.user.keyAuth == new.user.keyAuth },
            compareContent = { old, new -> old == new }
        )
    }
}
