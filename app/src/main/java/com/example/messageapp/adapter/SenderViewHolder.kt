package com.example.messageapp.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.adapter.ChatAdapter.ViewTypeMessage
import com.example.messageapp.databinding.ItemChatSenderBinding
import com.example.messageapp.model.Message
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.utils.DateUtils

class SenderViewHolder(val v: ItemChatSenderBinding) : RecyclerView.ViewHolder(v.root),
    ViewTypeMessage {

    // This function used to check if the message has emotion or not of sender
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

    // This function used to show view message of sender
    override fun initViewMessage(
        context: Context,
        message: Message,
        longClick: (View) -> Unit
    ) {
        v.viewMarginBottomMessage.isVisible = message.emotion?.emotionEmpty() == false
        v.viewMarginEmotion.layoutParams = LayoutParams(
            context.resources.getDimensionPixelSize(R.dimen.margin_100),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        showViewMessage(TypeMessage.MESSAGE)
        v.tvSender.text = message.message
        v.tvTime.text = DateUtils.convertTimeToHour(message.time)
        v.viewMessage.setOnLongClickListener {
            longClick.invoke(it)
            true
        }
    }

    // This function used to show view multi photo of sender
    override fun initViewMultiPhoto(context: Context) {
        initEmotionPhoto(context)
        showViewMessage(TypeMessage.PHOTOS)
    }

    // This function used to show view single photo of sender
    override fun initViewSinglePhoto(context: Context) {
        initEmotionPhoto(context)
        showViewMessage(TypeMessage.SINGLE_PHOTO)
    }

    override fun initViewAudio(context: Context, message: Message, longClick: (View) -> Unit) {
        showViewMessage(TypeMessage.AUDIO)
        v.viewRecordWave.loadDataWaveView(context, path = message.audio ?: "")
        v.viewRecordWave.setOnLongClickListener {
            longClick.invoke(it)
            false
        }
    }

    // This function used to init emotion photo of sender
    private fun initEmotionPhoto(context: Context) {
        v.viewEmotion.isVisible = true
        v.viewMarginEmotion.layoutParams = LayoutParams(
            context.resources.getDimensionPixelSize(R.dimen.margin_50),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // This function used to show view seen or receive message of sender
    fun showSeen(seen: Boolean) {
        this.v.avtSeen.isVisible = seen
        this.v.viewReceived.isVisible = !seen
    }


    // This function used to show view message of sender
    private fun showViewMessage(type: TypeMessage) {
        v.viewMessage.isVisible = type == TypeMessage.MESSAGE
        v.layoutPhoto.isVisible = type == TypeMessage.SINGLE_PHOTO || type == TypeMessage.PHOTOS
        v.viewPhotos.isVisible = type == TypeMessage.SINGLE_PHOTO || type == TypeMessage.PHOTOS
        v.viewRecordWave.isVisible = type == TypeMessage.AUDIO
    }
}