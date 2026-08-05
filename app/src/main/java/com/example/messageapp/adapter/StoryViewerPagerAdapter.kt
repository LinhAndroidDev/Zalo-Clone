package com.example.messageapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.databinding.ItemStoryPageBinding
import com.example.messageapp.model.StoryViewerPage

class StoryViewerPagerAdapter : RecyclerView.Adapter<StoryViewerPagerAdapter.PageViewHolder>() {

    private var pages: List<StoryViewerPage> = emptyList()

    fun submitPages(newPages: List<StoryViewerPage>) {
        pages = newPages
        notifyDataSetChanged()
    }

    fun pageAt(position: Int): StoryViewerPage? = pages.getOrNull(position)

    override fun getItemCount(): Int = pages.size

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
