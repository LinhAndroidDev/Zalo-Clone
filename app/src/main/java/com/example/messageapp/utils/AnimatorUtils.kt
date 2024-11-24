package com.example.messageapp.utils

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R

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
}