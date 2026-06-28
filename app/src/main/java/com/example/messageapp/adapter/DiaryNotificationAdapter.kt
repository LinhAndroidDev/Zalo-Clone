package com.example.messageapp.adapter

import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemDiaryNotificationBinding
import com.example.messageapp.domain.model.DiaryNotificationType
import com.example.messageapp.mapper.DiaryNotificationUiMapper
import com.example.messageapp.model.DiaryNotificationItem
import com.example.messageapp.utils.FileUtils.loadImg

class DiaryNotificationAdapter : BaseAdapter<DiaryNotificationItem, ItemDiaryNotificationBinding>() {

    var onItemClick: ((DiaryNotificationItem) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_diary_notification

    override fun onBindViewHolder(holder: BaseViewHolder<ItemDiaryNotificationBinding>, position: Int) {
        val item = items[position]
        val ctx = holder.v.root.context

        val fullText = "${item.actorName} ${item.actionText}"
        val spannable = SpannableString(fullText)
        if (item.actorName.isNotBlank()) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                item.actorName.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        holder.v.tvMessage.text = spannable

        val preview = when {
            item.commentPreviewText.isNotBlank() -> item.commentPreviewText
            item.postPreviewText.isNotBlank() -> item.postPreviewText
            else -> ""
        }
        holder.v.tvPreview.isVisible = preview.isNotBlank()
        holder.v.tvPreview.text = preview
        holder.v.tvTime.text = item.timeText

        ctx.loadImg(item.actorAvatarUrl, holder.v.imgAvatar, R.drawable.bg_grey_equal)

        val badgeRes = if (item.type == DiaryNotificationType.POST_REACTION) {
            DiaryNotificationUiMapper.reactionIconRes(item.reactionType)
        } else {
            DiaryNotificationUiMapper.actionIconRes(item.type)
        }
        holder.v.imgActionBadge.setImageResource(badgeRes)

        if (item.postThumbnailUrl.isNotBlank()) {
            holder.v.imgThumbnail.isVisible = true
            ctx.loadImg(item.postThumbnailUrl, holder.v.imgThumbnail, R.drawable.bg_grey_equal)
        } else {
            holder.v.imgThumbnail.isVisible = false
        }

        holder.v.rootRow.setBackgroundColor(
            ContextCompat.getColor(
                ctx,
                if (!item.read) R.color.blue_light else android.R.color.transparent,
            ),
        )
        holder.v.viewUnreadDot.isVisible = !item.read

        holder.v.root.setOnClickListener { onItemClick?.invoke(item) }
    }

    fun submitList(newList: List<DiaryNotificationItem>) {
        updateDiffList(
            newList,
            compareItem = { a, b -> a.id == b.id },
            compareContent = { a, b -> a == b },
        )
    }
}
