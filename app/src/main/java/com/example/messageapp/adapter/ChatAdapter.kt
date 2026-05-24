package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
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
import com.example.messageapp.custom.AudioPlaybackState
import com.example.messageapp.model.Message
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FileUtils.isLikelyVideoUrl
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FireBaseInstance
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

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
    private val isGroup: Boolean = false,
    private val myUserId: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var messages = arrayListOf<Message>()
    private val audioPlaybackStateMap = hashMapOf<String, AudioPlaybackState>()
    var seen: Boolean = false
    private var mCallBack: CallBackClickItem? = null

    companion object {
        /** Khoảng tối đa giữa hai tin cùng người gửi để gộp nhóm (kiểu Zalo / iMessage). */
        private const val MESSAGE_GROUP_GAP_MS = 3 * 60 * 1000L
    }

    init {
        setHasStableIds(true)
    }

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
        val oldList = ArrayList(messages)
        val oldSize = oldList.size
        val diffResult = DiffUtil.calculateDiff(BaseDiffUtil(messages, newList,
            areContentsTheSame = { old, new -> old.time == new.time },
            areItemsTheSame = { old, new -> old == new }
        ))
        messages.clear()
        messages.addAll(newList)
        val newSize = messages.size
        diffResult.dispatchUpdatesTo(this)
        // Khi chỉ nối thêm ở cuối, Diff thường không rebind hàng "cuối cũ" → viewBottom + cluster vẫn như lúc là last.
        val appendedAtEnd = newSize > oldSize && oldSize > 0 &&
            newList.size >= oldSize &&
            (0 until oldSize).all { i -> oldList[i] == newList[i] }
        if (appendedAtEnd) {
            notifyItemChanged(oldSize - 1)
        }
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
                        val key = buildAudioKey(message)
                        val state = audioPlaybackStateMap[key]
                        holder.initViewAudio(context, message, state, { newState ->
                            audioPlaybackStateMap[key] = newState
                        }) {
                            mCallBack?.onOptionMenuClick(message)
                        }
                    }
                }
                checkShowSeenMessage(holder, position)
                holder.v.optionMenuPhoto.setOnClickListener {
                    mCallBack?.onOptionMenuClick(message)
                }
                holder.v.viewBottom.isVisible = position == messages.size - 1
                applyMessageClusterUi(holder, position, message)
            }

            else -> {
                holder as ReceiverViewHolder
                holder.checkShowEmotion(message)
                if (!isGroupedWithPrevious(position)) {
                    val avatarId = if (isGroup) message.sender else friendId
                    holder.showAvatarReceiver(context, avatarId)
                }
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
                        val key = buildAudioKey(message)
                        val state = audioPlaybackStateMap[key]
                        holder.initViewAudio(context, message, state, { newState ->
                            audioPlaybackStateMap[key] = newState
                        }) {
                            mCallBack?.onOptionMenuClick(message)
                        }
                    }
                }
                holder.v.optionMenuPhoto.setOnClickListener {
                    mCallBack?.onOptionMenuClick(message)
                }
                holder.v.viewBottom.isVisible = position == messages.size - 1
                applyMessageClusterUi(holder, position, message)
            }
        }
    }

    private fun messageTimeMillis(msg: Message): Long? =
        DateUtils.parseChatMessageTimeMillis(msg.time)

    /** Tin liền trước cùng người gửi và trong [MESSAGE_GROUP_GAP_MS]. */
    private fun isGroupedWithPrevious(position: Int): Boolean {
        if (position <= 0) return false
        val prev = messages[position - 1]
        val curr = messages[position]
        if (prev.sender != curr.sender) return false
        val tPrev = messageTimeMillis(prev) ?: return false
        val tCurr = messageTimeMillis(curr) ?: return false
        val delta = tCurr - tPrev
        return delta >= 0 && delta <= MESSAGE_GROUP_GAP_MS
    }

    /** Tin liền sau cùng người gửi và trong [MESSAGE_GROUP_GAP_MS]. */
    private fun isGroupedWithNext(position: Int): Boolean {
        if (position >= messages.lastIndex) return false
        val curr = messages[position]
        val next = messages[position + 1]
        if (curr.sender != next.sender) return false
        val tCurr = messageTimeMillis(curr) ?: return false
        val tNext = messageTimeMillis(next) ?: return false
        val delta = tNext - tCurr
        return delta >= 0 && delta <= MESSAGE_GROUP_GAP_MS
    }

    /**
     * Nền bong bóng text phía gửi: nhóm nối theo cạnh phải — góc trên/dưới phải 3dp.
     */
    private fun senderGroupedTextBubbleDrawable(position: Int): Int {
        val prev = isGroupedWithPrevious(position)
        val next = isGroupedWithNext(position)
        return when {
            prev && next -> R.drawable.bg_chat_sender_bubble_group_middle
            prev && !next -> R.drawable.bg_chat_sender_bubble_group_last
            !prev && next -> R.drawable.bg_chat_sender_bubble_group_first
            else -> R.drawable.bg_chat_sender_bubble_single
        }
    }

    /**
     * Nền bong bóng text phía nhận: nhóm nối theo cạnh trái — góc trên/dưới trái 3dp.
     */
    private fun receiverGroupedTextBubbleDrawable(position: Int): Int {
        val prev = isGroupedWithPrevious(position)
        val next = isGroupedWithNext(position)
        return when {
            prev && next -> R.drawable.bg_chat_receiver_bubble_group_middle
            prev && !next -> R.drawable.bg_chat_receiver_bubble_group_last
            !prev && next -> R.drawable.bg_chat_receiver_bubble_group_first
            else -> R.drawable.bg_chat_receiver_bubble_single
        }
    }

    private fun applyItemTopMargin(holder: RecyclerView.ViewHolder, position: Int) {
        val p = holder.itemView.layoutParams as? RecyclerView.LayoutParams ?: return
        val topDp = if (isGroupedWithPrevious(position)) 2f else 5f
        p.topMargin = (topDp * context.resources.displayMetrics.density).toInt()
        holder.itemView.layoutParams = p
    }

    private fun applyReceiverBubbleCluster(holder: ReceiverViewHolder, position: Int) {
        val clusterPrev = isGroupedWithPrevious(position)
        val lp = holder.v.layoutReceiverBubbleColumn.layoutParams as LinearLayout.LayoutParams
        val res = context.resources
        if (clusterPrev) {
            holder.v.avatarReceiver.visibility = View.GONE
            lp.marginStart = res.getDimensionPixelSize(R.dimen.chat_receiver_bubble_margin_start_cluster)
        } else {
            holder.v.avatarReceiver.visibility = View.VISIBLE
            lp.marginStart = res.getDimensionPixelSize(R.dimen.chat_receiver_bubble_margin_start_normal)
        }
        holder.v.layoutReceiverBubbleColumn.layoutParams = lp
    }

    private fun applyMessageClusterUi(holder: RecyclerView.ViewHolder, position: Int, message: Message) {
        applyItemTopMargin(holder, position)
        when (holder) {
            is SenderViewHolder -> {
                if (TypeMessage.of(message.type) == TypeMessage.MESSAGE) {
                    holder.v.tvTime.isVisible = !isGroupedWithNext(position)
                    holder.v.viewMessage.setBackgroundResource(senderGroupedTextBubbleDrawable(position))
                }
            }
            is ReceiverViewHolder -> {
                applyReceiverBubbleCluster(holder, position)
                if (TypeMessage.of(message.type) == TypeMessage.MESSAGE) {
                    holder.v.tvTime.isVisible = !isGroupedWithNext(position)
                    holder.v.viewMessage.setBackgroundResource(receiverGroupedTextBubbleDrawable(position))
                }
            }
        }
    }

    /**
     * This function used to check if the message is seen or not
     * @param holder view holder of sender
     * @param position position of message
     */
    private fun checkShowSeenMessage(holder: SenderViewHolder, position: Int) {
        if (isGroup) {
            holder.showSeen(false)
            holder.v.avtSeen.isVisible = false
            holder.v.viewReceived.isVisible = false
            return
        }
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
     * Kích thước ô bubble cho một ảnh/video đơn (đồng bộ với logic scale cũ).
     */
    private fun bubbleDisplaySizeForPositive(intrinsicW: Int, intrinsicH: Int): Pair<Int, Int> {
        val scale = if (intrinsicW > intrinsicH) {
            (screenWidth * 3 / 4 - 120) / intrinsicW.toFloat()
        } else {
            screenHeight / (2 * intrinsicH.toFloat())
        }
        return (intrinsicW * scale).toInt() to (intrinsicH * scale).toInt()
    }

    /** Parse token "WxH" trong [Message.photoSizes]. */
    private fun parsePhotoSizeToken(sizes: ArrayList<String>?, index: Int): Pair<Int, Int> {
        val token = sizes?.getOrNull(index) ?: return 0 to 0
        val ix = token.indexOf('x')
        if (ix <= 0 || ix == token.lastIndex) return 0 to 0
        val w = token.substring(0, ix).toIntOrNull() ?: return 0 to 0
        val h = token.substring(ix + 1).toIntOrNull() ?: return 0 to 0
        return w to h
    }

    /**
     * Thu nhỏ media vào ô vuông tối đa [maxSide] nhưng giữ tỉ lệ (dùng trong lưới nhiều ảnh/video).
     */
    private fun gridCellDisplaySize(intrinsicW: Int, intrinsicH: Int, maxSide: Int): Pair<Int, Int> {
        if (intrinsicW <= 0 || intrinsicH <= 0) return maxSide to maxSide
        val scale = min(maxSide / intrinsicW.toFloat(), maxSide / intrinsicH.toFloat())
        return max(1, (intrinsicW * scale).toInt()) to max(1, (intrinsicH * scale).toInt())
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
        var width = message.singlePhoto[1].toInt()
        var height = message.singlePhoto[2].toInt()
        if (width <= 0 || height <= 0) {
            width = screenWidth / 2
            height = screenWidth / 2
        }

        viewPhoto.removeAllViews()
        val imageView = ImageView(context)
        val (w, h) = bubbleDisplaySizeForPositive(width, height)
        imageView.layoutParams = ViewGroup.LayoutParams(w, h)
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
        if (isLikelyVideoUrl(photo)) {
            val frame = FrameLayout(context)
            frame.layoutParams = ViewGroup.LayoutParams(w, h)
            imageView.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            frame.addView(imageView)
            val playSize = (32 * context.resources.displayMetrics.density).toInt()
            val play = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(playSize, playSize, Gravity.CENTER)
                setImageResource(R.drawable.ic_play)
                scaleType = ImageView.ScaleType.FIT_CENTER
                isClickable = false
            }
            frame.addView(play)
            viewPhoto.addView(frame)
        } else {
            viewPhoto.addView(imageView)
        }
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
        if (photos.isEmpty()) return

        val row = ceil(photos.size / 3f).toInt()
        val cellMax = screenWidth / 4 - 40
        for (i in 0 until row) {
            val layoutRow = LinearLayout(context)
            layoutRow.layoutParams =
                ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            layoutRow.orientation = LinearLayout.HORIZONTAL
            layoutRow.gravity = Gravity.BOTTOM
            for (j in 3 * i until 3 * i + 3) {
                if (j >= photos.size) break
                val (iw, ih) = parsePhotoSizeToken(message.photoSizes, j)
                val (fw, fh) = gridCellDisplaySize(iw, ih, cellMax)
                val frame = FrameLayout(context)
                frame.layoutParams =
                    MarginLayoutParams(fw, fh).apply {
                        bottomMargin = if (i == row - 1) 0 else 8
                        rightMargin = if (j == 3 * i + 2) 0 else 8
                    }
                val imgPhoto = ImageView(context)
                imgPhoto.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
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
                frame.addView(imgPhoto)
                if (isLikelyVideoUrl(photos[j])) {
                    val playSize = (28 * context.resources.displayMetrics.density).toInt()
                    val play = ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(playSize, playSize, Gravity.CENTER)
                        setImageResource(R.drawable.ic_play)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        isClickable = false
                    }
                    frame.addView(play)
                }
                layoutRow.addView(frame)
            }
            viewPhotos.addView(layoutRow)
        }
    }

    /**
     * This function used to get view type of chat adapter
     */
    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return if (!isGroup) {
            if (msg.sender != friendId) VIEW_SENDER else VIEW_RECEIVER
        } else {
            if (msg.sender == myUserId) VIEW_SENDER else VIEW_RECEIVER
        }
    }

    override fun getItemId(position: Int): Long {
        return messages[position].time.hashCode().toLong()
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is SenderViewHolder -> holder.v.viewRecordWave.pause()
            is ReceiverViewHolder -> holder.v.viewRecordWave.pause()
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        when (holder) {
            is SenderViewHolder -> holder.v.viewRecordWave.pause()
            is ReceiverViewHolder -> holder.v.viewRecordWave.pause()
        }
    }

    private fun buildAudioKey(message: Message): String = "${message.time}_${message.audio.orEmpty()}"

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
        fun initViewAudio(
            context: Context,
            message: Message,
            state: AudioPlaybackState?,
            onStateChanged: (AudioPlaybackState) -> Unit,
            longClick: (View) -> Unit
        )
    }
}