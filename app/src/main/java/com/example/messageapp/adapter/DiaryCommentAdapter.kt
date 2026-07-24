package com.example.messageapp.adapter

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemDiaryCommentBinding
import com.example.messageapp.databinding.ItemDiaryReplyBinding
import com.example.messageapp.databinding.ItemDiaryReplyToggleBinding
import com.example.messageapp.model.DiaryCommentRow
import com.example.messageapp.model.DiaryPostComment
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.RelativeTimeFormatter

class DiaryCommentAdapter(
    private val onToggleLike: (DiaryPostComment) -> Unit,
    private val onReply: (DiaryPostComment) -> Unit,
    private val onToggleReplyLike: (DiaryPostComment, String) -> Unit,
    private val onReplyToReply: (DiaryPostComment, String) -> Unit,
    private val onToggleReplies: (commentId: String) -> Unit,
    private val onLongClick: (comment: DiaryPostComment, anchor: View) -> Unit,
) : ListAdapter<DiaryCommentRow, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_COMMENT = 0
        private const val TYPE_REPLY = 1
        private const val TYPE_TOGGLE = 2

        private val DIFF = object : DiffUtil.ItemCallback<DiaryCommentRow>() {
            override fun areItemsTheSame(old: DiaryCommentRow, new: DiaryCommentRow): Boolean =
                old.rowId == new.rowId

            override fun areContentsTheSame(old: DiaryCommentRow, new: DiaryCommentRow): Boolean =
                old == new
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is DiaryCommentRow.CommentRow -> TYPE_COMMENT
        is DiaryCommentRow.ReplyRow -> TYPE_REPLY
        is DiaryCommentRow.ToggleRepliesRow -> TYPE_TOGGLE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_COMMENT -> CommentVH(ItemDiaryCommentBinding.inflate(inflater, parent, false))
            TYPE_REPLY -> ReplyVH(ItemDiaryReplyBinding.inflate(inflater, parent, false))
            else -> ToggleVH(ItemDiaryReplyToggleBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is DiaryCommentRow.CommentRow -> (holder as CommentVH).bind(row.comment)
            is DiaryCommentRow.ReplyRow -> (holder as ReplyVH).bind(row.reply, row.parentCommentId)
            is DiaryCommentRow.ToggleRepliesRow -> (holder as ToggleVH).bind(row)
        }
    }

    inner class CommentVH(private val binding: ItemDiaryCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(c: DiaryPostComment) {
            val ctx = binding.root.context
            binding.tvAuthor.text = c.authorName.ifBlank { ctx.getString(R.string.diary_default_user_name) }
            binding.tvText.text = c.text
            binding.tvTime.text = RelativeTimeFormatter.format(ctx, c.createdAtMillis)
            ctx.loadImg(c.authorAvatarUrl, binding.imgAvatar, R.drawable.bg_grey_equal)

            binding.tvLikeCount.text = if (c.likeCount > 0) c.likeCount.toString() else ""
            binding.tvLikeCount.setTextColor(likeCountColor(ctx, c.likedByMe))
            bindLikeAction(
                layoutLikeAction = binding.layoutLikeAction,
                tvLike = binding.tvLike,
                imgLike = binding.imgLike,
                likeCount = c.likeCount,
                likedByMe = c.likedByMe,
            ) { onToggleLike(c) }
            binding.tvReply.setOnClickListener { onReply(c) }
            binding.root.setOnLongClickListener { onLongClick(c, binding.root); true }
        }
    }

    inner class ReplyVH(private val binding: ItemDiaryReplyBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(r: DiaryPostComment, parentCommentId: String) {
            val ctx = binding.root.context
            binding.tvAuthor.text = r.authorName.ifBlank { ctx.getString(R.string.diary_default_user_name) }
            binding.tvText.text = buildReplyText(ctx, r)
            binding.tvTime.text = RelativeTimeFormatter.format(ctx, r.createdAtMillis)
            ctx.loadImg(r.authorAvatarUrl, binding.imgAvatar, R.drawable.bg_grey_equal)

            binding.tvLikeCount.text = if (r.likeCount > 0) r.likeCount.toString() else ""
            binding.tvLikeCount.setTextColor(likeCountColor(ctx, r.likedByMe))
            bindLikeAction(
                layoutLikeAction = binding.layoutLikeAction,
                tvLike = binding.tvLike,
                imgLike = binding.imgLike,
                likeCount = r.likeCount,
                likedByMe = r.likedByMe,
            ) { onToggleReplyLike(r, parentCommentId) }
            binding.tvReply.setOnClickListener { onReplyToReply(r, parentCommentId) }
            binding.root.setOnLongClickListener { onLongClick(r, binding.root); true }
        }
    }

    /** Tô đậm + xanh phần "@Tên" ở đầu nội dung (mention kiểu Facebook/Zalo). */
    private fun buildReplyText(ctx: Context, r: DiaryPostComment): CharSequence {
        val mention = r.mentionedName.trim()
        if (mention.isBlank()) return r.text
        val token = "@$mention"
        if (!r.text.startsWith(token)) return r.text
        val builder = SpannableStringBuilder(r.text)
        builder.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.color_link)),
            0,
            token.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        builder.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            token.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return builder
    }

    inner class ToggleVH(private val binding: ItemDiaryReplyToggleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: DiaryCommentRow.ToggleRepliesRow) {
            val ctx = binding.root.context
            binding.tvToggleReplies.text = if (row.expanded) {
                ctx.getString(R.string.diary_hide_replies)
            } else {
                ctx.getString(R.string.diary_view_replies, row.replyCount)
            }
            binding.tvToggleReplies.setOnClickListener { onToggleReplies(row.commentId) }
        }
    }

    /**
     * Có like (likeCount > 0): hiện icon + chữ "Thích".
     * Chữ "Thích" xanh khi người dùng hiện tại đã like; xám khi chưa like.
     */
    private fun bindLikeAction(
        layoutLikeAction: View,
        tvLike: TextView,
        imgLike: ImageView,
        likeCount: Int,
        likedByMe: Boolean,
        onClick: () -> Unit,
    ) {
        val ctx = tvLike.context
        val hasLikes = likeCount > 0
        imgLike.isVisible = hasLikes
        if (hasLikes) {
            imgLike.setImageResource(R.drawable.ic_like)
        }
        tvLike.isVisible = true
        tvLike.setTextColor(likeLabelColor(ctx, likedByMe))
        val textMarginStart = if (hasLikes) {
            (4 * ctx.resources.displayMetrics.density).toInt()
        } else {
            0
        }
        (tvLike.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            lp.marginStart = textMarginStart
            tvLike.layoutParams = lp
        }
        layoutLikeAction.setOnClickListener { onClick() }
    }

    private fun likeLabelColor(context: Context, likedByMe: Boolean): Int =
        ContextCompat.getColor(
            context,
            if (likedByMe) R.color.color_link else R.color.text_secondary,
        )

    private fun likeCountColor(context: Context, likedByMe: Boolean): Int = likeLabelColor(context, likedByMe)
}
