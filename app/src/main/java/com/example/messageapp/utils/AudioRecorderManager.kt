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
import java.io.IOException
import java.text.SimpleDateFormat

class AudioRecorderManager {
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
        timeCurrent: (String) -> Unit
    ) {
        if (isResumingAudio) {
            resumeAudio(audioWaveView, scope, timeCurrent)
            return
        }
        releasePlayer()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()

            isResumingAudio = true
            job?.cancel()
            job = scope.launch {
                updateAudioProgress(audioWaveView, scope) { position ->
                    timeCurrent(SimpleDateFormat(DateUtils.MINUTE_TIME).format(position.toLong()))
                }
            }

            setOnCompletionListener {
                isResumingAudio = false
                audioWaveView?.progress = 100f
                job?.cancel()
                onFinish()
            }
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
        position: (Int) -> Unit
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

                    position(currentPosition)
                }
                delay(100)
            }
        }
    }

    fun stopAudio() {
        mediaPlayer?.stop()
        releasePlayer()
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
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
            updateAudioProgress(audioWaveView, this) { position ->
                timeCurrent(SimpleDateFormat(DateUtils.MINUTE_TIME).format(position.toLong()))
            }
        }
    }

    fun pauseAudio() {
        mediaPlayer?.apply {
            currentListen = currentPosition
            pause()
        }
    }
}