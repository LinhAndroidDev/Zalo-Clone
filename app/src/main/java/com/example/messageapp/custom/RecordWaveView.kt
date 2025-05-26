package com.example.messageapp.custom

import android.annotation.SuppressLint
import android.content.Context
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
import com.example.messageapp.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TypeRecord(val value: Int) {
    LiSTEN(0),
    PREVIEW(1);

    companion object {
        fun of(value: Int): TypeRecord {
            return entries.firstOrNull { it.value == value } ?: LiSTEN
        }
    }
}

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

    init {
        binding = RecordWaveViewBinding.inflate(LayoutInflater.from(context))
        binding?.root?.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        binding?.let { addView(it.root) }
        audioRecorder = AudioRecorderManager(context)

        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.RecordWaveView, 0, 0)
        val type = array.getInt(R.styleable.RecordWaveView_type, 0)
        when (TypeRecord.of(type)) {
            TypeRecord.LiSTEN -> {
                binding?.viewRecord?.setBackgroundResource(R.drawable.bg_sender)
                binding?.btnPlayAudio?.backgroundTintList = ContextCompat.getColorStateList(context, R.color.blue)
                binding?.icPlay?.backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                binding?.txtDuration?.isVisible = true
                binding?.txtDurationListenAgain?.isVisible = false
            }

            TypeRecord.PREVIEW -> {
                binding?.viewRecord?.setBackgroundResource(R.drawable.bg_corner_25_stroke_grey)
                binding?.btnPlayAudio?.backgroundTintList = ContextCompat.getColorStateList(context, R.color.grey_bg)
                binding?.icPlay?.imageTintList = ContextCompat.getColorStateList(context, R.color.black)
                binding?.txtDuration?.isVisible = false
                binding?.txtDurationListenAgain?.isVisible = true
            }
        }

        binding?.btnPlayAudio?.setOnClickListener {
            if (isPlaying) {
                audioRecorder?.pauseAudio()
                binding?.icPlay?.setImageResource(R.drawable.ic_play)
                isPlaying = false
            } else {
                isPlaying = true
                binding?.icPlay?.setImageResource(R.drawable.ic_pause)
                findViewTreeLifecycleOwner()?.let { own ->
                    audioRecorder?.playRecordedAudio(
                        recordedFilePath ?: "",
                        binding?.audioWaveView,
                        own.lifecycleScope,
                        onFinish = {
                            binding?.icPlay?.setImageResource(R.drawable.ic_play)
                            isPlaying = false
                        },
                        timeCurrent = {
                            binding?.txtDuration?.text = it
                            binding?.txtDurationListenAgain?.text = it
                        })
                }
            }
        }
    }

    fun loadDataWaveView(context: Context, path: String, fromUrl: Boolean = true) {
        binding?.btnPlayAudio?.isVisible = false
        binding?.loading?.isVisible = true

        (context as AppCompatActivity).lifecycleScope.launch(Dispatchers.IO) {
            recordedFilePath = path
            val byteArray = if (fromUrl) {
                FileUtils.convertMp3UrlToByteArray(recordedFilePath ?: "")
            } else {
                FileUtils.fileToByteArray(recordedFilePath ?: "")
            }

            withContext(Dispatchers.Main) {
                byteArray?.let {
                    binding?.audioWaveView?.setRawData(it, callback = {
                        binding?.btnPlayAudio?.isVisible = true
                        binding?.loading?.isVisible = false
                    })
                }
            }
        }
    }

    fun pause() {
        audioRecorder?.pauseAudio()
    }

    fun destroy() {
        audioRecorder?.stopAudio()
        audioRecorder = null
        binding = null
    }
}