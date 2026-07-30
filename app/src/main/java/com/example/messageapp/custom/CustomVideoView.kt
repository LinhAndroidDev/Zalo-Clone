package com.example.messageapp.custom

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.messageapp.R
import com.example.messageapp.databinding.CustomVideoViewBinding
import com.example.messageapp.utils.AnimatorUtils
import java.util.Locale
import java.util.concurrent.TimeUnit

class CustomVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var binding: CustomVideoViewBinding? = null
    private var player: ExoPlayer? = null
    private var ownsPlayer = true
    private var isPlaying = false
    private var showViewControl = true
    private var isUserSeeking = false
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var playerListener: Player.Listener? = null

    init {
        binding = CustomVideoViewBinding.inflate(LayoutInflater.from(context), this, true)
        binding?.videoView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        setupControls()
    }

    private fun setupControls() {
        binding?.btnPlay?.setOnClickListener { togglePlayPause() }

        binding?.btnReplay10s?.setOnClickListener {
            player?.seekTo((player?.currentPosition ?: 0L) - 10_000L)
        }

        binding?.btnForward10s?.setOnClickListener {
            player?.seekTo((player?.currentPosition ?: 0L) + 10_000L)
        }

        binding?.videoView?.setOnClickListener {
            showViewControl = !showViewControl
            binding?.viewControl?.let {
                if (showViewControl) AnimatorUtils.fadeIn(it) else AnimatorUtils.fadeOut(it)
            }
            binding?.viewControl?.isVisible = showViewControl
        }

        binding?.seekBarVideo?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding?.tvCurrentTime?.text = formatDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                player?.seekTo((seekBar?.progress ?: 0).toLong())
            }
        })
    }

    fun initVideo(url: String, autoPlay: Boolean = true) {
        ensurePlayer()
        val exoPlayer = player ?: return
        stopProgressUpdates()
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = autoPlay
        isPlaying = autoPlay
        updatePlayButton()
        attachPlayerListener(exoPlayer)
        if (autoPlay) startProgressUpdates()
    }

    fun bindPlayer(externalPlayer: ExoPlayer) {
        releaseOwnedPlayer()
        player = externalPlayer
        ownsPlayer = false
        binding?.videoView?.player = externalPlayer
        attachPlayerListener(externalPlayer)
    }

    fun pausePlayback() {
        player?.pause()
        isPlaying = false
        updatePlayButton()
        stopProgressUpdates()
    }

    fun release() {
        stopProgressUpdates()
        playerListener?.let { player?.removeListener(it) }
        playerListener = null
        binding?.videoView?.player = null
        if (ownsPlayer) {
            player?.release()
        }
        player = null
        binding = null
    }

    @Deprecated("Use release()", ReplaceWith("release()"))
    fun cancel() = release()

    private fun ensurePlayer() {
        if (player != null) return
        player = ExoPlayer.Builder(context).build()
        ownsPlayer = true
        binding?.videoView?.player = player
    }

    private fun releaseOwnedPlayer() {
        if (ownsPlayer) {
            player?.release()
        }
        playerListener?.let { player?.removeListener(it) }
        playerListener = null
        player = null
    }

    private fun attachPlayerListener(exoPlayer: ExoPlayer) {
        playerListener?.let { exoPlayer.removeListener(it) }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                binding?.progressLoading?.isVisible = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                    binding?.seekBarVideo?.max = duration.toInt()
                    binding?.tvTotalTime?.text = formatDuration(duration)
                    updateProgressUi()
                }
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    updatePlayButton()
                    stopProgressUpdates()
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                updatePlayButton()
                if (playing) startProgressUpdates() else stopProgressUpdates()
            }
        }
        playerListener = listener
        exoPlayer.addListener(listener)
    }

    private fun togglePlayPause() {
        val exoPlayer = player ?: return
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
            }
            exoPlayer.play()
        }
    }

    private fun updatePlayButton() {
        binding?.btnPlay?.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
        )
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        val runnable = object : Runnable {
            override fun run() {
                updateProgressUi()
                progressHandler.postDelayed(this, 500L)
            }
        }
        progressRunnable = runnable
        progressHandler.post(runnable)
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let { progressHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun updateProgressUi() {
        if (isUserSeeking) return
        val position = player?.currentPosition?.coerceAtLeast(0L) ?: return
        binding?.seekBarVideo?.progress = position.toInt()
        binding?.tvCurrentTime?.text = formatDuration(position)
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0L))
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
