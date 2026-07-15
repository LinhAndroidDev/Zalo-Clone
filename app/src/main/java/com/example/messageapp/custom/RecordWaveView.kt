package com.example.messageapp.custom

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.R
import com.example.messageapp.databinding.RecordWaveViewBinding
import com.example.messageapp.utils.AudioRecorderManager
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.LruCache

enum class TypeRecord(val value: Int) {
    SENDER(0),
    PREVIEW(1),
    RECEIVER(2);

    companion object {
        fun of(value: Int): TypeRecord {
            return entries.firstOrNull { it.value == value } ?: SENDER
        }
    }
}

data class AudioPlaybackState(
    val progress: Float = 0f,
    val positionMs: Int = 0
)

@SuppressLint("ResourceType")
class RecordWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var binding: RecordWaveViewBinding? = null
    private var isPlaying = false
    private var audioRecorder: AudioRecorderManager? = null
    private var recordedFilePath: String? = null
    private var audioLoadJob: Job? = null
    private var currentAudioKey: String? = null
    private var isAudioLoading = false
    private var isAudioReady = false
    private var playbackState = AudioPlaybackState()
    private var onPlaybackStateChanged: ((AudioPlaybackState) -> Unit)? = null
    private var handlerAnimation = Handler()
    private var runnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                binding?.viewAnimation1?.animate()?.scaleX(1.4f)?.scaleY(1.4f)?.alpha(0f)
                    ?.setDuration(1000)
                    ?.withEndAction {
                        binding?.viewAnimation1?.scaleX = 1f
                        binding?.viewAnimation1?.scaleY = 1f
                        binding?.viewAnimation1?.alpha = 1f
                    }

                binding?.viewAnimation2?.animate()?.scaleX(1.4f)?.scaleY(1.4f)?.alpha(0f)
                    ?.setDuration(700)
                    ?.withEndAction {
                        binding?.viewAnimation2?.scaleX = 1f
                        binding?.viewAnimation2?.scaleY = 1f
                        binding?.viewAnimation2?.alpha = 1f
                    }
            }
            if (isPlaying) {
                handlerAnimation.postDelayed(this, 1500)
            }
        }

    }

    companion object {
        // Cache sampled waveform by file path + chunk count to reduce rebinding cost.
        private val waveformCache = LruCache<String, ByteArray>(120)
    }

    init {
        binding = RecordWaveViewBinding.inflate(LayoutInflater.from(context))
        binding?.root?.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        binding?.let { addView(it.root) }
        audioRecorder = AudioRecorderManager()

        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.RecordWaveView, 0, 0)
        val type = array.getInt(R.styleable.RecordWaveView_type, 0)
        when (TypeRecord.of(type)) {
            TypeRecord.SENDER -> {
                binding?.viewRecord?.setBackgroundResource(R.drawable.bg_sender)
                binding?.btnPlayAudio?.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.color_accent)
                binding?.icPlay?.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.white)
                binding?.txtDuration?.isVisible = true
                binding?.txtDurationListenAgain?.isVisible = false
            }

            TypeRecord.PREVIEW -> {
                binding?.viewRecord?.setBackgroundResource(R.drawable.bg_corner_25_stroke_grey)
                binding?.btnPlayAudio?.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.surface_divider)
                binding?.icPlay?.imageTintList =
                    ContextCompat.getColorStateList(context, R.color.black)
                binding?.txtDuration?.isVisible = false
                binding?.txtDurationListenAgain?.isVisible = true
                binding?.audioWaveView?.wavePaint = ContextCompat.getColor(context, R.color.icon_default)
            }

            TypeRecord.RECEIVER -> {
                binding?.viewRecord?.setBackgroundResource(R.drawable.bg_receiver)
                binding?.btnPlayAudio?.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.surface_divider)
                binding?.icPlay?.imageTintList =
                    ContextCompat.getColorStateList(context, R.color.black)
                binding?.txtDuration?.isVisible = true
                binding?.txtDurationListenAgain?.isVisible = false
                binding?.audioWaveView?.wavePaint = ContextCompat.getColor(context, R.color.icon_default)
            }
        }

        binding?.btnPlayAudio?.setOnClickListener {
            if (isAudioLoading || !isAudioReady) return@setOnClickListener

            if (isPlaying) {
                val currentPosition = audioRecorder?.currentPositionMs() ?: playbackState.positionMs
                playbackState = playbackState.copy(positionMs = currentPosition)
                onPlaybackStateChanged?.invoke(playbackState)
                audioRecorder?.pauseAudio()
                binding?.icPlay?.setImageResource(R.drawable.ic_play)
                isPlaying = false
                stopAnimationLoop()
            } else {
                val filePath = recordedFilePath.orEmpty()
                val isPlayablePath =
                    filePath.isNotBlank() && (File(filePath).exists() || filePath.startsWith("http"))
                if (!isPlayablePath) {
                    binding?.icPlay?.setImageResource(R.drawable.ic_play)
                    isPlaying = false
                    return@setOnClickListener
                }
                isPlaying = true
                binding?.icPlay?.setImageResource(R.drawable.ic_pause)
                startAnimationLoop()
                val scope =
                    findViewTreeLifecycleOwner()?.lifecycleScope
                        ?: (context as? AppCompatActivity)?.lifecycleScope
                if (scope == null) {
                    binding?.icPlay?.setImageResource(R.drawable.ic_play)
                    isPlaying = false
                    stopAnimationLoop()
                    return@setOnClickListener
                }
                audioRecorder?.playRecordedAudio(
                    filePath,
                    binding?.audioWaveView,
                    scope,
                    onFinish = {
                        playbackState = AudioPlaybackState(progress = 100f, positionMs = 0)
                        onPlaybackStateChanged?.invoke(playbackState)
                        updateDurationText(0)
                        binding?.icPlay?.setImageResource(R.drawable.ic_play)
                        isPlaying = false
                        stopAnimationLoop()
                    },
                    timeCurrent = {
                        binding?.txtDuration?.text = it
                        binding?.txtDurationListenAgain?.text = it
                    },
                    startPositionMs = playbackState.positionMs,
                    onProgress = { positionMs, progress ->
                        playbackState = playbackState.copy(progress = progress, positionMs = positionMs)
                        onPlaybackStateChanged?.invoke(playbackState)
                    }
                )
            }
        }
    }

    fun loadDataWaveView(
        context: Context,
        path: String,
        fromUrl: Boolean = true,
        state: AudioPlaybackState? = null,
        onStateChanged: ((AudioPlaybackState) -> Unit)? = null
    ) {
        audioLoadJob?.cancel()
        audioRecorder?.pauseAudio()
        isPlaying = false
        stopAnimationLoop()
        recordedFilePath = null
        currentAudioKey = null
        playbackState = state ?: AudioPlaybackState()
        onPlaybackStateChanged = onStateChanged
        isAudioReady = false
        isAudioLoading = false
        binding?.icPlay?.setImageResource(R.drawable.ic_play)
        currentAudioKey = "${fromUrl}_$path"
        val requestKey = currentAudioKey
        val lifecycleScope =
            findViewTreeLifecycleOwner()?.lifecycleScope
                ?: (context as? AppCompatActivity)?.lifecycleScope
        if (path.isBlank() || lifecycleScope == null) {
            showLoading(false)
            return
        }

        showLoading(true)

        audioLoadJob = lifecycleScope.launch(Dispatchers.IO) {
            val localAudioFile = if (fromUrl) {
                FileUtils.getOrDownloadAudioFile(context, path)
            } else {
                File(path)
            }
            recordedFilePath = localAudioFile?.absolutePath
            val byteArray = if (fromUrl) {
                localAudioFile?.let { FileUtils.fileToByteArray(it.absolutePath) }
            } else {
                FileUtils.fileToByteArray(recordedFilePath ?: "")
            }

            withContext(Dispatchers.Main) {
                if (requestKey != currentAudioKey) return@withContext
                byteArray?.let {
                    val waveView = binding?.audioWaveView
                    val chunksCount = (waveView?.chunksCount ?: 0).coerceAtLeast(1)
                    val waveformKey = "${recordedFilePath.orEmpty()}_$chunksCount"
                    val cachedScaledData = waveformCache.get(waveformKey)

                    if (cachedScaledData != null && waveView != null) {
                        waveView.scaledData = cachedScaledData
                        waveView.progress = playbackState.progress.coerceIn(0f, 100f)
                        updateDurationText(playbackState.positionMs)
                        isAudioReady = true
                        showLoading(false)
                        return@withContext
                    }

                    waveView?.progress = 0f
                    waveView?.setRawData(it, callback = {
                        if (requestKey != currentAudioKey) return@setRawData
                        waveView.scaledData.let { scaled ->
                            waveformCache.put(waveformKey, scaled)
                        }
                        waveView.progress = playbackState.progress.coerceIn(0f, 100f)
                        updateDurationText(playbackState.positionMs)
                        isAudioReady = true
                        showLoading(false)
                    })
                } ?: run {
                    isAudioReady = false
                    showLoading(false)
                }
            }
        }
    }

    fun pause() {
        val currentPosition = audioRecorder?.currentPositionMs() ?: playbackState.positionMs
        playbackState = playbackState.copy(positionMs = currentPosition)
        onPlaybackStateChanged?.invoke(playbackState)
        audioRecorder?.pauseAudio()
        binding?.icPlay?.setImageResource(R.drawable.ic_play)
        isPlaying = false
        stopAnimationLoop()
    }

    private fun showLoading(loading: Boolean) {
        isAudioLoading = loading
        binding?.loading?.isVisible = loading
        binding?.btnPlayAudio?.isVisible = !loading && isAudioReady
        binding?.btnPlayAudio?.isEnabled = !loading && isAudioReady
        binding?.viewAnimation1?.isVisible = !loading && isAudioReady
        binding?.viewAnimation2?.isVisible = !loading && isAudioReady
    }

    private fun updateDurationText(positionMs: Int) {
        val safePosition = if (positionMs < 0) 0 else positionMs
        val timeText =
            SimpleDateFormat(DateUtils.MINUTE_TIME, Locale.getDefault()).format(safePosition.toLong())
        binding?.txtDuration?.text = timeText
        binding?.txtDurationListenAgain?.text = timeText
    }

    private fun startAnimationLoop() {
        handlerAnimation.removeCallbacks(runnable)
        handlerAnimation.post(runnable)
    }

    private fun stopAnimationLoop() {
        handlerAnimation.removeCallbacks(runnable)
    }
}