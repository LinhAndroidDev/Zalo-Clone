package com.example.messageapp.adapter

import android.net.Uri
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemDiaryPostBinding
import com.example.messageapp.helper.StatusMediaGridLayout
import com.example.messageapp.model.DiaryPost
import com.example.messageapp.utils.FileUtils.loadImg
import androidx.core.net.toUri

class DiaryPostAdapter : BaseAdapter<DiaryPost, ItemDiaryPostBinding>() {

    /** Opens full-screen image preview (same dialog as Status). */
    var onOpenImagePreview: ((uris: List<Uri>, startIndex: Int) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_diary_post

    override fun onBindViewHolder(holder: BaseViewHolder<ItemDiaryPostBinding>, position: Int) {
        val post = items[position]
        val ctx = holder.v.root.context

        holder.v.tvAuthorName.text = post.authorName
        holder.v.tvTime.text = formatRelativeTime(post.createdAtMillis)

        if (post.content.isNotBlank()) {
            holder.v.tvContent.isVisible = true
            holder.v.tvContent.text = post.content
        } else {
            holder.v.tvContent.isVisible = false
        }

        ctx.loadImg(post.authorAvatarUrl, holder.v.imgAuthor, R.drawable.bg_grey_equal)

        val grid = holder.v.frameMediaGrid
        if (post.imageUris.isEmpty()) {
            grid.isVisible = false
        } else {
            grid.isVisible = true
            val uris = post.imageUris.map { it.toUri() }
            StatusMediaGridLayout.render(
                context = ctx,
                container = grid,
                uris = uris,
                spacingPx = StatusMediaGridLayout.spacingPxDefault(ctx),
                showRemoveControls = false,
                onRemove = null,
                onOpenPreview = { index ->
                    onOpenImagePreview?.invoke(uris, index)
                }
            )
        }
    }

    private fun formatRelativeTime(createdAtMillis: Long): String {
        val diff = System.currentTimeMillis() - createdAtMillis
        val sec = diff / 1000
        return when {
            sec < 60 -> "Vừa xong"
            sec < 3600 -> "${sec / 60} phút trước"
            sec < 86400 -> "${sec / 3600} giờ trước"
            else -> "${sec / 86400} ngày trước"
        }
    }

    fun submitList(newList: List<DiaryPost>) {
        updateDiffList(
            newList,
            compareItem = { a, b -> a.id == b.id },
            compareContent = { a, b -> a == b }
        )
    }
}
