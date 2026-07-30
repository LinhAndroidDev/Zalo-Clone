package com.example.messageapp.custom

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.media3.exoplayer.ExoPlayer
import com.example.messageapp.R
import com.example.messageapp.chat.ChatVideoPlayTarget
import com.example.messageapp.databinding.LayoutChatVideoCellBinding
import com.example.messageapp.utils.FileUtils.loadImg

class ChatVideoCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ChatVideoPlayTarget {

    private val binding: LayoutChatVideoCellBinding =
        LayoutChatVideoCellBinding.inflate(LayoutInflater.from(context), this)

    override var videoUrl: String = ""
        private set

    val thumbnailView: ImageView
        get() = binding.imgThumbnail

    fun bindVideo(url: String, placeholderRes: Int) {
        videoUrl = url
        binding.playerView.player = null
        binding.playerView.isVisible = false
        binding.imgPlayOverlay.isVisible = true
        binding.imgThumbnail.isVisible = true
        context.loadImg(url, binding.imgThumbnail, placeholderRes)
    }

    override fun attachPlayer(player: ExoPlayer) {
        binding.playerView.player = player
        binding.playerView.isVisible = true
        binding.imgThumbnail.isVisible = false
        binding.imgPlayOverlay.isVisible = false
    }

    override fun detachPlayer() {
        binding.playerView.player = null
        binding.playerView.isVisible = false
        binding.imgThumbnail.isVisible = true
        binding.imgPlayOverlay.isVisible = true
    }

    override fun visibleRatio(): Float {
        if (!isShown || width <= 0 || height <= 0) return 0f
        val rect = Rect()
        if (!getGlobalVisibleRect(rect)) return 0f
        val visibleArea = rect.width().coerceAtLeast(0) * rect.height().coerceAtLeast(0)
        val totalArea = width * height
        if (totalArea <= 0) return 0f
        return visibleArea.toFloat() / totalArea.toFloat()
    }
}
