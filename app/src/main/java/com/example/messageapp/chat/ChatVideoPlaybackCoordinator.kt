package com.example.messageapp.chat

import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
    private var handoffInProgress = false
    private var handoffVideoUrl: String? = null
    private var handoffWasPlaying = false
    private val minVisibleRatio = 0.6f

    private val playbackListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            if (!enabled || handoffInProgress || currentTarget == null) return
            player.seekTo(0)
            player.playWhenReady = true
            currentTarget?.attachPlayer(player)
            player.play()
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                refresh()
            }
        }
    }

    init {
        recyclerView.addOnScrollListener(scrollListener)
        player.addListener(playbackListener)
    }

    fun refresh(preferredVideoUrl: String? = null) {
        if (!enabled || handoffInProgress) return
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) {
            pauseInternal(keepPlaybackState = true)
            return
        }

        preferredVideoUrl?.let { url ->
            for (position in first..last) {
                val holder = recyclerView.findViewHolderForAdapterPosition(position) ?: continue
                findVideoTargets(holder.itemView).forEach { target ->
                    if (target.videoUrl == url && target.visibleRatio() > 0f) {
                        playTarget(target)
                        return
                    }
                }
            }
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
            pauseInternal(keepPlaybackState = true)
            return
        }
        if (bestTarget !== currentTarget) {
            playTarget(bestTarget!!)
        } else if (!player.isPlaying) {
            resumeCurrentTarget()
        }
    }

    fun isHandoffInProgress(): Boolean = handoffInProgress

    fun prepareHandoffToFullscreen(clickedVideoUrl: String): HandoffSnapshot {
        handoffInProgress = true
        enabled = false

        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val videoUrl = currentTarget?.videoUrl ?: currentVideoUrl()
        handoffVideoUrl = clickedVideoUrl
        handoffWasPlaying = player.isPlaying || player.playWhenReady
        val sameMediaAlreadyLoaded = videoUrl != null &&
            currentVideoUrl() == videoUrl &&
            player.playbackState != Player.STATE_IDLE

        currentTarget?.detachPlayer()
        currentTarget = null
        player.clearVideoSurface()
        player.volume = 1f

        return HandoffSnapshot(
            videoUrl = videoUrl,
            positionMs = positionMs,
            sameMediaAlreadyLoaded = sameMediaAlreadyLoaded,
        )
    }

    fun resumeFromFullscreen() {
        handoffInProgress = false
        enabled = true
        player.volume = 0f

        val (returnUrl, shouldResumeFromActivity) = ChatVideoPlayerHolder.consumeListResumeState()
        val preferredUrl = returnUrl ?: handoffVideoUrl ?: currentVideoUrl()
        val resumePlayback = shouldResumeFromActivity ||
            handoffWasPlaying ||
            player.isPlaying ||
            player.playWhenReady

        handoffVideoUrl = null
        handoffWasPlaying = false

        player.playWhenReady = resumePlayback
        if (resumePlayback && !player.isPlaying) {
            player.play()
        }

        recyclerView.post {
            attachAndResume(preferredVideoUrl = preferredUrl, resumePlayback = resumePlayback)
        }
    }

    private fun attachAndResume(preferredVideoUrl: String?, resumePlayback: Boolean, retryCount: Int = 0) {
        refresh(preferredVideoUrl = preferredVideoUrl)
        if (currentTarget != null) {
            if (resumePlayback) {
                resumeCurrentTarget()
            } else {
                currentTarget?.attachPlayer(player)
            }
            return
        }
        if (preferredVideoUrl != null && retryCount < 3) {
            recyclerView.postDelayed({
                attachAndResume(preferredVideoUrl, resumePlayback, retryCount + 1)
            }, 100L)
        }
    }

    fun pause() {
        if (handoffInProgress) return
        enabled = false
        pauseInternal()
    }

    fun resume() {
        if (handoffInProgress) return
        enabled = true
        refresh()
    }

    fun onViewRecycled(view: View) {
        if (handoffInProgress) return
        if (currentTarget != null && containsView(view, currentTarget!!)) {
            pauseInternal()
        }
    }

    fun release() {
        enabled = false
        handoffInProgress = false
        recyclerView.removeOnScrollListener(scrollListener)
        player.removeListener(playbackListener)
        pauseInternal()
    }

    private fun playTarget(target: ChatVideoPlayTarget) {
        currentTarget?.detachPlayer()
        currentTarget = target

        val currentUri = currentVideoUrl().orEmpty()
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
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
        }
        target.attachPlayer(player)
        player.play()
    }

    private fun pauseInternal(keepPlaybackState: Boolean = false) {
        if (!keepPlaybackState) {
            player.pause()
            player.playWhenReady = false
        }
        currentTarget?.detachPlayer()
        currentTarget = null
    }

    private fun currentVideoUrl(): String? =
        player.currentMediaItem?.localConfiguration?.uri?.toString()

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
