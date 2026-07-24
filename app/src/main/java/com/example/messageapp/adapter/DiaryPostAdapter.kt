package com.example.messageapp.adapter

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemDiaryPostBinding
import com.example.messageapp.helper.StatusMediaGridLayout
import com.example.messageapp.model.DiaryPost
import com.example.messageapp.model.EmotionType
import com.example.messageapp.utils.EmotionBurstEffect
import com.example.messageapp.utils.FileUtils.loadImg
import androidx.core.net.toUri

class DiaryPostAdapter : BaseAdapter<DiaryPost, ItemDiaryPostBinding>() {

    var onOpenImagePreview: ((uris: List<Uri>, startIndex: Int) -> Unit)? = null
    var onToggleLike: ((DiaryPost) -> Unit)? = null
    var onSetReaction: ((DiaryPost, EmotionType) -> Unit)? = null
    var onOpenComments: ((DiaryPost) -> Unit)? = null
    var currentUserId: String = ""
    var onEditPost: ((DiaryPost) -> Unit)? = null
    var onDeletePost: ((DiaryPost) -> Unit)? = null
    var onOpenAuthorProfile: ((authorUserId: String) -> Unit)? = null

    private companion object {
        private const val MENU_EDIT = 1
        private const val MENU_DELETE = 2
    }

    override fun getLayout(): Int = R.layout.item_diary_post

    override fun onBindViewHolder(holder: BaseViewHolder<ItemDiaryPostBinding>, position: Int) {
        val post = items[position]
        val ctx = holder.v.root.context

        holder.v.tvAuthorName.text = post.authorName
        holder.v.tvTime.text = formatRelativeTime(ctx, post.createdAtMillis)

        val openAuthorProfile = {
            val id = post.authorUserId
            if (id.isNotBlank()) onOpenAuthorProfile?.invoke(id)
        }
        holder.v.imgAuthor.setOnClickListener { openAuthorProfile() }
        holder.v.layoutAuthorTap.setOnClickListener { openAuthorProfile() }

        if (post.content.isNotBlank()) {
            holder.v.tvContent.isVisible = true
            holder.v.tvContent.text = post.content
        } else {
            holder.v.tvContent.isVisible = false
        }

        val link = post.linkPreview
        val linkWrap = holder.v.wrapperDiaryLinkPreview
        if (link != null) {
            linkWrap.isVisible = true
            with(holder.v.diaryLinkPreviewCard) {
                tvLinkTitle.text = link.title
                val hasDesc = link.description.isNotBlank()
                tvLinkDescription.isVisible = hasDesc
                tvLinkDescription.text = link.description
                tvLinkHost.text = link.url.toUri().host ?: link.url
                if (!link.imageUrl.isNullOrBlank()) {
                    imgLinkPreview.isVisible = true
                    ctx.loadImg(link.imageUrl, imgLinkPreview, R.drawable.bg_grey_equal)
                } else {
                    imgLinkPreview.isVisible = true
                    imgLinkPreview.setImageResource(R.drawable.bg_grey_equal)
                }
            }
            linkWrap.setOnClickListener {
                try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, link.url.toUri()))
                } catch (_: Exception) {
                }
            }
        } else {
            linkWrap.isVisible = false
            linkWrap.setOnClickListener(null)
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
                },
            )
        }

        if (post.emotionCounts.values.any { it > 0 }) {
            holder.v.viewReleaseEmotion.isVisible = true
            holder.v.viewReleaseEmotion.updateFromCounts(post.emotionCounts)
        } else {
            holder.v.viewReleaseEmotion.isVisible = false
        }

        bindReactionIcon(holder.v.ivLike, post.myReactionType)
        holder.v.tvLikeLabel.text = if (post.myReactionType != null) {
            ""
        } else {
            ctx.getString(R.string.diary_like_label)
        }
        holder.v.tvLikeLabel.isVisible = post.myReactionType == null
        holder.v.ivLikeSmall.isVisible = post.likeCount > 0
        holder.v.tvLikeCount.text = post.likeCount.toString()
        holder.v.tvCommentCount.text = post.commentCount.toString()

        holder.v.layoutLike.setOnClickListener { onToggleLike?.invoke(post) }
        holder.v.layoutLike.setOnLongClickListener { anchor ->
            showReactionPopup(ctx, anchor, post)
            true
        }
        holder.v.layoutComment.setOnClickListener { onOpenComments?.invoke(post) }

        val isOwner = post.authorUserId == currentUserId && currentUserId.isNotBlank()
        holder.v.icMenu.isVisible = isOwner
        holder.v.icMenu.setOnClickListener { anchor ->
            if (!isOwner) return@setOnClickListener
            val popup = PopupMenu(ctx, anchor)
            popup.menu.add(0, MENU_EDIT, 0, ctx.getString(R.string.diary_post_menu_edit))
            popup.menu.add(0, MENU_DELETE, 0, ctx.getString(R.string.diary_post_menu_delete))
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_EDIT -> {
                        onEditPost?.invoke(post)
                        true
                    }
                    MENU_DELETE -> {
                        onDeletePost?.invoke(post)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun bindReactionIcon(imageView: ImageView, type: EmotionType?) {
        val ctx = imageView.context
        val layoutParams = imageView.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        if (type == EmotionType.LIKE) {
            layoutParams.width = ctx.resources.getDimensionPixelSize(R.dimen.emotion_like_icon_action_size)
            layoutParams.height = ctx.resources.getDimensionPixelSize(R.dimen.emotion_like_icon_action_size)
            layoutParams.marginStart = ctx.resources.getDimensionPixelSize(R.dimen.emotion_like_icon_action_margin_start)
        } else {
            layoutParams.width = ctx.resources.getDimensionPixelSize(R.dimen.emotion_icon_action_size)
            layoutParams.height = ctx.resources.getDimensionPixelSize(R.dimen.emotion_icon_action_size)
            layoutParams.marginStart = 0
        }
        imageView.layoutParams = layoutParams
        imageView.setImageDrawable(reactionDrawable(ctx, type))
    }

    private fun reactionDrawable(ctx: Context, type: EmotionType?) = when (type) {
        EmotionType.FAVOURITE -> AppCompatResources.getDrawable(ctx, R.drawable.emotion_favourite)
        EmotionType.LIKE -> AppCompatResources.getDrawable(ctx, R.drawable.emotion_like)
        EmotionType.LAUGH -> AppCompatResources.getDrawable(ctx, R.drawable.emotion_laugh)
        EmotionType.CRY -> AppCompatResources.getDrawable(ctx, R.drawable.emotion_cry)
        EmotionType.ANGRY -> AppCompatResources.getDrawable(ctx, R.drawable.emotion_angry)
        null -> AppCompatResources.getDrawable(ctx, R.drawable.ic_favourite_not_fill)
    }

    private fun showReactionPopup(ctx: Context, anchor: View, post: DiaryPost) {
        val popupView = LayoutInflater.from(ctx).inflate(R.layout.popup_diary_reaction, null)
        val popup = PopupWindow(
            popupView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            elevation = 12f
        }
        val activity = anchor.context as? android.app.Activity
        val pick: (EmotionType) -> Unit = { type ->
            onSetReaction?.invoke(post, type)
            activity?.let { EmotionBurstEffect.play(it, anchor, type) }
            popup.dismiss()
        }
        popupView.findViewById<View>(R.id.imgFavourite).setOnClickListener { pick(EmotionType.FAVOURITE) }
        popupView.findViewById<View>(R.id.imgLike).setOnClickListener { pick(EmotionType.LIKE) }
        popupView.findViewById<View>(R.id.imgLaugh).setOnClickListener { pick(EmotionType.LAUGH) }
        popupView.findViewById<View>(R.id.imgCry).setOnClickListener { pick(EmotionType.CRY) }
        popupView.findViewById<View>(R.id.imgAngry).setOnClickListener { pick(EmotionType.ANGRY) }
        popup.showAsDropDown(anchor, 0, -anchor.height * 2, Gravity.START)
    }

    private fun formatRelativeTime(context: Context, createdAtMillis: Long): String {
        val diff = System.currentTimeMillis() - createdAtMillis
        val sec = diff / 1000
        return when {
            sec < 60 -> context.getString(R.string.time_just_now)
            sec < 3600 -> context.getString(R.string.time_minutes_ago, sec / 60)
            sec < 86400 -> context.getString(R.string.time_hours_ago, sec / 3600)
            else -> context.getString(R.string.time_days_ago, sec / 86400)
        }
    }

    fun submitList(newList: List<DiaryPost>) {
        updateDiffList(
            newList,
            compareItem = { a, b -> a.id == b.id },
            compareContent = { a, b -> a == b },
        )
    }
}
