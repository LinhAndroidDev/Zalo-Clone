package com.example.messageapp.fragment

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentDiaryBinding
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.viewmodel.DiaryFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class TypeNews {
    Camera, Video, Edit
}

@AndroidEntryPoint
class DiaryFragment : BaseFragment<FragmentDiaryBinding, DiaryFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_diary

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

        val path = "https://firebasestorage.googleapis.com/v0/b/zalo-clone-45246.appspot.com/o/ChacAiDoSeVe-SonTungMTP-3666636.mp3?alt=media&token=2381255d-6983-4076-83bf-fdca2bcdd24e"
        binding?.recordWaveView?.loadDataWaveView(requireActivity(), path = path)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(requireActivity(), "Quyền ghi âm đã được cấp!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireActivity(), "Bạn cần cấp quyền để ghi âm!", Toast.LENGTH_SHORT).show()
            }
        }

//    private fun checkAndRequestPermission() {
//        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(requireActivity(), "Đã có quyền ghi âm!", Toast.LENGTH_SHORT).show()
//            if (!startRecord) {
//                startRecord = true
//                binding?.btnRecord?.text = "Stop Recording..."
//                audioRecorder?.startRecording(requireActivity())
//            } else {
//                startRecord = false
//                binding?.btnRecord?.text = "Start Recording"
//                recordedFilePath = audioRecorder?.stopRecording()
//                val byteArray = FileUtils.fileToByteArray(recordedFilePath.toString())
//                byteArray?.let { binding?.audioWaveView?.setRawData(it) }
//            }
//        } else {
//            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
//        }
//    }

    override fun onPause() {
        super.onPause()
        binding?.recordWaveView?.pause()
    }

    override fun onStop() {
        super.onStop()
        binding?.recordWaveView?.destroy()
    }
}