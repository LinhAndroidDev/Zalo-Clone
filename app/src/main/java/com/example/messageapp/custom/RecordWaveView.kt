package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
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
                        })
                }
            }
        }
    }

    fun loadDataWaveView(context: Context, path: String) {
        binding?.btnPlayAudio?.isVisible = false
        binding?.loading?.isVisible = true

        (context as AppCompatActivity).lifecycleScope.launch(Dispatchers.IO) {
            recordedFilePath = path
            val byteArray = FileUtils.convertMp3UrlToByteArray(recordedFilePath ?: "")

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