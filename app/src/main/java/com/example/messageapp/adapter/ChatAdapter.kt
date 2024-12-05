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
import androidx.constraintlayout.widget.ConstraintLayout
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
    var seen: Boolean = false
    private var mCallBack: CallBackClickItem? = null

    fun setOnActionClickItem(callBackClickItem: CallBackClickItem) {
        this.mCallBack = callBackClickItem
    }

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
                holder.checkShowEmotion(message)
                when (TypeMessage.of(message.type)) {
                    TypeMessage.MESSAGE -> {
                        holder.initViewMessage(context, message) {
                            mCallBack?.longClickItemSender(it to message)
                        }
                    }

                    TypeMessage.PHOTOS -> {
                        holder.initViewMultiPhoto(context)
                        drawViewMultiPhoto(holder.v.viewPhotos, message)
                    }

                    TypeMessage.SINGLE_PHOTO -> {
                        holder.initViewSinglePhoto(context)
                        loadSinglePhoto(holder.v.viewPhotos, message)
                    }
                }
                checkShowSeenMessage(holder, position)
                holder.v.optionMenuPhoto.setOnClickListener {
                    mCallBack?.clickOptionMenuPhoto(message)
                }
                holder.v.viewBottom.isVisible = position == messages.size - 1
            }

            else -> {
                holder as ReceiverViewHolder
                holder.checkShowEmotion(message)
                holder.showAvatarReceiver(context, friendId)
                when (TypeMessage.of(message.type)) {
                    TypeMessage.MESSAGE -> {
                        holder.initViewMessage(context, message) {
                            mCallBack?.longClickItemReceiver(it to message)
                        }
                    }

                    TypeMessage.PHOTOS -> {
                        holder.initViewMultiPhoto(context)
                        drawViewMultiPhoto(holder.v.viewPhotos, message, false)
                    }

                    TypeMessage.SINGLE_PHOTO -> {
                        holder.initViewSinglePhoto(context)
                        loadSinglePhoto(holder.v.viewPhotos, message, false)
                    }
                }
                holder.v.optionMenuPhoto.setOnClickListener {
                    mCallBack?.clickOptionMenuPhoto(message)
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
            mCallBack?.clickPhoto(Pair(Pair(message, photo), fromSender))
        }
        viewPhoto.addView(imageView)
        context.loadImg(
            photo,
            imageView,
            imgDefault = if (width < height) R.drawable.bg_grey else R.drawable.bg_grey_horizontal
        )
    }

    private fun drawViewMultiPhoto(viewPhotos: LinearLayout, message: Message, fromSender: Boolean = true) {
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
                        mCallBack?.clickPhoto(Pair(Pair(message, photos[j]), fromSender))
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

    class SenderViewHolder(val v: ItemChatSenderBinding) : RecyclerView.ViewHolder(v.root), ViewTypeMessage {

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

        override fun initViewMessage(context: Context, message: Message, longClick: (View) -> Unit) {
            v.viewMarginBottomMessage.isVisible = message.emotion?.emotionEmpty() == false
            v.viewMarginEmotion.layoutParams = LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.margin_100),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            showViewMessage(true)
            v.tvSender.text = message.message
            v.tvTime.text = DateUtils.convertTimeToHour(message.time)
            v.viewMessage.setOnLongClickListener {
                longClick.invoke(it)
                true
            }
        }

        override fun initViewMultiPhoto(context: Context) {
            initEmotionPhoto(context)
            showViewMessage(false)
        }

        override fun initViewSinglePhoto(context: Context) {
            initEmotionPhoto(context)
            showViewMessage(false)
        }

        private fun initEmotionPhoto(context: Context) {
            v.viewEmotion.isVisible = true
            v.viewMarginEmotion.layoutParams = LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.margin_50),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        fun showSeen(seen: Boolean) {
            this.v.avtSeen.isVisible = seen
            this.v.viewReceived.isVisible = !seen
        }

        private fun showViewMessage(show: Boolean) {
            v.layoutPhoto.isVisible = !show
            v.viewMessage.isVisible = show
            v.viewPhotos.isVisible = !show
        }
    }

    class ReceiverViewHolder(val v: ItemChatReceiverBinding) : RecyclerView.ViewHolder(v.root), ViewTypeMessage {
        fun showAvatarReceiver(context: Context, friendId: String) {
            FireBaseInstance.getInfoUser(friendId) { user ->
                context.loadImg(
                    user.avatar.toString(),
                    v.avatarReceiver
                )
            }
        }

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

        private fun showViewMessage(show: Boolean) {
            v.layoutPhoto.isVisible = !show
            v.viewMessage.isVisible = show
            v.viewPhotos.isVisible = !show
        }

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

        override fun initViewMultiPhoto(context: Context) {
            initEmotionPhoto(context)
            showViewMessage(false)
        }

        override fun initViewSinglePhoto(context: Context) {
            initEmotionPhoto(context)
            showViewMessage(false)
        }

        private fun initEmotionPhoto(context: Context) {
            v.viewEmotion.isVisible = true
            context.calculatorViewMarginEmotion(R.dimen.margin_50)
        }

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

    interface CallBackClickItem {
        fun longClickItemSender(data: (Pair<View, Message>))
        fun longClickItemReceiver(data: (Pair<View, Message>))
        fun clickPhoto(data: (Pair<Pair<Message, String>, Boolean>))
        fun clickOptionMenuPhoto(msg: Message)
    }

    interface ViewTypeMessage {
        fun initViewMessage(context: Context, message: Message, longClick: (View) -> Unit)
        fun initViewMultiPhoto(context: Context)
        fun initViewSinglePhoto(context: Context)
    }
}