package com.example.messageapp.adapter

import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemStoryMusicBinding
import com.example.messageapp.model.MusicTrackItem
import com.example.messageapp.utils.FileUtils.loadImg

class StoryMusicAdapter : BaseAdapter<MusicTrackItem, ItemStoryMusicBinding>() {

    var onTrackClick: ((MusicTrackItem) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_story_music

    override fun onBindViewHolder(holder: BaseViewHolder<ItemStoryMusicBinding>, position: Int) {
        val item = items[position]
        holder.v.tvTrackName.text = item.name
        holder.v.tvArtistName.text = item.artistName
        holder.v.root.context.loadImg(item.imageUrl, holder.v.imgTrack, R.drawable.bg_grey_equal)
        holder.v.root.setOnClickListener { onTrackClick?.invoke(item) }
    }

    fun updateDiff(newList: List<MusicTrackItem>) {
        updateDiffList(newList, { a, b -> a.id == b.id }, { a, b -> a == b })
    }

    fun appendTracks(tracks: List<MusicTrackItem>) {
        addItems(ArrayList(tracks))
    }
}
