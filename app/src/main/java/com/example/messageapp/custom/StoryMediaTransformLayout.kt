package com.example.messageapp.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import kotlin.math.atan2

class StoryMediaTransformLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var transformTarget: View? = null
    private var currentScale = 1f
    private var currentRotation = 0f
    private var translationX = 0f
    private var translationY = 0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastRotationDegrees = 0f
    private var isMultiTouch = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentScale = (currentScale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
                applyTransform()
                return true
            }
        },
    )

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount == 1) {
            transformTarget = getChildAt(0)
        }
    }

    fun resetTransform() {
        currentScale = 1f
        currentRotation = 0f
        translationX = 0f
        translationY = 0f
        applyTransform()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isMultiTouch = false
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    isMultiTouch = true
                    lastRotationDegrees = rotationDegrees(event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val degrees = rotationDegrees(event)
                    currentRotation += degrees - lastRotationDegrees
                    lastRotationDegrees = degrees
                    applyTransform()
                } else if (!isMultiTouch) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    translationX += dx
                    translationY += dy
                    lastTouchX = event.x
                    lastTouchY = event.y
                    applyTransform()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 1) {
                    isMultiTouch = false
                }
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        return true
    }

    private fun applyTransform() {
        val target = transformTarget ?: return
        if (target.width == 0 || target.height == 0) {
            target.post { applyTransform() }
            return
        }
        target.pivotX = target.width / 2f
        target.pivotY = target.height / 2f
        target.scaleX = currentScale
        target.scaleY = currentScale
        target.rotation = currentRotation
        target.translationX = translationX
        target.translationY = translationY
    }

    private fun rotationDegrees(event: MotionEvent): Float {
        val deltaX = event.getX(0) - event.getX(1)
        val deltaY = event.getY(0) - event.getY(1)
        return Math.toDegrees(atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
    }

    companion object {
        private const val MIN_SCALE = 0.4f
        private const val MAX_SCALE = 5f
    }
}
