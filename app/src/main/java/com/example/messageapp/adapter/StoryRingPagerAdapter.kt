package com.example.messageapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.messageapp.databinding.ItemStoryRingPageBinding
import com.example.messageapp.model.StoryRingItem

class StoryRingPagerAdapter : RecyclerView.Adapter<StoryRingPagerAdapter.RingViewHolder>() {

    private var rings: List<StoryRingItem> = emptyList()

    var onInnerPageSelected: ((ringIndex: Int, storyIndex: Int) -> Unit)? = null
    var onRingBound: ((ringIndex: Int, innerPager: ViewPager2) -> Unit)? = null
    var suppressInnerPageChange = false

    fun submitRings(
        newRings: List<StoryRingItem>,
        hostRecyclerView: RecyclerView? = null,
        onCommitted: (() -> Unit)? = null,
    ) {
        rings = newRings
        val commit: () -> Unit = { onCommitted?.invoke() }
        val apply: () -> Unit = {
            notifyDataSetChanged()
            if (hostRecyclerView != null) {
                hostRecyclerView.post(commit)
            } else {
                commit()
            }
        }
        val host = hostRecyclerView
        if (host != null &&
            (host.isComputingLayout || host.scrollState != RecyclerView.SCROLL_STATE_IDLE)
        ) {
            host.post(apply)
        } else {
            apply()
        }
    }

    fun ringAt(position: Int): StoryRingItem? = rings.getOrNull(position)

    fun findInnerPager(outerPager: ViewPager2, ringIndex: Int): ViewPager2? {
        val recyclerView = outerPager.getChildAt(0) as? RecyclerView ?: return null
        val holder = recyclerView.findViewHolderForAdapterPosition(ringIndex) as? RingViewHolder
        return holder?.innerPager
    }

    fun setInnerCurrentItem(
        outerPager: ViewPager2,
        ringIndex: Int,
        storyIndex: Int,
        smooth: Boolean,
    ) {
        findInnerPager(outerPager, ringIndex)?.setCurrentItem(storyIndex, smooth)
    }

    override fun getItemCount(): Int = rings.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RingViewHolder {
        val binding = ItemStoryRingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return RingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RingViewHolder, position: Int) {
        holder.bind(rings[position], position)
    }

    override fun onViewRecycled(holder: RingViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    inner class RingViewHolder(
        private val binding: ItemStoryRingPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        val innerPager: ViewPager2
            get() = binding.storyPagerInRing

        private var boundRingIndex: Int = RecyclerView.NO_POSITION
        private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

        fun bind(ring: StoryRingItem, ringIndex: Int) {
            unbind()
            boundRingIndex = ringIndex
            val stories = ring.stories.sortedBy { it.createdAtMillis }
            val innerPager = binding.storyPagerInRing
            innerPager.isUserInputEnabled = false
            innerPager.offscreenPageLimit = 1

            val innerAdapter = (innerPager.adapter as? StoryViewerPagerAdapter)
                ?: StoryViewerPagerAdapter().also { innerPager.adapter = it }
            innerAdapter.submitStories(stories)

            pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (suppressInnerPageChange) return
                    if (boundRingIndex != ringIndex) return
                    onInnerPageSelected?.invoke(ringIndex, position)
                }
            }.also { innerPager.registerOnPageChangeCallback(it) }
            onRingBound?.invoke(ringIndex, innerPager)
        }

        fun unbind() {
            pageChangeCallback?.let { callback ->
                binding.storyPagerInRing.unregisterOnPageChangeCallback(callback)
            }
            pageChangeCallback = null
            boundRingIndex = RecyclerView.NO_POSITION
        }
    }
}
