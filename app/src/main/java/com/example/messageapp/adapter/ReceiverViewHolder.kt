package com.example.messageapp.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.adapter.ChatAdapter.ViewTypeMessage
import com.example.messageapp.databinding.ItemChatReceiverBinding
import com.example.messageapp.model.Message
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FireBaseInstance

class ReceiverViewHolder(val v: ItemChatReceiverBinding) : RecyclerView.ViewHolder(v.root),
    ViewTypeMessage {

    // This function used to show avatar receiver
    fun showAvatarReceiver(context: Context, friendId: String) {
        FireBaseInstance.getInfoUser(friendId) { user ->
            context.loadImg(
                user.avatar.toString(),
                v.avatarReceiver
            )
        }
    }

    // This function used to check show emotion of receiver
    fun checkShowEmotion(message: Message) {
        if (message.emotion?.emotionEmpty() == false) {
            v.viewEmotion.isVisible = true
            v.viewReleaseEmotion.isVisible = true
            v.viewReleaseEmotion.updateReleaseEmotion(message.emotion!!)
        } else {
            v.viewEmotion.isVisible = false
            v.viewReleaseEmotion.isVisible = false
        }
    }

    // This function used to show view message of receiver
    private fun showViewMessage(show: Boolean) {
        v.layoutPhoto.isVisible = !show
        v.viewMessage.isVisible = show
        v.viewPhotos.isVisible = !show
    }

    // This function used to show view message of receiver
    override fun initViewMessage(
        context: Context,
        message: Message,
        longClick: (View) -> Unit
    ) {
        v.viewMarginBottomMessage.isVisible = message.emotion?.emotionEmpty() == false
        context.calculatorViewMarginEmotion(R.dimen.margin_100)
        showViewMessage(true)
        v.tvReceiver.text = message.message
        v.tvTime.text = DateUtils.convertTimeToHour(message.time)
        v.viewMessage.setOnLongClickListener {
            longClick.invoke(it)
            true
        }
    }

    // This function used to show view multi photo of receiver
    override fun initViewMultiPhoto(context: Context) {
        initEmotionPhoto(context)
        showViewMessage(false)
    }

    // This function used to show view single photo of receiver
    override fun initViewSinglePhoto(context: Context) {
        initEmotionPhoto(context)
        showViewMessage(false)
    }

    // This function used to init emotion photo of receiver
    private fun initEmotionPhoto(context: Context) {
        v.viewEmotion.isVisible = true
        context.calculatorViewMarginEmotion(R.dimen.margin_50)
    }

    // This function used to calculate view margin emotion of receiver
    private fun Context.calculatorViewMarginEmotion(margin: Int) {
        val params = ConstraintLayout.LayoutParams(
            resources.getDimensionPixelSize(margin),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        v.viewMarginEmotion.layoutParams = params
    }
}