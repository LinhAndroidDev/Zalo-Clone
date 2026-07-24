package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemGalleryBinding
import com.example.messageapp.model.GalleryItem
import com.example.messageapp.utils.FileUtils.loadImg

class GalleryAdapter(
    private val context: Context,
    private val singleSelect: Boolean = false,
    private val onItemChecked: (GalleryItem, Boolean) -> Unit = { _, _ -> },
    private val onItemSelected: (GalleryItem) -> Unit = {},
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    private var items = listOf<GalleryItem>()
    private val selectedItems = mutableSetOf<GalleryItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<GalleryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class GalleryViewHolder(private val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GalleryItem, position: Int) {
            binding.apply {
                context.loadImg(item.path, imgGallery)

                icVideo.visibility = if (item.isVideo) ViewGroup.VISIBLE else ViewGroup.GONE
                tvDuration.visibility = if (item.isVideo) ViewGroup.VISIBLE else ViewGroup.GONE
                bgCoverVideo.visibility = if (item.isVideo) ViewGroup.VISIBLE else ViewGroup.GONE

                if (item.isVideo) {
                    tvDuration.text = item.duration
                }

                if (singleSelect) {
                    icCheck.isVisible = false
                    bgCoverCheck.isVisible = false
                    itemView.setOnClickListener { onItemSelected(item) }
                    return
                }

                val isSelected = selectedItems.contains(item)
                if (isSelected) {
                    icCheck.setImageResource(R.drawable.ic_check_gallery)
                    bgCoverCheck.isVisible = true
                } else {
                    icCheck.setImageResource(R.drawable.ic_un_check_gallery)
                    bgCoverCheck.isVisible = false
                }
                icCheck.visibility = ViewGroup.VISIBLE

                itemView.setOnClickListener {
                    val newCheckedState = !isSelected
                    if (newCheckedState) {
                        selectedItems.add(item)
                    } else {
                        selectedItems.remove(item)
                    }
                    icCheck.setImageResource(
                        if (newCheckedState) R.drawable.ic_check_gallery else R.drawable.ic_un_check_gallery,
                    )
                    onItemChecked(item, newCheckedState)
                    notifyItemChanged(position)
                }
            }
        }
    }
}
