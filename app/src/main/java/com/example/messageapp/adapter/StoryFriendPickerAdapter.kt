package com.example.messageapp.adapter

import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemStoryFriendPickerBinding
import com.example.messageapp.model.Friend
import com.example.messageapp.utils.FileUtils.loadImg

class StoryFriendPickerAdapter : BaseAdapter<Friend, ItemStoryFriendPickerBinding>() {

    var onToggle: ((friendId: String, checked: Boolean) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_story_friend_picker

    override fun onBindViewHolder(holder: BaseViewHolder<ItemStoryFriendPickerBinding>, position: Int) {
        val friend = items[position]
        val friendId = friend.keyAuth
        holder.v.tvName.text = friend.name
        holder.v.root.context.loadImg(friend.avatar, holder.v.imgAvatar, R.drawable.bg_grey_equal)
        holder.v.checkFriend.setOnCheckedChangeListener(null)
        holder.v.checkFriend.isChecked = friendId in checkedIds
        holder.v.checkFriend.setOnCheckedChangeListener { _, checked ->
            onToggle?.invoke(friendId, checked)
        }
        holder.v.root.setOnClickListener {
            holder.v.checkFriend.isChecked = !holder.v.checkFriend.isChecked
        }
    }

    private var checkedIds: Set<String> = emptySet()

    fun submit(friends: List<Friend>, selectedIds: Set<String>) {
        checkedIds = selectedIds
        updateDiffList(friends, { a, b -> a.keyAuth == b.keyAuth }, { a, b -> a == b })
    }
}
