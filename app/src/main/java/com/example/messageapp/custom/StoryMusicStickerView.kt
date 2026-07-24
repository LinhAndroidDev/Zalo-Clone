package com.example.messageapp.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.messageapp.R
import com.example.messageapp.databinding.ViewStoryMusicStickerBinding
import com.example.messageapp.model.MusicTrackItem
import com.example.messageapp.utils.FileUtils.loadImg
import kotlin.math.abs

class StoryMusicStickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewStoryMusicStickerBinding.inflate(
        LayoutInflater.from(context),
        this,
    )

    private var musicPlayer: ExoPlayer? = null
    private var isPlaying = false
    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downRawX = 0f
    private var downRawY = 0f
    private var downViewX = 0f
    private var downViewY = 0f
    private var isDragging = false

    var isDraggable = true
    var previewAudioEnabled = true

    init {
        binding.btnPlayContainer.setOnClickListener { togglePlayback() }
        setupDrag()
    }

    fun bindTrack(track: MusicTrackItem) {
        isVisible = true
        binding.btnPlayContainer.isVisible = previewAudioEnabled
        context.loadImg(track.imageUrl, binding.imgAlbum, R.drawable.bg_grey_equal)
        if (previewAudioEnabled) {
            prepareMusic(track.audioUrl)
        } else {
            stopAndReleaseMusic()
        }
        updatePlayIcon()
    }

    fun clearSticker() {
        stopAndReleaseMusic()
        isVisible = false
    }

    fun centerInParent() {
        applyNormalizedPosition(0.5f, 0.5f)
    }

    fun getNormalizedPosition(): Pair<Float, Float> {
        val parentView = parent as? FrameLayout ?: return 0.5f to 0.5f
        if (width == 0 || height == 0 || parentView.width == 0 || parentView.height == 0) {
            return 0.5f to 0.5f
        }
        val maxX = (parentView.width - width).toFloat().coerceAtLeast(1f)
        val maxY = (parentView.height - height).toFloat().coerceAtLeast(1f)
        return (x / maxX).coerceIn(0f, 1f) to (y / maxY).coerceIn(0f, 1f)
    }

    fun applyNormalizedPosition(normalizedX: Float, normalizedY: Float) {
        val nx = normalizedX.coerceIn(0f, 1f)
        val ny = normalizedY.coerceIn(0f, 1f)
        post {
            val parentView = parent as? FrameLayout ?: return@post
            if (width == 0 || height == 0 || parentView.width == 0 || parentView.height == 0) return@post
            val maxX = (parentView.width - width).toFloat().coerceAtLeast(0f)
            val maxY = (parentView.height - height).toFloat().coerceAtLeast(0f)
            x = nx * maxX
            y = ny * maxY
        }
    }

    fun pausePlayback() {
        musicPlayer?.pause()
        isPlaying = false
        updatePlayIcon()
    }

    fun release() {
        stopAndReleaseMusic()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag() {
        setOnTouchListener { _, event ->
            if (!isVisible || !isDraggable) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    downViewX = x
                    downViewY = y
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        isDragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (isDragging) {
                        x = downViewX + dx
                        y = downViewY + dy
                        clampToParent()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }
    }

    private fun clampToParent() {
        val parentView = parent as? FrameLayout ?: return
        val maxX = (parentView.width - width).toFloat().coerceAtLeast(0f)
        val maxY = (parentView.height - height).toFloat().coerceAtLeast(0f)
        x = x.coerceIn(0f, maxX)
        y = y.coerceIn(0f, maxY)
    }

    private fun prepareMusic(audioUrl: String) {
        stopAndReleaseMusic()
        if (audioUrl.isBlank()) return
        musicPlayer = ExoPlayer.Builder(context).build().also { player ->
            player.setMediaItem(MediaItem.fromUri(audioUrl))
            player.prepare()
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        player.seekTo(0)
                        player.pause()
                        isPlaying = false
                        updatePlayIcon()
                    }
                }
            })
        }
        isPlaying = false
        updatePlayIcon()
    }

    private fun togglePlayback() {
        if (!previewAudioEnabled) return
        val player = musicPlayer ?: return
        if (isPlaying) {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
        updatePlayIcon()
    }

    private fun updatePlayIcon() {
        binding.btnPlay.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
        )
    }

    private fun stopAndReleaseMusic() {
        musicPlayer?.release()
        musicPlayer = null
        isPlaying = false
    }
}
