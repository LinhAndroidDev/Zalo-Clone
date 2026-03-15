package com.example.messageapp.adapter

import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemStickerBinding
import androidx.core.view.isVisible

class StickerAdapter : BaseAdapter<String, ItemStickerBinding>() {
    var onClickItem: ((String) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_sticker

    override fun onBindViewHolder(holder: BaseViewHolder<ItemStickerBinding>, position: Int) {
        holder.v.progressStickerLoading.isVisible = true

        Glide.with(holder.itemView.context)
            .asGif()
            .load(items[position])
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<GifDrawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.v.progressStickerLoading.isVisible = false
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any,
                    target: Target<GifDrawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.v.progressStickerLoading.isVisible = false
                    return false
                }
            })
            .into(holder.v.imgSticker)

        holder.itemView.setOnClickListener {
            onClickItem?.invoke(items[position])
        }
    }
}