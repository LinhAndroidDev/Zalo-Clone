package com.example.messageapp.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.fragment.TypeNews

object AnimatorUtils {
    private const val DURATION_TIME = 300L

    fun fadeIn(view: View) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = DURATION_TIME
        animator.addUpdateListener { animation ->
            val alpha = animation.animatedValue as Float
            view.alpha = alpha
        }
        animator.start()
    }

    fun fadeOut(view: View) {
        val animator = ValueAnimator.ofFloat(1f, 0f)
        animator.duration = DURATION_TIME
        animator.addUpdateListener { animation ->
            val alpha = animation.animatedValue as Float
            view.alpha = alpha
        }
        animator.start()
    }

    fun fadeInItemRecyclerView(context: Context, recyclerView: RecyclerView?) {
        val animFadeIn =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_fade_in)
        recyclerView?.layoutAnimation = animFadeIn
    }

    fun scaleEmotion(context: Context, view: ViewGroup) {
        view.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_scale_emotion)
        view.scheduleLayoutAnimation()
    }

    fun fadeInViewItem(context: Context, view: ViewGroup) {
        view.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_fade_in)
        view.scheduleLayoutAnimation()
    }

    fun transitionInViewItem(context: Context, view: ViewGroup) {
        view.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_transition_in)
        view.scheduleLayoutAnimation()
    }

    fun scaleNews(img: ImageView?, type: TypeNews) {
        img?.setImageResource(getNextIcon(type))

        img?.scaleX = 0f
        img?.scaleY = 0f

        val scaleXAnimator = ObjectAnimator.ofFloat(img, "scaleX", 0f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(img, "scaleY", 0f, 1f)

        val animatorSet = AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator)
            duration = 1000
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scaleNews(
                        img,
                        when (type) {
                            TypeNews.Camera -> TypeNews.Video
                            TypeNews.Video -> TypeNews.Edit
                            TypeNews.Edit -> TypeNews.Camera
                        }
                    )
                }
            })
        }
        animatorSet.start()
    }

    private fun getNextIcon(type: TypeNews): Int {
        return when (type) {
            TypeNews.Camera -> R.drawable.ic_video_news
            TypeNews.Video -> R.drawable.ic_edit_news
            TypeNews.Edit -> R.drawable.ic_camera_news
        }
    }
}