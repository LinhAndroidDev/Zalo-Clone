package com.example.messageapp.custom

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import com.example.messageapp.databinding.TypingIndicatorBinding

class TypingIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var binding: TypingIndicatorBinding? = null

    init {
        binding = TypingIndicatorBinding.inflate(LayoutInflater.from(context))
        addView(binding?.root)
        startTypingAnimation()
    }

    private fun startTypingAnimation() {
        bounceDot(binding?.dot1, 0)
        bounceDot(binding?.dot2, 500)
        bounceDot(binding?.dot3, 1000)
    }

    fun bounceDot(dot: View?, delay: Long) {
        val up = ObjectAnimator.ofFloat(dot, "translationY", 0f, -6f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
        }

        val down = ObjectAnimator.ofFloat(dot, "translationY", -6f, 0f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
        }

        val animatorSet = AnimatorSet().apply {
            playSequentially(up, down)
            startDelay = delay
        }

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                bounceDot(dot, 0) // Lặp lại vô hạn
            }
        })

        animatorSet.start()
    }
}