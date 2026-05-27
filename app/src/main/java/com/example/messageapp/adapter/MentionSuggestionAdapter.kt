package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemMentionSuggestionBinding
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.MentionHelper

class MentionSuggestionAdapter(
    private val onSelect: (MentionHelper.MentionCandidate) -> Unit,
) : RecyclerView.Adapter<MentionSuggestionAdapter.VH>() {

    private val items = ArrayList<MentionHelper.MentionCandidate>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(candidates: List<MentionHelper.MentionCandidate>) {
        items.clear()
        items.addAll(candidates)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMentionSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemMentionSuggestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(candidate: MentionHelper.MentionCandidate) {
            binding.tvName.text = candidate.displayName
            if (candidate.isAll) {
                binding.avatar.setImageResource(R.drawable.ic_a)
            } else if (candidate.avatar.isNotBlank()) {
                binding.root.context.loadImg(candidate.avatar, binding.avatar)
            } else {
                binding.avatar.setImageResource(R.drawable.bg_grey_equal)
            }
            binding.root.setOnClickListener { onSelect(candidate) }
        }
    }
}
