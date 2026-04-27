package com.example.messageapp.helper

import android.content.Context
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.messageapp.R

/**
 * Renders the same multi-image grid as [com.example.messageapp.fragment.StatusFragment]
 * (1 / 2 / 3 / 4 / 5+ layouts with optional remove controls and "+N" overlay).
 */
object StatusMediaGridLayout {

    fun render(
        context: Context,
        container: FrameLayout,
        uris: List<Uri>,
        spacingPx: Int,
        showRemoveControls: Boolean = false,
        onRemove: ((Int) -> Unit)? = null,
        onOpenPreview: ((Int) -> Unit)? = null
    ) {
        container.removeAllViews()
        if (uris.isEmpty()) return

        when (uris.size) {
            1 -> renderOne(context, container, uris, spacingPx, showRemoveControls, onRemove, onOpenPreview)
            2 -> renderTwo(context, container, uris, spacingPx, showRemoveControls, onRemove, onOpenPreview)
            3 -> renderThree(context, container, uris, spacingPx, showRemoveControls, onRemove, onOpenPreview)
            4 -> renderFour(context, container, uris, spacingPx, showRemoveControls, onRemove, onOpenPreview)
            else -> renderFiveOrMore(context, container, uris, spacingPx, showRemoveControls, onRemove, onOpenPreview)
        }
    }

    private fun renderOne(
        context: Context,
        container: FrameLayout,
        uris: List<Uri>,
        spacingPx: Int,
        showRemoveControls: Boolean,
        onRemove: ((Int) -> Unit)?,
        onOpenPreview: ((Int) -> Unit)?
    ) {
        val layout = rowLayout(context)
        layout.addView(createSingleAdaptiveCell(context, uris, 0, showRemoveControls, onRemove, onOpenPreview))
        container.addView(layout)
    }

    private fun renderTwo(
        context: Context,
        container: FrameLayout,
        uris: List<Uri>,
        spacingPx: Int,
        showRemoveControls: Boolean,
        onRemove: ((Int) -> Unit)?,
        onOpenPreview: ((Int) -> Unit)?
    ) {
        val layout = rowLayout(context)
        layout.addView(createCell(context, uris, 0, 180, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        layout.addView(spaceView(context, spacingPx, vertical = false))
        layout.addView(createCell(context, uris, 1, 180, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        container.addView(layout)
    }

    private fun renderThree(
        context: Context,
        container: FrameLayout,
        uris: List<Uri>,
        spacingPx: Int,
        showRemoveControls: Boolean,
        onRemove: ((Int) -> Unit)?,
        onOpenPreview: ((Int) -> Unit)?
    ) {
        val root = columnLayout(context)
        root.addView(createCell(context, uris, 0, 190, null, null, showRemoveControls, onRemove, onOpenPreview))
        root.addView(spaceView(context, spacingPx, vertical = true))
        val row = rowLayout(context)
        row.addView(createCell(context, uris, 1, 130, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        row.addView(spaceView(context, spacingPx, vertical = false))
        row.addView(createCell(context, uris, 2, 130, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        root.addView(row)
        container.addView(root)
    }

    private fun renderFour(
        context: Context,
        container: FrameLayout,
        uris: List<Uri>,
        spacingPx: Int,
        showRemoveControls: Boolean,
        onRemove: ((Int) -> Unit)?,
        onOpenPreview: ((Int) -> Unit)?
    ) {
        val root = columnLayout(context)
        val top = rowLayout(context)
        top.addView(createCell(context, uris, 0, 130, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        top.addView(spaceView(context, spacingPx, vertical = false))
        top.addView(createCell(context, uris, 1, 130, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        val bottom = rowLayout(context).apply { applyTopMargin(this, spacingPx) }
        bottom.addView(createCell(context, uris, 2, 130, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        bottom.addView(spaceView(context, spacingPx, vertical = false))
        bottom.addView(createCell(context, uris, 3, 130, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        root.addView(top)
        root.addView(bottom)
        container.addView(root)
    }

    private fun renderFiveOrMore(
        context: Context,
        container: FrameLayout,
        uris: List<Uri>,
        spacingPx: Int,
        showRemoveControls: Boolean,
        onRemove: ((Int) -> Unit)?,
        onOpenPreview: ((Int) -> Unit)?
    ) {
        val root = columnLayout(context)
        val top = rowLayout(context)
        top.addView(createCell(context, uris, 0, 120, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        top.addView(spaceView(context, spacingPx, vertical = false))
        top.addView(createCell(context, uris, 1, 120, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        val bottom = rowLayout(context).apply { applyTopMargin(this, spacingPx) }
        bottom.addView(createCell(context, uris, 2, 120, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        bottom.addView(spaceView(context, spacingPx, vertical = false))
        bottom.addView(createCell(context, uris, 3, 120, 1f, null, showRemoveControls, onRemove, onOpenPreview))
        bottom.addView(spaceView(context, spacingPx, vertical = false))
        val extra = uris.size - 5
        bottom.addView(
            createCell(
                context, uris, 4, 120, 1f,
                overlayMoreCount = if (extra > 0) extra else null,
                showRemoveControls, onRemove, onOpenPreview
            )
        )
        root.addView(top)
        root.addView(bottom)
        container.addView(root)
    }

    private fun createCell(
        context: Context,
        uris: List<Uri>,
        index: Int,
        heightDp: Int,
        weight: Float?,
        overlayMoreCount: Int?,
        showRemoveControls: Boolean,
        onRemove: ((Int) -> Unit)?,
        onOpenPreview: ((Int) -> Unit)?
    ): ViewGroup {
        val frame = FrameLayout(context).apply {
            val heightPx = dp(context, heightDp)
            layoutParams = if (weight != null) {
                LinearLayout.LayoutParams(0, heightPx, weight)
            } else {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
            }
            background = context.getDrawable(R.drawable.bg_grey_equal)
            clipToOutline = true
        }

        val imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        frame.addView(imageView)

        if (index in uris.indices) {
            Glide.with(imageView)
                .load(uris[index])
                .placeholder(R.drawable.bg_grey_equal)
                .error(R.drawable.bg_grey_equal)
                .into(imageView)
        }

        if (overlayMoreCount != null && overlayMoreCount > 0) {
            val overlay = TextView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                text = "+$overlayMoreCount"
                setTextColor(context.getColor(R.color.white))
                textSize = 26f
                setBackgroundColor(0x66000000)
            }
            frame.addView(overlay)
        }

        if (showRemoveControls) {
            val removeBtn = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dp(context, 22), dp(context, 22), Gravity.TOP or Gravity.END).apply {
                    topMargin = dp(context, 6)
                    marginEnd = dp(context, 6)
                }
                background = context.getDrawable(R.drawable.bg_circle)
                backgroundTintList = android.content.res.ColorStateList.valueOf(0x99000000.toInt())
                setPadding(dp(context, 5), dp(context, 5), dp(context, 5), dp(context, 5))
                setImageResource(R.drawable.ic_close)
                imageTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.white))
            }
            frame.addView(removeBtn)
            removeBtn.setOnClickListener {
                if (index in uris.indices) onRemove?.invoke(index)
            }
        }

        frame.setOnClickListener {
            if (index in uris.indices) onOpenPreview?.invoke(index)
        }

        return frame
    }

    private fun createSingleAdaptiveCell(
        context: Context,
        uris: List<Uri>,
        index: Int,
        showRemoveControls: Boolean,
        onRemove: ((Int) -> Unit)?,
        onOpenPreview: ((Int) -> Unit)?
    ): ViewGroup {
        val frame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            background = context.getDrawable(R.drawable.bg_grey_equal)
            clipToOutline = true
        }

        val imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        frame.addView(imageView)

        if (index in uris.indices) {
            Glide.with(imageView)
                .load(uris[index])
                .placeholder(R.drawable.bg_grey_equal)
                .error(R.drawable.bg_grey_equal)
                .into(imageView)
        }

        if (showRemoveControls) {
            val removeBtn = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dp(context, 22), dp(context, 22), Gravity.TOP or Gravity.END).apply {
                    topMargin = dp(context, 6)
                    marginEnd = dp(context, 6)
                }
                background = context.getDrawable(R.drawable.bg_circle)
                backgroundTintList = android.content.res.ColorStateList.valueOf(0x99000000.toInt())
                setPadding(dp(context, 5), dp(context, 5), dp(context, 5), dp(context, 5))
                setImageResource(R.drawable.ic_close)
                imageTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.white))
            }
            frame.addView(removeBtn)
            removeBtn.setOnClickListener {
                if (index in uris.indices) onRemove?.invoke(index)
            }
        }

        frame.setOnClickListener {
            if (index in uris.indices) onOpenPreview?.invoke(index)
        }

        return frame
    }

    private fun rowLayout(context: Context) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun columnLayout(context: Context) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun spaceView(context: Context, spacingPx: Int, vertical: Boolean): ViewGroup {
        return FrameLayout(context).apply {
            layoutParams = if (vertical) {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spacingPx)
            } else {
                LinearLayout.LayoutParams(spacingPx, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }
    }

    private fun applyTopMargin(view: ViewGroup, value: Int) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        params.topMargin = value
        view.layoutParams = params
    }

    fun spacingPxDefault(context: Context): Int = dp(context, 4)

    fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
