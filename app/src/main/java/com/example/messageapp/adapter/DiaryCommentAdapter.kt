package com.example.messageapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemDiaryCommentBinding
import com.example.messageapp.model.DiaryPostComment
import com.example.messageapp.utils.FileUtils.loadImg

class DiaryCommentAdapter : RecyclerView.Adapter<DiaryCommentAdapter.VH>() {

    private val items = mutableListOf<DiaryPostComment>()

    fun submitList(list: List<DiaryPostComment>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDiaryCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(private val binding: ItemDiaryCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(c: DiaryPostComment) {
            val ctx = binding.root.context
            binding.tvAuthor.text = c.authorName.ifBlank { ctx.getString(R.string.diary_default_user_name) }
            binding.tvText.text = c.text
            binding.tvTime.text = formatRelative(ctx, c.createdAtMillis)
            binding.root.context.loadImg(c.authorAvatarUrl, binding.imgAvatar, R.drawable.bg_grey_equal)
        }

        private fun formatRelative(context: Context, createdAtMillis: Long): String {
            val diff = System.currentTimeMillis() - createdAtMillis
            val sec = diff / 1000
            return when {
                sec < 60 -> context.getString(R.string.time_just_now)
                sec < 3600 -> context.getString(R.string.time_minutes_ago, sec / 60)
                sec < 86400 -> context.getString(R.string.time_hours_ago, sec / 3600)
                else -> context.getString(R.string.time_days_ago, sec / 86400)
            }
        }
    }
}
