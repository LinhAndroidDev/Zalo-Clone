package com.example.messageapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.doOnPreDraw
import com.bumptech.glide.Glide
import com.example.messageapp.databinding.ActivityPreviewAvatarBinding

/**
 * Full-screen preview of profile avatar / cover with pinch-zoom (PhotoView).
 * Optional shared element transition when [EXTRA_TRANSITION_NAME] is non-empty (see [createIntent]).
 */
class PreviewAvatarActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPreviewAvatarBinding.inflate(layoutInflater) }
    private var sharedElementName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_IMAGE_URL).orEmpty()
        sharedElementName = intent.getStringExtra(EXTRA_TRANSITION_NAME).orEmpty()
        if (url.isBlank()) {
            finish()
            return
        }

        if (sharedElementName.isNotBlank()) {
            val move = TransitionInflater.from(this).inflateTransition(R.transition.shared_image_avatar)
            window.sharedElementEnterTransition = move
            window.sharedElementReturnTransition =
                TransitionInflater.from(this).inflateTransition(R.transition.shared_image_avatar_return)
            move.addListener(object : Transition.TransitionListener {
                override fun onTransitionStart(transition: Transition) {}
                override fun onTransitionEnd(transition: Transition) {
                    binding.photoViewAvatar.post {
                        binding.photoViewAvatar.setScale(binding.photoViewAvatar.minimumScale, false)
                    }
                    move.removeListener(this)
                }

                override fun onTransitionCancel(transition: Transition) {}
                override fun onTransitionPause(transition: Transition) {}
                override fun onTransitionResume(transition: Transition) {}
            })
            supportPostponeEnterTransition()
        }

        setContentView(binding.root)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        window.statusBarColor = Color.TRANSPARENT

        if (sharedElementName.isNotBlank()) {
            binding.photoViewAvatar.transitionName = sharedElementName
        }

        binding.photoViewAvatar.scaleType = ImageView.ScaleType.FIT_CENTER

        if (sharedElementName.isNotBlank()) {
            var enterStarted = false
            fun startSharedElementEnterOnce() {
                if (enterStarted) return
                enterStarted = true
                binding.photoViewAvatar.doOnPreDraw {
                    supportStartPostponedEnterTransition()
                }
            }
            Glide.with(this)
                .load(url)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        startSharedElementEnterOnce()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        startSharedElementEnterOnce()
                        return false
                    }
                })
                .into(binding.photoViewAvatar)
        } else {
            Glide.with(this)
                .load(url)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(binding.photoViewAvatar)
        }

        binding.btnClosePreviewAvatar.setOnClickListener {
            finishWithTransition()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithTransition()
            }
        })
    }

    private fun finishWithTransition() {
        if (sharedElementName.isNotBlank()) {
            val pv = binding.photoViewAvatar
            pv.post {
                val w = pv.width
                val h = pv.height
                val cx = if (w > 0) w / 2f else pv.pivotX
                val cy = if (h > 0) h / 2f else pv.pivotY
                pv.setScale(pv.minimumScale, cx, cy, false)
                ActivityCompat.finishAfterTransition(this)
            }
        } else {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    companion object {
        const val EXTRA_IMAGE_URL = "extra_image_url"
        const val EXTRA_TRANSITION_NAME = "extra_transition_name"

        /** Shared element name for profile avatar (must match source [View.transitionName]). */
        const val TRANSITION_AVATAR = "transition_personal_avatar"

        /** Shared element name for profile cover image. */
        const val TRANSITION_COVER = "transition_personal_cover"

        fun createIntent(
            context: Context,
            imageUrl: String,
            sharedElementTransitionName: String = ""
        ): Intent =
            Intent(context, PreviewAvatarActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URL, imageUrl)
                putExtra(EXTRA_TRANSITION_NAME, sharedElementTransitionName)
            }
    }
}
