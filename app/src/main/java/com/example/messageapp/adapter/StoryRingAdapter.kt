package com.example.messageapp.adapter

import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemStoryRingBinding
import com.example.messageapp.model.StoryRingItem
import com.example.messageapp.utils.FileUtils.loadImg

class StoryRingAdapter : BaseAdapter<StoryRingItem, ItemStoryRingBinding>() {

    var onMyStoryClick: ((StoryRingItem?) -> Unit)? = null
    var onFriendStoryClick: ((StoryRingItem) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_story_ring

    override fun onBindViewHolder(holder: BaseViewHolder<ItemStoryRingBinding>, position: Int) {
        val item = items[position]
        val context = holder.v.root.context

        holder.v.tvName.text = if (item.isMe) {
            context.getString(R.string.story_my_ring_label)
        } else {
            item.authorName.ifBlank { item.authorId.takeLast(6) }
        }

        context.loadImg(item.authorAvatarUrl, holder.v.imgAvatar, R.drawable.bg_grey_equal)

        val hasStories = item.stories.isNotEmpty()
        holder.v.storyRingBorder.isVisible = when {
            !hasStories -> false
            item.isMe -> true
            else -> item.hasUnseen
        }
        holder.v.imgAddBadge.isVisible = item.isMe

        if (hasStories) {
            val unseenStyle = item.isMe || item.hasUnseen
            holder.v.storyRingBorder.setBackgroundResource(
                if (unseenStyle) R.drawable.bg_circle_gradient_stroke_grey
                else R.drawable.bg_circle_stroke_1,
            )
        }

        holder.v.root.setOnClickListener {
            if (item.isMe) {
                onMyStoryClick?.invoke(item.takeIf { hasStories })
            } else if (hasStories) {
                onFriendStoryClick?.invoke(item)
            }
        }
    }

    fun updateDiff(newList: List<StoryRingItem>) {
        updateDiffList(newList, { a, b -> a.authorId == b.authorId }, { a, b -> a == b })
    }
}
