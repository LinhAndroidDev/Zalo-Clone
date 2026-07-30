package com.example.messageapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.messageapp.argument.ChatVideoPlayerArgument
import com.example.messageapp.databinding.ActivityChatVideoPlayerBinding

class ChatVideoPlayerActivity : AppCompatActivity() {

    private val binding by lazy { ActivityChatVideoPlayerBinding.inflate(layoutInflater) }
    private var argument: ChatVideoPlayerArgument? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpFullScreen()
        setContentView(binding.root)
        argument = intent.getParcelableExtra(ARG_CHAT_VIDEO)
        val videoUrl = argument?.videoUrl.orEmpty()
        if (videoUrl.isBlank()) {
            Toast.makeText(this, R.string.chat_video_play_error, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.btnClose.setOnClickListener { finish() }
        binding.videoPlayerView.initVideo(videoUrl, autoPlay = true)
    }

    override fun onPause() {
        binding.videoPlayerView.pausePlayback()
        super.onPause()
    }

    override fun onDestroy() {
        binding.videoPlayerView.release()
        super.onDestroy()
    }

    private fun setUpFullScreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        window.statusBarColor = Color.TRANSPARENT
    }

    companion object {
        const val ARG_CHAT_VIDEO = "ARG_CHAT_VIDEO"
    }
}
