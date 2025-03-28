package com.example.messageapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.adapter.PhotoAdapter
import com.example.messageapp.argument.PreviewPhotoArgument
import com.example.messageapp.databinding.ActivityPreviewPhotoBinding
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.loadImg
import com.example.messageapp.viewmodel.PreviewPhotoActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PreviewPhotoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityPreviewPhotoBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<PreviewPhotoActivityViewModel>()
    private var isShowHeader = true
    companion object {
        const val PREVIEW_PHOTO_ARGUMENT = "PREVIEW_PHOTO_ARGUMENT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setUpFullScreen()
        initView()
        onClickView()
    }

    private fun initView() {
        val previewPhotoArgument: PreviewPhotoArgument? = intent.getParcelableExtra(PREVIEW_PHOTO_ARGUMENT)
        previewPhotoArgument?.let { arg ->
            binding.tvTimeSend.text = DateUtils.formatDateTimeApp(arg.message.time)
            val adapter = PhotoAdapter(this)
            adapter.items = arg.photoData
            adapter.onClickPhoto = {
                if (isShowHeader) {
                    AnimatorUtils.fadeOut(binding.header)
                } else {
                    AnimatorUtils.fadeIn(binding.header)
                }
                isShowHeader = !isShowHeader
            }
            binding.photoPager.adapter = adapter
            binding.photoPager.setCurrentItem(arg.indexOfPhoto, false)
            viewModel.getInfo(arg.message, arg.keyId)
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.user.collect { user ->
                loadImg(user?.avatar.toString(), binding.cirAvatar)
                binding.tvName.text = user?.name.toString()
            }
        }
    }

    private fun onClickView() {
        binding.backPreview.setOnClickListener { onBackPressed() }

        binding.photoPager.setOnClickListener {
            if (isShowHeader) {
                AnimatorUtils.fadeOut(binding.header)
            } else {
                AnimatorUtils.fadeIn(binding.header)
            }
            isShowHeader = !isShowHeader
        }
    }

    private fun setUpFullScreen() {
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT
    }
}