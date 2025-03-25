package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.example.messageapp.databinding.LayoutReleaseEmotionBinding
import com.example.messageapp.model.Emotion

class ReleaseEmotionView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var binding: LayoutReleaseEmotionBinding? = null

    init {
        binding = LayoutReleaseEmotionBinding.inflate(LayoutInflater.from(context))
        binding?.let { addView(it.root) }
    }

    fun updateReleaseEmotion(dataEmotion: Emotion) {
        binding?.emotionFavourite?.isVisible = dataEmotion.favourite.isNotEmpty()
        binding?.emotionLaugh?.isVisible = dataEmotion.laugh.isNotEmpty()
        binding?.emotionLike?.isVisible = dataEmotion.like.isNotEmpty()
        binding?.emotionCry?.isVisible = dataEmotion.cry.isNotEmpty()
        binding?.emotionAngry?.isVisible = dataEmotion.angry.isNotEmpty()

        if (dataEmotion.emotionEmpty()) {
            binding?.tvQuantityEmotion?.isVisible = false
        } else {
            binding?.tvQuantityEmotion?.isVisible = true
            binding?.tvQuantityEmotion?.text = dataEmotion.totalQuantityEmotion().toString()
        }
    }
}