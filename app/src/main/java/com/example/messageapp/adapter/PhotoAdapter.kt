package com.example.messageapp.adapter

import android.content.Context
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemPhotoPageBinding
import com.example.messageapp.utils.loadImg

class PhotoAdapter(private val context: Context) : BaseAdapter<String, ItemPhotoPageBinding>() {
    var onClickPhoto: (() -> Unit)? = null
    override fun getLayout(): Int = R.layout.item_photo_page

    override fun onBindViewHolder(holder: BaseViewHolder<ItemPhotoPageBinding>, position: Int) {
        context.loadImg(items[position], holder.v.imageView)
        holder.itemView.setOnClickListener {
            onClickPhoto?.invoke()
        }
    }

}