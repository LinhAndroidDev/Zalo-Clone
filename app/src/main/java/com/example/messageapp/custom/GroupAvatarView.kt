package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.databinding.ViewGroupAvatarBinding
import com.example.messageapp.utils.FileUtils.loadImg

class GroupAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewGroupAvatarBinding =
        ViewGroupAvatarBinding.inflate(LayoutInflater.from(context), this)

    private val avatarSlots by lazy {
        listOf(binding.slot1, binding.slot2, binding.slot3)
    }

    init {
        setBackgroundResource(R.drawable.bg_grey_equal)
    }

    fun showSinglePhoto(url: String) {
        hideCompositeSlots()
        binding.layoutComposite.isVisible = false
        binding.imgSingle.isVisible = true
        setBackgroundResource(R.drawable.bg_grey_equal)
        context.loadImg(url, binding.imgSingle, R.drawable.bg_grey_equal)
    }

    fun showPlaceholder() {
        clearImageLoads()
        binding.imgSingle.isVisible = false
        binding.layoutComposite.isVisible = false
        hideCompositeSlots()
        setBackgroundResource(R.drawable.bg_grey_equal)
    }

    fun bindMemberAvatars(avatarUrls: List<String>, totalMemberCount: Int) {
        if (avatarUrls.isEmpty() || totalMemberCount <= 0) {
            showPlaceholder()
            return
        }

        clearImageLoads()
        setBackgroundResource(android.R.color.transparent)
        binding.imgSingle.isVisible = false
        binding.layoutComposite.isVisible = true
        hideCompositeSlots()

        val urls = avatarUrls.take(3)
        when (totalMemberCount) {
            1 -> showOne(urls.firstOrNull().orEmpty())
            2 -> showTwo(
                urls.getOrElse(0) { "" },
                urls.getOrElse(1) { "" },
            )
            3 -> showThree(
                urls.getOrElse(0) { "" },
                urls.getOrElse(1) { "" },
                urls.getOrElse(2) { "" },
            )
            else -> showSquareWithMore(
                urls.getOrElse(0) { "" },
                urls.getOrElse(1) { "" },
                urls.getOrElse(2) { "" },
                totalMemberCount - 3,
            )
        }
    }

    fun reset() {
        tag = null
        showPlaceholder()
    }

    private fun showOne(url: String) {
        val inset = (CONTAINER_SIZE - AVATAR_SIZE_ONE) / 2
        layoutSlotAt(binding.slot1, AVATAR_SIZE_ONE, inset, inset, elevation = 1f)
        binding.slot1.isVisible = true
        loadSlot(url, binding.slot1)
    }

    private fun showTwo(leftUrl: String, rightUrl: String) {
        val size = AVATAR_SIZE_TWO
        val overlap = OVERLAP_TWO
        val totalWidth = size * 2 - overlap
        val startX = (CONTAINER_SIZE - totalWidth) / 2
        val startY = (CONTAINER_SIZE - size) / 2

        layoutSlotAt(binding.slot1, size, xDp = startX, yDp = startY, elevation = 1f)
        layoutSlotAt(binding.slot2, size, xDp = startX + size - overlap, yDp = startY, elevation = 2f)
        binding.slot1.isVisible = true
        binding.slot2.isVisible = true
        loadSlot(leftUrl, binding.slot1)
        loadSlot(rightUrl, binding.slot2)
    }

    private fun showThree(topUrl: String, bottomLeftUrl: String, bottomRightUrl: String) {
        layoutSlotAt(binding.slot1, AVATAR_SIZE_THREE, xDp = 13, yDp = 2, elevation = 3f)
        layoutSlotAt(binding.slot2, AVATAR_SIZE_THREE, xDp = 2, yDp = 24, elevation = 1f)
        layoutSlotAt(binding.slot3, AVATAR_SIZE_THREE, xDp = 24, yDp = 24, elevation = 2f)
        binding.slot1.isVisible = true
        binding.slot2.isVisible = true
        binding.slot3.isVisible = true
        loadSlot(topUrl, binding.slot1)
        loadSlot(bottomLeftUrl, binding.slot2)
        loadSlot(bottomRightUrl, binding.slot3)
    }

    private fun showSquareWithMore(
        topLeftUrl: String,
        topRightUrl: String,
        bottomLeftUrl: String,
        moreCount: Int,
    ) {
        val step = AVATAR_SIZE_SQUARE - OVERLAP_SQUARE
        layoutSlotAt(binding.slot1, AVATAR_SIZE_SQUARE, xDp = 4, yDp = 4, elevation = 1f)
        layoutSlotAt(binding.slot2, AVATAR_SIZE_SQUARE, xDp = 4 + step, yDp = 4, elevation = 2f)
        layoutSlotAt(binding.slot3, AVATAR_SIZE_SQUARE, xDp = 4, yDp = 4 + step, elevation = 3f)
        layoutSlotAt(binding.tvMore, AVATAR_SIZE_SQUARE, xDp = 4 + step, yDp = 4 + step, elevation = 4f)

        binding.slot1.isVisible = true
        binding.slot2.isVisible = true
        binding.slot3.isVisible = true
        binding.tvMore.isVisible = true
        binding.tvMore.text = "$moreCount"

        loadSlot(topLeftUrl, binding.slot1)
        loadSlot(topRightUrl, binding.slot2)
        loadSlot(bottomLeftUrl, binding.slot3)
    }

    private fun layoutSlotAt(
        view: View,
        sizeDp: Int,
        xDp: Int,
        yDp: Int,
        elevation: Float,
    ) {
        val density = resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val layoutParams = LayoutParams(sizePx, sizePx, Gravity.START or Gravity.TOP)
        layoutParams.marginStart = (xDp * density).toInt()
        layoutParams.topMargin = (yDp * density).toInt()
        view.layoutParams = layoutParams
        view.elevation = elevation
    }

    private fun hideCompositeSlots() {
        avatarSlots.forEach { it.isVisible = false }
        binding.tvMore.isVisible = false
    }

    private fun loadSlot(url: String, imageView: ImageView) {
        if (url.isBlank()) {
            imageView.setImageResource(R.drawable.bg_grey_equal)
        } else {
            context.loadImg(url, imageView, R.drawable.bg_grey_equal)
        }
    }

    private fun clearImageLoads() {
        val targets = listOf(binding.imgSingle) + avatarSlots
        targets.forEach { Glide.with(context).clear(it) }
    }

    private companion object {
        const val CONTAINER_SIZE = 60
        const val AVATAR_SIZE_ONE = 56
        const val AVATAR_SIZE_TWO = 40
        const val OVERLAP_TWO = 20
        const val AVATAR_SIZE_THREE = 34
        const val AVATAR_SIZE_SQUARE = 28
        const val OVERLAP_SQUARE = 6
    }
}
