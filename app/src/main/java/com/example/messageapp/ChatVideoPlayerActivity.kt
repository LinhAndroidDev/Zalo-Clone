package com.example.messageapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.messageapp.argument.ChatVideoPlayerArgument
import com.example.messageapp.chat.ChatVideoPlayerHolder
import com.example.messageapp.databinding.ActivityChatVideoPlayerBinding

class ChatVideoPlayerActivity : AppCompatActivity() {

    private val binding by lazy { ActivityChatVideoPlayerBinding.inflate(layoutInflater) }
    private var argument: ChatVideoPlayerArgument? = null
    private var returnStateMarked = false

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

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    markReturnState()
                    finish()
                }
            },
        )
        binding.btnClose.setOnClickListener {
            markReturnState()
            finish()
        }

        binding.videoPlayerView.setControlsVisible(false)
        binding.videoPlayerView.setAutoRestartOnEnd(true)

        val player = ChatVideoPlayerHolder.obtainPlayer(this)
        val startPositionMs = argument?.startPositionMs?.coerceAtLeast(0L) ?: 0L
        val canContinueSameMedia = ChatVideoPlayerHolder.isSameMediaLoaded(videoUrl)

        if (canContinueSameMedia) {
            binding.videoPlayerView.bindPlayer(player)
            player.volume = 1f
            player.playWhenReady = true
            player.play()
        } else {
            binding.videoPlayerView.initVideo(
                url = videoUrl,
                startPositionMs = startPositionMs,
                autoPlay = true,
                sharedPlayer = player,
            )
            player.volume = 1f
        }
    }

    override fun onPause() {
        if (isFinishing) {
            markReturnState()
        }
        super.onPause()
    }

    override fun onDestroy() {
        binding.videoPlayerView.setAutoRestartOnEnd(false)
        binding.videoPlayerView.detachPlayerOnly()
        super.onDestroy()
    }

    private fun markReturnState() {
        if (returnStateMarked) return
        returnStateMarked = true
        val player = ChatVideoPlayerHolder.obtainPlayer(this)
        ChatVideoPlayerHolder.markReturnToList(
            videoUrl = argument?.videoUrl.orEmpty(),
            shouldResume = player.isPlaying || player.playWhenReady,
        )
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
