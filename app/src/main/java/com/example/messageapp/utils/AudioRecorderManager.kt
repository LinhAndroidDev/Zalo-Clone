package com.example.messageapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import com.example.messageapp.library.audiowave.AudioWaveView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat

class AudioRecorderManager {
    companion object {
        private val lock = Any()
        private var activeManager: AudioRecorderManager? = null
    }

    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var job: Job? = null
    private var currentListen: Int = 0
    private var isResumingAudio = false

    fun startRecording(context: Context) {
        outputFilePath =
            "${context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/record_${System.currentTimeMillis()}.mp3"

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

    @SuppressLint("SimpleDateFormat")
    fun playRecordedAudio(
        filePath: String,
        audioWaveView: AudioWaveView?,
        scope: CoroutineScope,
        onFinish: () -> Unit,
        timeCurrent: (String) -> Unit,
        startPositionMs: Int = 0,
        onProgress: (positionMs: Int, progress: Float) -> Unit = { _, _ -> }
    ) {
        val isValidFilePath = filePath.isNotBlank() &&
                (File(filePath).exists() || filePath.startsWith("http"))
        if (!isValidFilePath) {
            onFinish()
            return
        }

        // Ensure only one audio plays at a time across all RecordWaveView instances.
        synchronized(lock) {
            if (activeManager != null && activeManager !== this) {
                activeManager?.stopAudio()
            }
            activeManager = this
        }

        if (isResumingAudio) {
            resumeAudio(audioWaveView, scope, timeCurrent)
            return
        }
        releasePlayer()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                if (startPositionMs > 0 && startPositionMs < duration) {
                    seekTo(startPositionMs)
                    currentListen = startPositionMs
                }
                start()

                isResumingAudio = true
                job?.cancel()
                job = scope.launch {
                    updateAudioProgress(audioWaveView, scope) { position, progress ->
                        onProgress(position, progress)
                        timeCurrent(SimpleDateFormat(DateUtils.MINUTE_TIME).format(position.toLong()))
                    }
                }

                setOnCompletionListener {
                    isResumingAudio = false
                    audioWaveView?.progress = 100f
                    job?.cancel()
                    synchronized(lock) {
                        if (activeManager === this@AudioRecorderManager) {
                            activeManager = null
                        }
                    }
                    onFinish()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Cannot play audio: ${e.message}")
            isResumingAudio = false
            releasePlayer()
            job?.cancel()
            synchronized(lock) {
                if (activeManager === this) {
                    activeManager = null
                }
            }
            onFinish()
        }

        audioWaveView?.onProgressChanged = { progress, byUser ->
            if (byUser && mediaPlayer != null) {
                val duration = mediaPlayer?.duration ?: 0
                if (duration > 0) {
                    val seekPosition = (progress / 100f * duration).toInt()
                    mediaPlayer?.seekTo(seekPosition)
                    currentListen = seekPosition
                }
            }
        }
    }

    private fun updateAudioProgress(
        audioWaveView: AudioWaveView?,
        scope: CoroutineScope,
        position: (Int, Float) -> Unit
    ) {
        scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (mediaPlayer != null) {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    val duration = mediaPlayer?.duration ?: 1
                    val progress = (currentPosition.toFloat() / duration) * 100

                    withContext(Dispatchers.Main) {
                        audioWaveView?.progress = if (progress > 100) 100f else progress
                    }

                    position(currentPosition, if (progress > 100) 100f else progress)
                }
                delay(100)
            }
        }
    }

    fun stopAudio() {
        mediaPlayer?.stop()
        releasePlayer()
        job?.cancel()
        synchronized(lock) {
            if (activeManager === this) {
                activeManager = null
            }
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        isResumingAudio = false
    }

    @SuppressLint("SimpleDateFormat")
    private fun resumeAudio(
        audioWaveView: AudioWaveView?,
        scope: CoroutineScope,
        timeCurrent: (String) -> Unit
    ) {
        mediaPlayer?.seekTo(currentListen)
        mediaPlayer?.start()
        job?.cancel()
        job = scope.launch {
            updateAudioProgress(audioWaveView, this) { position, _ ->
                timeCurrent(SimpleDateFormat(DateUtils.MINUTE_TIME).format(position.toLong()))
            }
        }
    }

    fun currentPositionMs(): Int = mediaPlayer?.currentPosition ?: currentListen

    fun pauseAudio() {
        mediaPlayer?.apply {
            currentListen = currentPosition
            pause()
        }
    }
}