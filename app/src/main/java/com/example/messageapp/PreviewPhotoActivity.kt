package com.example.messageapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.messageapp.databinding.ActivityPreviewPhotoBinding
import com.example.messageapp.model.Message
import com.example.messageapp.utils.loadImg
import com.example.messageapp.viewmodel.PreviewPhotoActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PreviewPhotoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityPreviewPhotoBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<PreviewPhotoActivityViewModel>()
    companion object {
        const val OBJECT_MESSAGE = "OBJECT_MESSAGE"
        const val PHOTO_DATA = "PHOTO_DATA"
        const val KEY_ID = "KEY_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setUpFullScreen()

        val photoData = intent.getStringExtra(PHOTO_DATA)
        photoData?.let {
            Glide.with(this)
                .load(it)
                .into(binding.photo)
        }

        val message: Message? = intent.getParcelableExtra(OBJECT_MESSAGE)
        val keyId = intent.getStringExtra(KEY_ID)
        message?.let {
            viewModel.getInfo(it, keyId.toString())
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.user.collect { user ->
                loadImg(user?.avatar.toString(), binding.cirAvatar)
                binding.tvName.text = user?.name.toString()
            }
        }

        binding.backPreview.setOnClickListener { onBackPressed() }
    }

    private fun setUpFullScreen() {
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT
    }
}