package com.example.messageapp.custom

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.databinding.LayoutChatTypingBannerBinding
import com.example.messageapp.model.TypingUiState
import com.example.messageapp.model.TypingUserUi
import com.example.messageapp.utils.FileUtils.loadImg
import de.hdodenhof.circleimageview.CircleImageView

class ChatTypingBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutChatTypingBannerBinding =
        LayoutChatTypingBannerBinding.inflate(LayoutInflater.from(context), this)

    private val avatarSlots: List<CircleImageView> by lazy {
        listOf(binding.avatarSlot1, binding.avatarSlot2, binding.avatarSlot3)
    }

    private val avatarSizePx = (24 * resources.displayMetrics.density).toInt()
    private val overlapPx = (8 * resources.displayMetrics.density).toInt()

    fun bind(state: TypingUiState) {
        isVisible = state.isVisible
        if (!state.isVisible) {
            hideAvatars()
            binding.tvTypingLabel.text = ""
            return
        }
        binding.tvTypingLabel.text = state.label
        renderAvatars(state.users.take(3))
    }

    private fun hideAvatars() {
        binding.typingAvatarStack.isVisible = false
        avatarSlots.forEach { slot ->
            slot.isVisible = false
            slot.setImageResource(R.drawable.bg_grey_equal)
        }
    }

    private fun renderAvatars(users: List<TypingUserUi>) {
        val stack = binding.typingAvatarStack
        stack.isVisible = users.isNotEmpty()
        if (users.isEmpty()) {
            hideAvatars()
            return
        }

        val totalWidth = avatarSizePx + (users.size - 1) * (avatarSizePx - overlapPx)
        stack.layoutParams = stack.layoutParams.apply {
            width = totalWidth
            height = avatarSizePx
        }

        avatarSlots.forEachIndexed { index, slot ->
            val user = users.getOrNull(index)
            if (user == null) {
                slot.isVisible = false
                slot.setImageResource(R.drawable.bg_grey_equal)
                return@forEachIndexed
            }
            slot.layoutParams = FrameLayout.LayoutParams(avatarSizePx, avatarSizePx).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                marginStart = index * (avatarSizePx - overlapPx)
            }
            slot.elevation = index.toFloat()
            slot.isVisible = true
            context.loadImg(user.avatarUrl, slot, R.drawable.bg_grey_equal)
        }
    }
}
