package com.example.messageapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.databinding.ItemStoryPageBinding
import com.example.messageapp.model.StoryItem

class StoryViewerPagerAdapter : RecyclerView.Adapter<StoryViewerPagerAdapter.PageViewHolder>() {

    private var stories: List<StoryItem> = emptyList()

    fun submitStories(newStories: List<StoryItem>) {
        stories = newStories
        notifyDataSetChanged()
    }

    fun storyAt(position: Int): StoryItem? = stories.getOrNull(position)

    override fun getItemCount(): Int = stories.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemStoryPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.binding.musicSticker.clearSticker()
    }

    class PageViewHolder(val binding: ItemStoryPageBinding) : RecyclerView.ViewHolder(binding.root)
}
