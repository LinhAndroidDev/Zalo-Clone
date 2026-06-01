package com.example.messageapp.utils

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.messageapp.R
import com.example.messageapp.model.EmotionType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object EmotionBurstEffect {

    private const val PARTICLE_COUNT = 28
    private const val DURATION_MS = 1200L
    private const val POP_DURATION_MS = 180L
    private const val MIN_DISTANCE_DP = 72
    private const val MAX_DISTANCE_EXTRA_DP = 96

    fun play(activity: Activity, anchor: View, type: EmotionType) {
        if (!anchor.isShown || anchor.width == 0 || anchor.height == 0) return

        val root = activity.window.decorView as ViewGroup
        val overlay = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        root.addView(overlay)

        val anchorLoc = IntArray(2)
        val rootLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        root.getLocationOnScreen(rootLoc)
        val centerX = anchorLoc[0] - rootLoc[0] + anchor.width / 2f
        val centerY = anchorLoc[1] - rootLoc[1] + anchor.height / 2f

        val density = activity.resources.displayMetrics.density
        val iconSizePx = (18 * density).toInt().coerceAtLeast(1)
        val drawableRes = emotionDrawable(type)
        val random = Random(System.nanoTime())

        repeat(PARTICLE_COUNT) { index ->
            val particle = ImageView(activity).apply {
                setImageResource(drawableRes)
                layoutParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx)
                x = centerX - iconSizePx / 2f
                y = centerY - iconSizePx / 2f
                scaleX = 0.15f
                scaleY = 0.15f
                alpha = 0.95f
            }
            overlay.addView(particle)

            val angleDeg = (360f / PARTICLE_COUNT * index) + random.nextFloat() * 22f - 11f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val distancePx = (MIN_DISTANCE_DP + random.nextInt(MAX_DISTANCE_EXTRA_DP)) * density
            val targetX = centerX - iconSizePx / 2f + (cos(angleRad) * distancePx).toFloat()
            val targetY = centerY - iconSizePx / 2f + (sin(angleRad) * distancePx).toFloat()
            val rotation = random.nextFloat() * 50f - 25f

            particle.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(POP_DURATION_MS)
                .withEndAction {
                    particle.animate()
                        .x(targetX)
                        .y(targetY)
                        .alpha(0f)
                        .scaleX(0.35f)
                        .scaleY(0.35f)
                        .rotation(rotation)
                        .setDuration(DURATION_MS - POP_DURATION_MS)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                .start()
        }

        overlay.postDelayed({
            root.removeView(overlay)
        }, DURATION_MS + 120L)
    }

    private fun emotionDrawable(type: EmotionType): Int {
        return when (type) {
            EmotionType.FAVOURITE -> R.drawable.emotion_favourite
            EmotionType.LIKE -> R.drawable.emotion_like
            EmotionType.LAUGH -> R.drawable.emotion_laugh
            EmotionType.CRY -> R.drawable.emotion_cry
            EmotionType.ANGRY -> R.drawable.emotion_angry
        }
    }
}
