package com.example.messageapp.chat

import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.custom.ChatVideoCellView

class ChatVideoPlaybackCoordinator(
    private val recyclerView: RecyclerView,
    private val player: ExoPlayer,
) {
    private var currentTarget: ChatVideoPlayTarget? = null
    private var enabled = true
    private val minVisibleRatio = 0.6f

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                refresh()
            }
        }
    }

    init {
        recyclerView.addOnScrollListener(scrollListener)
    }

    fun refresh() {
        if (!enabled) return
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) {
            pauseInternal()
            return
        }

        var bestTarget: ChatVideoPlayTarget? = null
        var bestRatio = 0f
        for (position in first..last) {
            val holder = recyclerView.findViewHolderForAdapterPosition(position) ?: continue
            findVideoTargets(holder.itemView).forEach { target ->
                val ratio = target.visibleRatio()
                if (ratio >= minVisibleRatio && ratio > bestRatio) {
                    bestRatio = ratio
                    bestTarget = target
                }
            }
        }

        if (bestTarget == null) {
            pauseInternal()
            return
        }
        if (bestTarget !== currentTarget) {
            playTarget(bestTarget!!)
        } else if (!player.isPlaying) {
            resumeCurrentTarget()
        }
    }

    fun pause() {
        enabled = false
        pauseInternal()
    }

    fun resume() {
        enabled = true
        refresh()
    }

    fun onViewRecycled(view: View) {
        if (currentTarget != null && containsView(view, currentTarget!!)) {
            pauseInternal()
        }
    }

    fun release() {
        enabled = false
        recyclerView.removeOnScrollListener(scrollListener)
        pauseInternal()
    }

    private fun playTarget(target: ChatVideoPlayTarget) {
        currentTarget?.detachPlayer()
        currentTarget = target

        val currentUri = player.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
        if (currentUri != target.videoUrl) {
            player.setMediaItem(MediaItem.fromUri(target.videoUrl))
            player.prepare()
        }
        player.volume = 0f
        player.playWhenReady = true
        target.attachPlayer(player)
        player.play()
    }

    private fun resumeCurrentTarget() {
        val target = currentTarget ?: return
        player.volume = 0f
        player.playWhenReady = true
        target.attachPlayer(player)
        player.play()
    }

    private fun pauseInternal() {
        player.pause()
        player.playWhenReady = false
        currentTarget?.detachPlayer()
        currentTarget = null
    }

    private fun findVideoTargets(root: View): List<ChatVideoPlayTarget> {
        val targets = mutableListOf<ChatVideoPlayTarget>()
        collectVideoTargets(root, targets)
        return targets
    }

    private fun collectVideoTargets(view: View, out: MutableList<ChatVideoPlayTarget>) {
        if (view is ChatVideoPlayTarget) {
            out.add(view)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectVideoTargets(view.getChildAt(i), out)
            }
        }
    }

    private fun containsView(root: View, target: ChatVideoPlayTarget): Boolean {
        if (root === target) return true
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                if (containsView(root.getChildAt(i), target)) return true
            }
        }
        return false
    }
}
