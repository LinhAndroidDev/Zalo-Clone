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
    private var showViewControl = false
    private var autoRestartOnEnd = false
    private var isUserSeeking = false
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var playerListener: Player.Listener? = null

    init {
        binding = CustomVideoViewBinding.inflate(LayoutInflater.from(context), this, true)
        binding?.videoView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        setupControls()
        setControlsVisible(false)
    }

    fun setControlsVisible(visible: Boolean) {
        showViewControl = visible
        binding?.viewControl?.isVisible = visible
    }

    fun setAutoRestartOnEnd(enabled: Boolean) {
        autoRestartOnEnd = enabled
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

    fun initVideo(
        url: String,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true,
        sharedPlayer: ExoPlayer? = null,
    ) {
        val exoPlayer = if (sharedPlayer != null) {
            bindPlayer(sharedPlayer)
            sharedPlayer
        } else {
            ensurePlayer()
            player
        } ?: return

        stopProgressUpdates()
        val currentUrl = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUrl != url) {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)), startPositionMs)
            exoPlayer.prepare()
        } else if (startPositionMs > 0L) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.playWhenReady = autoPlay
        isPlaying = autoPlay
        updatePlayButton()
        attachPlayerListener(exoPlayer)
        syncUiFromPlayer(exoPlayer)
        if (autoPlay) startProgressUpdates()
    }

    fun bindPlayer(externalPlayer: ExoPlayer): ExoPlayer {
        if (player !== externalPlayer) {
            releaseOwnedPlayer()
            player = externalPlayer
            ownsPlayer = false
        }
        binding?.videoView?.player = externalPlayer
        attachPlayerListener(externalPlayer)
        isPlaying = externalPlayer.isPlaying
        updatePlayButton()
        syncUiFromPlayer(externalPlayer)
        if (externalPlayer.isPlaying) startProgressUpdates() else stopProgressUpdates()
        return externalPlayer
    }

    fun detachPlayerOnly() {
        stopProgressUpdates()
        playerListener?.let { listener -> player?.removeListener(listener) }
        playerListener = null
        binding?.videoView?.player = null
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
                val targetPosition = exoPlayer.currentPosition
                val isBufferingAhead = exoPlayer.bufferedPosition >= targetPosition
                binding?.progressLoading?.isVisible =
                    playbackState == Player.STATE_BUFFERING && !isBufferingAhead
                if (playbackState == Player.STATE_READY) {
                    syncUiFromPlayer(exoPlayer)
                }
                if (playbackState == Player.STATE_ENDED) {
                    if (autoRestartOnEnd) {
                        exoPlayer.seekTo(0)
                        exoPlayer.playWhenReady = true
                        exoPlayer.play()
                        isPlaying = true
                        updatePlayButton()
                        syncUiFromPlayer(exoPlayer)
                        startProgressUpdates()
                    } else {
                        isPlaying = false
                        updatePlayButton()
                        stopProgressUpdates()
                    }
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

    private fun syncUiFromPlayer(exoPlayer: ExoPlayer) {
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        if (duration > 0L) {
            binding?.seekBarVideo?.max = duration.toInt()
            binding?.tvTotalTime?.text = formatDuration(duration)
        }
        val position = exoPlayer.currentPosition.coerceAtLeast(0L)
        binding?.seekBarVideo?.progress = position.toInt()
        binding?.tvCurrentTime?.text = formatDuration(position)
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
