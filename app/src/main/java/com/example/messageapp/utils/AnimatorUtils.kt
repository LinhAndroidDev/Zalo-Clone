package com.example.messageapp.utils

import android.animation.ValueAnimator
import android.view.View

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
}