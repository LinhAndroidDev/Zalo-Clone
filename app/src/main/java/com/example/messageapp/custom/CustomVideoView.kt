package com.example.messageapp.custom

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.messageapp.R
import com.example.messageapp.databinding.CustomVideoViewBinding
import com.example.messageapp.utils.AnimatorUtils

class CustomVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var binding: CustomVideoViewBinding? = null
    private var isPlaying = true
    private var player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var showViewControl: Boolean = true
    private val timePlay = Handler(Looper.getMainLooper())

    init {
        binding = CustomVideoViewBinding.inflate(LayoutInflater.from(context))
        addView(binding?.root)

        binding?.videoView?.player = player

        binding?.btnPlay?.setOnClickListener {
            isPlaying = !isPlaying
            if (isPlaying) {
                player.play()
                binding?.btnPlay?.setImageResource(R.drawable.ic_pause)
            } else {
                player.pause()
                binding?.btnPlay?.setImageResource(R.drawable.ic_play)
            }
        }

        binding?.btnReplay10s?.setOnClickListener {
            player.seekTo(player.currentPosition - 10000)
        }

        binding?.btnForward10s?.setOnClickListener {
            player.seekTo(player.currentPosition + 10000)
        }

        binding?.videoView?.setOnClickListener {
            showViewControl = !showViewControl
            binding?.viewControl?.let {
                if (showViewControl) AnimatorUtils.fadeIn(it) else AnimatorUtils.fadeOut(it)
            }
            binding?.viewControl?.isVisible = showViewControl
        }
    }

    fun initVideo(url: String) {
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    binding?.seekBarVideo?.apply {
                        max = player.duration.toInt()
                    }

                    Log.e("CustomVideoView", "initVideo: ${player.duration}")
                    timePlay.postDelayed(object : Runnable {
                        override fun run() {
                            if (isPlaying) {
                                Log.e("CustomVideoView", "run: ${player.currentPosition.toInt()}")
                                binding?.seekBarVideo?.progress = player.currentPosition.toInt()
                            }
                            timePlay.postDelayed(this, 1000)
                        }
                    }, 0)
                }
            }
        })
    }

    fun cancel() {
        player.release()
        binding = null
    }
}