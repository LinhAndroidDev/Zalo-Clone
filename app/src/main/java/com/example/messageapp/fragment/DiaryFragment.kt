package com.example.messageapp.fragment

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

        val path =
            "https://firebasestorage.googleapis.com/v0/b/zalo-clone-45246.appspot.com/o/ChacAiDoSeVe-SonTungMTP-3666636.mp3?alt=media&token=2381255d-6983-4076-83bf-fdca2bcdd24e"
        binding?.recordWaveView?.loadDataWaveView(requireActivity(), path = path)

        // URL video
        val videoUrl =
            "https://firebasestorage.googleapis.com/v0/b/zalo-clone-45246.appspot.com/o/Mo%CC%82%CC%83i%20Ti%CC%80nh%20Kho%CC%82ng%20Te%CC%82n%20-%20%C4%90i%CC%80nh%20Nguye%CC%82%CC%83n%20x%20Le%CC%82%20Chi%CC%81%20Trung%20_%20Tu%CC%81%20Na%20Cover.mp4?alt=media&token=54f1b86d-a36a-454d-acf8-9079e92de810"
        binding?.videoView?.initVideo(videoUrl)
    }

    override fun onPause() {
        super.onPause()
        binding?.recordWaveView?.pause()
    }

    override fun onStop() {
        super.onStop()
        binding?.recordWaveView?.destroy()
        binding?.videoView?.cancel()
    }
}