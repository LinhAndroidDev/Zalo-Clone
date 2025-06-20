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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter.BaseDiffUtil
import com.example.messageapp.helper.screenHeight
import com.example.messageapp.helper.screenWidth
import com.example.messageapp.model.Message
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FireBaseInstance
import kotlin.math.ceil

const val VIEW_SENDER = 0
const val VIEW_RECEIVER = 1

data class ClickPhotoModel(
    val message: Message,
    val indexOfPhoto: Int,
    val photoData: ArrayList<String>,
    val fromSender: Boolean,
    val imageView: ImageView
)

class ChatAdapter(
    private val context: Context,
    private val friendId: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var messages = arrayListOf<Message>()
    var seen: Boolean = false
    private var mCallBack: CallBackClickItem? = null

    /**
     * This function used to set on click item in chat adapter
     * @param callBackClickItem call back click item
     */
    fun setOnActionClickItem(callBackClickItem: CallBackClickItem) {
        this.mCallBack = callBackClickItem
    }

    /**
     * This function used to update data message
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateDiffList(newList: List<Message>) {
        val diffResult = DiffUtil.calculateDiff(BaseDiffUtil(messages, newList,
            areContentsTheSame = { old, new -> old.time == new.time },
            areItemsTheSame = { old, new -> old == new }
        ))
        messages.clear()
        messages.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * This function used to create view holder for chat adapter
     */
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

    /**
     * This function used to get item count of chat adapter
     */
    override fun getItemCount(): Int = messages.size

    /**
     * This function used to bind view holder for chat adapter
     */
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
                            mCallBack?.onSenderLongClick(it to message)
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

                    TypeMessage.AUDIO -> {
                        holder.initViewAudio(context, message) {
                            mCallBack?.onOptionMenuClick(message)
                        }
                    }
                }
                checkShowSeenMessage(holder, position)
                holder.v.optionMenuPhoto.setOnClickListener {
                    mCallBack?.onOptionMenuClick(message)
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
                            mCallBack?.onReceiverLongClick(it to message)
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

                    TypeMessage.AUDIO -> {
                        holder.initViewAudio(context, message) {
                            mCallBack?.onOptionMenuClick(message)
                        }
                    }
                }
                holder.v.optionMenuPhoto.setOnClickListener {
                    mCallBack?.onOptionMenuClick(message)
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

    /**
     * This function is used to calculate the size of a single photo based on the size returned from the server
     * + Then scale it according to the width and height of the device.
     * + If the width is greater than the height, the width is 3/4 of the screen width - 120
     * + If the height is greater than the width, the height is half of the screen height
     * + Calculate the size of the photo and display the size in advance while loading.
     * @param viewPhoto view photo
     * @param message message
     * @param fromSender from sender or not
     */
    private fun loadSinglePhoto(
        viewPhoto: LinearLayout,
        message: Message,
        fromSender: Boolean = true
    ) {
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
        imageView.transitionName = message.time
        imageView.setOnClickListener {
            mCallBack?.onPhotoClick(
                ClickPhotoModel(
                    message = message,
                    indexOfPhoto = 0,
                    photoData = arrayListOf(photo),
                    fromSender = fromSender,
                    imageView = imageView
                )
            )
        }
        viewPhoto.addView(imageView)
        context.loadImg(
            photo,
            imageView,
            imgDefault = if (width < height) R.drawable.bg_grey else R.drawable.bg_grey_horizontal
        )
    }

    /**
     * This function is used to draw multiple photos to create a gridview-like list with 3 columns.
     * + Calculate the number of rows based on the number of photos by taking size of photos and dividing it by 3
     * + Then add each photo and row each row has 3 photos
     * + Finally add each row to viewPhotos
     * @param viewPhotos view photos
     * @param message message
     * @param fromSender from sender or not
     */
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
                if (j >= photos.size) break
                val imgPhoto = ImageView(context)
                imgPhoto.layoutParams =
                    MarginLayoutParams(screenWidth / 4 - 40, screenWidth / 4 - 40).apply {
                        bottomMargin = if (i == row - 1) 0 else 8
                        rightMargin = if (j == 3 * i + 2) 0 else 8
                    }
                imgPhoto.transitionName = message.time
                imgPhoto.setOnClickListener {
                    mCallBack?.onPhotoClick(
                        ClickPhotoModel(
                            message = message,
                            indexOfPhoto = j,
                            photoData = photos,
                            fromSender = fromSender,
                            imageView = imgPhoto
                        )
                    )
                }
                imgPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
                context.loadImg(photos[j], imgPhoto, imgDefault = R.drawable.bg_grey_equal)
                layoutRow.addView(imgPhoto)
            }
            viewPhotos.addView(layoutRow)
        }

    }

    /**
     * This function used to get view type of chat adapter
     */
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender != friendId) VIEW_SENDER else VIEW_RECEIVER
    }

    /**
     * This interface used to handle click item in chat adapter
     */
    interface CallBackClickItem {
        fun onSenderLongClick(data: (Pair<View, Message>))
        fun onReceiverLongClick(data: (Pair<View, Message>))
        fun onPhotoClick(data: ClickPhotoModel)
        fun onOptionMenuClick(msg: Message)
    }

    /**
     * This interface used to handle view type message
     */
    interface ViewTypeMessage {
        fun initViewMessage(context: Context, message: Message, longClick: (View) -> Unit)
        fun initViewMultiPhoto(context: Context)
        fun initViewSinglePhoto(context: Context)
        fun initViewAudio(context: Context, message: Message, longClick: (View) -> Unit)
    }
}