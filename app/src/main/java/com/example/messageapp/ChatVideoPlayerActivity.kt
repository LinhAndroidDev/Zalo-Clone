package com.example.messageapp

import android.graphics.Color
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.messageapp.argument.ChatVideoPlayerArgument
import com.example.messageapp.chat.ChatVideoPlayerHolder
import com.example.messageapp.databinding.ActivityChatVideoPlayerBinding

class ChatVideoPlayerActivity : AppCompatActivity() {

    private val binding by lazy { ActivityChatVideoPlayerBinding.inflate(layoutInflater) }
    private var argument: ChatVideoPlayerArgument? = null
    private var returnStateMarked = false
    private var enterTransitionStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.sharedElementEnterTransition = TransitionInflater.from(this)
            .inflateTransition(android.R.transition.move)
        window.sharedElementReturnTransition = TransitionInflater.from(this)
            .inflateTransition(android.R.transition.move)
        supportPostponeEnterTransition()
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
                    finishWithTransition()
                }
            },
        )
        binding.btnClose.setOnClickListener { finishWithTransition() }

        binding.imgSharedElement.transitionName = argument?.messageTime.orEmpty()
        binding.videoPlayerView.alpha = 0f
        binding.videoPlayerView.setControlsVisible(false)
        binding.videoPlayerView.setAutoRestartOnEnd(true)

        Glide.with(this)
            .load(videoUrl)
            .into(binding.imgSharedElement.apply {
                doOnPreDraw {
                    if (!enterTransitionStarted) {
                        enterTransitionStarted = true
                        supportStartPostponedEnterTransition()
                    }
                }
            })

        window.sharedElementEnterTransition.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition?) = Unit

            override fun onTransitionEnd(transition: Transition?) {
                revealVideoPlayer()
            }

            override fun onTransitionCancel(transition: Transition?) {
                revealVideoPlayer()
            }

            override fun onTransitionPause(transition: Transition?) = Unit

            override fun onTransitionResume(transition: Transition?) = Unit
        })

        bindVideoPlayer(videoUrl)
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

    private fun bindVideoPlayer(videoUrl: String) {
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

    private fun revealVideoPlayer() {
        binding.imgSharedElement.isVisible = false
        binding.videoPlayerView.alpha = 1f
    }

    private fun finishWithTransition() {
        markReturnState()
        binding.videoPlayerView.alpha = 0f
        binding.imgSharedElement.isVisible = true
        supportFinishAfterTransition()
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
