package com.example.messageapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.messageapp.library.audiowave.AudioWaveView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String? = null
    private var exoPlayer: ExoPlayer? = null
    private var job: Job? = null // Job để hủy coroutine khi cần
    private var currentListen: Long = 0L
    private var isResumingAudio = false

    fun startRecording(context: Context) {
        outputFilePath = "${context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/record_${System.currentTimeMillis()}.mp3"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(outputFilePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("AUDIO_RECORDING", "prepare() failed")
            }

            start()
        }
    }

    fun stopRecording(): String? {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        return outputFilePath
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("SimpleDateFormat")
    fun playRecordedAudio(filePath: String, audioWaveView: AudioWaveView?, scope: CoroutineScope, onFinish: () -> Unit, timeCurrent: (String) -> Unit) {
        if (isResumingAudio) {
            resumeAudio(audioWaveView, scope, timeCurrent)
            return
        }
        releasePlayer()

        exoPlayer = ExoPlayer.Builder(context).build().apply {
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(filePath))
            setMediaSource(mediaSource)
            prepare()
            play()

            isResumingAudio = true
            job?.cancel()
            job = scope.launch {
                updateAudioProgress(audioWaveView, scope) { position ->
                    timeCurrent(SimpleDateFormat(DateUtils.MINUTE_TIME).format(position))
                }
            }

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        isResumingAudio = false
                        audioWaveView?.progress = 100f
                        job?.cancel()
                        onFinish()
                    }
                }
            })
        }

        audioWaveView?.onProgressChanged = { progress, byUser ->
            if (byUser && exoPlayer != null && exoPlayer?.isPlaying == true) {
                val duration = exoPlayer?.duration ?: 0L
                if (duration > 0) {
                    val seekPosition = (progress / 100f * duration).toLong()
                    exoPlayer?.seekTo(seekPosition)
                }
            }
        }
    }

    private fun updateAudioProgress(
        audioWaveView: AudioWaveView?,
        scope: CoroutineScope,
        position: (Long) -> Unit
    ) {
        scope.launch(Dispatchers.Main) {
            while (isActive) { // Kiểm tra coroutine còn hoạt động
                if (exoPlayer != null && exoPlayer?.isPlaying == true) {
                    val currentPosition = exoPlayer?.currentPosition ?: 0L
                    val duration = exoPlayer?.duration ?: 1L
                    val progress = (currentPosition.toFloat() / duration) * 100

                    withContext(Dispatchers.Main) {
                        Log.e("ExoAudioPlayer", "Progress: $progress")
                        audioWaveView?.progress = progress
                    }

                    position(currentPosition)
                }
                delay(100) // Cập nhật mỗi 100ms
            }
        }
    }

    fun stopAudio() {
        exoPlayer?.stop()
        releasePlayer()
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    @SuppressLint("SimpleDateFormat")
    private fun resumeAudio(audioWaveView: AudioWaveView?, scope: CoroutineScope, timeCurrent: (String) -> Unit) {
        exoPlayer?.seekTo(currentListen)
        exoPlayer?.play()
        job?.cancel()
        job = scope.launch {
            updateAudioProgress(audioWaveView, this) { position ->
                timeCurrent(SimpleDateFormat(DateUtils.MINUTE_TIME).format(position))
            }
        }
    }

    fun pauseAudio() {
        exoPlayer?.apply {
            currentListen = currentPosition
            pause()
        }
    }
}