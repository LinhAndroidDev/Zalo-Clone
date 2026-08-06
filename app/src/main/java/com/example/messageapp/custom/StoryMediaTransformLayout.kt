package com.example.messageapp.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import com.example.messageapp.model.StoryMediaTransform
import kotlin.math.atan2
import kotlin.math.min

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

    private var mediaWidth = 0
    private var mediaHeight = 0
    private var minScale = DEFAULT_MIN_SCALE
    private var maxScale = DEFAULT_MAX_SCALE
    private var fitWidthScale = 1f

    var isTransformEnabled = true

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentScale = (currentScale * detector.scaleFactor).coerceIn(minScale, maxScale)
                applyTransform()
                return true
            }
        },
    )

    init {
        isClickable = true
        isFocusable = true
        clipChildren = false
        clipToPadding = false
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount == 1) {
            transformTarget = getChildAt(0)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mediaWidth > 0 && mediaHeight > 0) {
            updateScaleLimits()
        }
    }

    fun clearMediaSize() {
        mediaWidth = 0
        mediaHeight = 0
        minScale = DEFAULT_MIN_SCALE
        maxScale = DEFAULT_MAX_SCALE
        fitWidthScale = 1f
    }

    fun configureMediaSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        mediaWidth = width
        mediaHeight = height
        updateScaleLimits()
    }

    fun resetToFitWidth() {
        if (mediaWidth > 0 && mediaHeight > 0) {
            updateScaleLimits()
            currentScale = fitWidthScale
        } else {
            currentScale = 1f
        }
        currentRotation = 0f
        translationX = 0f
        translationY = 0f
        applyTransform()
    }

    fun resetTransform() {
        resetToFitWidth()
    }

    fun captureTransformState(): StoryMediaTransform {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        return StoryMediaTransform(
            scale = currentScale,
            rotation = currentRotation,
            translationXNorm = translationX / w,
            translationYNorm = translationY / h,
        )
    }

    fun applyTransformState(state: StoryMediaTransform) {
        currentScale = state.scale.coerceIn(minScale, maxScale)
        currentRotation = state.rotation
        if (width == 0 || height == 0) {
            post { applyTransformState(state) }
            return
        }
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        translationX = state.translationXNorm * w
        translationY = state.translationYNorm * h
        applyTransform()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isTransformEnabled) return false
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

    private fun updateScaleLimits() {
        val containerW = width.toFloat()
        val containerH = height.toFloat()
        if (containerW <= 0f || containerH <= 0f || mediaWidth <= 0 || mediaHeight <= 0) {
            return
        }

        val imageW = mediaWidth.toFloat()
        val imageH = mediaHeight.toFloat()
        val fitCenterScale = min(containerW / imageW, containerH / imageH)
        val fitWidthAbsoluteScale = containerW / imageW

        fitWidthScale = (fitWidthAbsoluteScale / fitCenterScale).coerceAtLeast(1f)
        // fitCenter base: zooming out only shrinks the full image (letterbox), never crops top/bottom.
        minScale = DEFAULT_MIN_SCALE
        maxScale = fitWidthScale * DEFAULT_MAX_SCALE
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
        private const val DEFAULT_MIN_SCALE = 0.1f
        private const val DEFAULT_MAX_SCALE = 5f
    }
}
