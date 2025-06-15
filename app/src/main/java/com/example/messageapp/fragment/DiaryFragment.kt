package com.example.messageapp.fragment

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.messageapp.PersonalActivity
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
    }

    override fun onClickView() {
        super.onClickView()

        binding?.avatarUser?.setOnClickListener {
            val intent = Intent(requireActivity(), PersonalActivity::class.java)
            startActivity(intent)
        }

        binding?.addStatus?.setOnClickListener {
            findNavController().navigate(R.id.action_diaryFragment_to_statusFragment)
        }
    }
}