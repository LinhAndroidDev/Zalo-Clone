package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemChatReceiverBinding
import com.example.messageapp.databinding.ItemChatSenderBinding
import com.example.messageapp.helper.screenHeight
import com.example.messageapp.helper.screenWidth
import com.example.messageapp.model.Message
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.loadImg
import kotlin.math.ceil

const val VIEW_SENDER = 0
const val VIEW_RECEIVER = 1

class ChatAdapter(
    private val context: Context,
    private val friendId: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var messages = arrayListOf<Message>()
    var longClickItemSender: ((Pair<View, Message>) -> Unit)? = null
    var longClickItemReceiver: ((Pair<View, Message>) -> Unit)? = null
    var seen: Boolean = false
    var clickPhoto: ((Pair<Pair<Message, String>, Boolean>) -> Unit)? = null
    var clickOptionMenuPhoto: ((Message) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setMessage(list: ArrayList<Message>) {
        seen = false
        messages.clear()
        messages.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_SENDER -> {
                SenderViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_chat_sender,
                        parent,
                        false
                    )
                )
            }

            else -> {
                ReceiverViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_chat_receiver,
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder.itemViewType) {
            VIEW_SENDER -> {
                holder as SenderViewHolder
                if (message.emotion?.emotionEmpty() == false) {
                    holder.v.viewEmotion.isVisible = true
                    holder.v.viewReleaseEmotion.isVisible = true
                    holder.v.viewReleaseEmotion.updateReleaseEmotion(message.emotion!!)
                } else {
                    holder.v.viewEmotion.isVisible = false
                    holder.v.viewReleaseEmotion.isVisible = false
                }
                when (TypeMessage.of(message.type)) {
                    TypeMessage.MESSAGE -> {
                        holder.v.viewMarginBottomMessage.isVisible = message.emotion?.emotionEmpty() == false
                        holder.v.viewMarginEmotion.layoutParams = LayoutParams(
                            context.resources.getDimensionPixelSize(R.dimen.margin_100),
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        holder.showViewMessage(true)
                        holder.v.tvSender.text = message.message
                        holder.v.tvTime.text = DateUtils.convertTimeToHour(message.time)
                        holder.v.viewMessage.setOnLongClickListener {
                            longClickItemSender?.invoke(it to message)
                            true
                        }
                    }

                    TypeMessage.PHOTOS -> {
                        holder.v.viewEmotion.isVisible = true
                        holder.v.viewMarginEmotion.layoutParams = LayoutParams(
                            context.resources.getDimensionPixelSize(R.dimen.margin_50),
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        holder.showViewMessage(false)
                        holder.v.viewMessage.isVisible = false
                        holder.v.viewPhotos.isVisible = true
                        drawViewPhoto(holder.v.viewPhotos, message)
                    }

                    TypeMessage.SINGLE_PHOTO -> {
                        holder.v.viewEmotion.isVisible = true
                        holder.v.viewMarginEmotion.layoutParams = LayoutParams(
                            context.resources.getDimensionPixelSize(R.dimen.margin_50),
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        holder.showViewMessage(false)
                        holder.v.viewMessage.isVisible = false
                        holder.v.viewPhotos.isVisible = true
                        loadSinglePhoto(holder.v.viewPhotos, message)
                    }
                }
                checkShowSeenMessage(holder, position)
                holder.v.optionMenuPhoto.setOnClickListener {
                    clickOptionMenuPhoto?.invoke(message)
                }
                holder.v.viewBottom.isVisible = position == messages.size - 1
            }

            else -> {
                holder as ReceiverViewHolder
                FireBaseInstance.getInfoUser(friendId) { user ->
                    context.loadImg(
                        user.avatar.toString(),
                        holder.v.avatarReceiver
                    )
                }
                when (TypeMessage.of(message.type)) {
                    TypeMessage.MESSAGE -> {
                        holder.showViewMessage(true)
                        holder.v.tvReceiver.text = message.message
                        holder.v.tvTime.text = DateUtils.convertTimeToHour(message.time)
                        holder.v.layoutMessage.setOnLongClickListener {
                            longClickItemReceiver?.invoke(it to message)
                            true
                        }
                    }

                    TypeMessage.PHOTOS -> {
                        holder.showViewMessage(false)
                        holder.v.viewMessage.isVisible = false
                        holder.v.viewPhotos.isVisible = true
                        drawViewPhoto(holder.v.viewPhotos, message, false)
                    }

                    TypeMessage.SINGLE_PHOTO -> {
                        holder.showViewMessage(false)
                        holder.v.viewMessage.isVisible = false
                        holder.v.viewPhotos.isVisible = true
                        loadSinglePhoto(holder.v.viewPhotos, message, false)
                    }
                }
                holder.v.optionMenuPhoto.setOnClickListener {
                    clickOptionMenuPhoto?.invoke(message)
                }
                holder.v.viewBottom.isVisible = position == messages.size - 1
            }
        }
    }

    /**
     * This function used to check if the message is seen or not
     * @param holder view holder of sender
     * @param position position of message
     */
    private fun checkShowSeenMessage(holder: SenderViewHolder, position: Int) {
        if (position == messages.lastIndex) {
            if (seen) {
                FireBaseInstance.getInfoUser(friendId) { user ->
                    context.loadImg(
                        user.avatar.toString(),
                        holder.v.avtSeen
                    )
                }
                holder.showSeen(true)
            } else {
                holder.showSeen(false)
            }
        } else {
            holder.v.avtSeen.isVisible = false
            holder.v.viewReceived.isVisible = false
        }
    }

    private fun loadSinglePhoto(viewPhoto: LinearLayout, message: Message, fromSender: Boolean = true) {
        val photo = message.singlePhoto[0]
        val width = message.singlePhoto[1].toInt()
        val height = message.singlePhoto[2].toInt()

        viewPhoto.removeAllViews()
        val imageView = ImageView(context)
        val scale = if (width > height) {
            (screenWidth * 3 / 4 - 120) / width.toFloat()
        } else {
            screenHeight / (2 * height).toFloat()
        }
        imageView.layoutParams =
            ViewGroup.LayoutParams((width * scale).toInt(), (height * scale).toInt())
        imageView.setOnClickListener {
            clickPhoto?.invoke(Pair(Pair(message, photo), fromSender))
        }
        viewPhoto.addView(imageView)
        context.loadImg(
            photo,
            imageView,
            imgDefault = if (width < height) R.drawable.bg_grey else R.drawable.bg_grey_horizontal
        )
    }

    private fun drawViewPhoto(viewPhotos: LinearLayout, message: Message, fromSender: Boolean = true) {
        val photos = message.photos
        viewPhotos.removeAllViews()
        val row = ceil(photos.size / 3f).toInt()
        for (i in 0 until row) {
            val layoutRow = LinearLayout(context)
            layoutRow.layoutParams =
                ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            layoutRow.orientation = LinearLayout.HORIZONTAL
            for (j in 3 * i until 3 * i + 3) {
                if (j < photos.size) {
                    val imgPhoto = ImageView(context)
                    imgPhoto.layoutParams =
                        MarginLayoutParams(screenWidth / 4 - 40, screenWidth / 4 - 40).apply {
                            bottomMargin = if (i == row - 1) 0 else 8
                            rightMargin = if (j == 3 * i + 2) 0 else 8
                        }
                    imgPhoto.setOnClickListener {
                        clickPhoto?.invoke(Pair(Pair(message, photos[j]), fromSender))
                    }
                    imgPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
                    context.loadImg(photos[j], imgPhoto, imgDefault = R.drawable.bg_grey_equal)
                    layoutRow.addView(imgPhoto)
                } else {
                    break
                }
            }
            viewPhotos.addView(layoutRow)
        }

    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender != friendId) VIEW_SENDER else VIEW_RECEIVER
    }

    class SenderViewHolder(val v: ItemChatSenderBinding) : RecyclerView.ViewHolder(v.root) {
        fun showSeen(seen: Boolean) {
            this.v.avtSeen.isVisible = seen
            this.v.viewReceived.isVisible = !seen
        }

        fun showViewMessage(show: Boolean) {
            v.layoutPhoto.isVisible = !show
            v.viewMessage.isVisible = show
        }
    }

    class ReceiverViewHolder(val v: ItemChatReceiverBinding) : RecyclerView.ViewHolder(v.root) {
        fun showViewMessage(show: Boolean) {
            v.layoutPhoto.isVisible = !show
            v.viewMessage.isVisible = show
        }
    }
}