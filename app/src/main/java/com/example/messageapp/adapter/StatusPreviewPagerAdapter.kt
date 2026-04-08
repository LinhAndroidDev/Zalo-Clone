package com.example.messageapp.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemStatusPreviewImageBinding

class StatusPreviewPagerAdapter(
    private val items: List<Uri>
) : RecyclerView.Adapter<StatusPreviewPagerAdapter.PreviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val binding =
            ItemStatusPreviewImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(items[position])
            .placeholder(R.drawable.bg_grey_equal)
            .error(R.drawable.bg_grey_equal)
            .into(holder.binding.imgPreview)
    }

    override fun getItemCount(): Int = items.size

    class PreviewViewHolder(val binding: ItemStatusPreviewImageBinding) :
        RecyclerView.ViewHolder(binding.root)
}

