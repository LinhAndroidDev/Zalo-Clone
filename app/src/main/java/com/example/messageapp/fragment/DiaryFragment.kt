package com.example.messageapp.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentDiaryBinding
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.utils.AudioRecorderManager
import com.example.messageapp.utils.FileUtils
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.viewmodel.DiaryFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TypeNews {
    Camera, Video, Edit
}

@AndroidEntryPoint
class DiaryFragment : BaseFragment<FragmentDiaryBinding, DiaryFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_diary

    private var audioRecorder: AudioRecorderManager? = null
    private var recordedFilePath: String? = null

    private var startRecord = false
    private var isPlaying = false

    override fun initView() {
        super.initView()
        // log event: screen_diary
        FirebaseAnalyticsInstance.logDiaryScreen()

        viewModel?.getInfoUser()
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.user?.collect { user ->
                binding?.let { binding ->
                    activity?.loadImg(user?.avatar.toString(), binding.avatarUser)
                    activity?.loadImg(user?.avatar.toString(), binding.imgCreateNews)
                }
            }
        }

        AnimatorUtils.scaleNews(binding?.iconNews, TypeNews.Camera)

        audioRecorder = AudioRecorderManager(requireActivity())
        binding?.btnRecord?.setOnClickListener {
            binding?.btnPlayAudio?.isVisible = false
            binding?.loading?.isVisible = true

            lifecycleScope.launch(Dispatchers.IO) {
                val byteArray = FileUtils.convertMp3UrlToByteArray("https://firebasestorage.googleapis.com/v0/b/zalo-clone-45246.appspot.com/o/YeuDi-ChauKhaiPhong-8876313.mp3?alt=media&token=4199b0e1-4816-4786-a819-035533331976")

                withContext(Dispatchers.Main) {
                    byteArray?.let { binding?.audioWaveView?.setRawData(it, callback = {
                        binding?.btnPlayAudio?.isVisible = true
                        binding?.loading?.isVisible = false
                    }) }
//            checkAndRequestPermission()
                }
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
                audioRecorder?.playRecordedAudio("https://firebasestorage.googleapis.com/v0/b/zalo-clone-45246.appspot.com/o/YeuDi-ChauKhaiPhong-8876313.mp3?alt=media&token=4199b0e1-4816-4786-a819-035533331976", binding?.audioWaveView, lifecycleScope, onFinish = {
                    binding?.icPlay?.setImageResource(R.drawable.ic_play)
                    isPlaying = false
                }, timeCurrent = {
                    binding?.txtDuration?.text = it
                })
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(requireActivity(), "Quyền ghi âm đã được cấp!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireActivity(), "Bạn cần cấp quyền để ghi âm!", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireActivity(), "Đã có quyền ghi âm!", Toast.LENGTH_SHORT).show()
            if (!startRecord) {
                startRecord = true
                binding?.btnRecord?.text = "Stop Recording..."
                audioRecorder?.startRecording(requireActivity())
            } else {
                startRecord = false
                binding?.btnRecord?.text = "Start Recording"
                recordedFilePath = audioRecorder?.stopRecording()
                val byteArray = FileUtils.fileToByteArray(recordedFilePath.toString())
                byteArray?.let { binding?.audioWaveView?.setRawData(it) }
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}