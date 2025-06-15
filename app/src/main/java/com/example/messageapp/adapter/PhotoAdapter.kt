package com.example.messageapp.adapter

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemPhotoPageBinding
import com.example.messageapp.utils.FileUtils.loadImg

class PhotoAdapter(private val context: Context, private val transitionName: String, private val indexOfPhoto: Int) : BaseAdapter<String, ItemPhotoPageBinding>() {
    var onClickPhoto: (() -> Unit)? = null
    override fun getLayout(): Int = R.layout.item_photo_page

    override fun onBindViewHolder(holder: BaseViewHolder<ItemPhotoPageBinding>, position: Int) {
        holder.v.imageView.transitionName = if (position == indexOfPhoto) transitionName else ""
        if (position == indexOfPhoto) {
            Glide.with(context)
                .load(items[position])
                .into(holder.v.imageView.apply {
                    doOnPreDraw {
                        (context as AppCompatActivity).supportStartPostponedEnterTransition()
                    }
                })
        } else {
            context.loadImg(items[position], holder.v.imageView)
        }
        holder.itemView.setOnClickListener {
            onClickPhoto?.invoke()
        }
    }

}