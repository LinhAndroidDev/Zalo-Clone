package com.example.messageapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemStatusSelectedMediaBinding
import com.example.messageapp.model.StatusMediaItem

class StatusSelectedMediaAdapter : RecyclerView.Adapter<StatusSelectedMediaAdapter.StatusMediaViewHolder>() {
    var items = mutableListOf<StatusMediaItem>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onClickRemove: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusMediaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemStatusSelectedMediaBinding.inflate(inflater, parent, false)
        return StatusMediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatusMediaViewHolder, position: Int) {
        val item = items[position]
        Glide.with(holder.itemView.context)
            .load(item.uri)
            .placeholder(R.drawable.bg_grey_equal)
            .error(R.drawable.bg_grey_equal)
            .into(holder.binding.imgSelectedMedia)

        holder.binding.btnRemoveMedia.setOnClickListener {
            onClickRemove?.invoke(position)
        }
    }

    override fun getItemCount(): Int = items.size

    class StatusMediaViewHolder(val binding: ItemStatusSelectedMediaBinding) :
        RecyclerView.ViewHolder(binding.root)
}

