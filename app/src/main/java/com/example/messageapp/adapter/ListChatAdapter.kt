package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.library.swipe.SwipeRevealLayout
import com.example.messageapp.library.swipe.ViewBinderHelper
import com.example.messageapp.databinding.ItemListChatBinding
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.UserPresence
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FireBaseInstance
import com.google.firebase.firestore.ListenerRegistration

class ListChatAdapter(private val userId: String) :
    BaseAdapter<Conversation, ItemListChatBinding>() {

    private val binderHelper = ViewBinderHelper()
    var onClickView: ((Conversation) -> Unit)? = null
    var showOptionConversation: (() -> Unit)? = null
    var indexOpenSwipe: Int? = null
    private var presenceMap: Map<String, UserPresence> = emptyMap()

    override fun getLayout(): Int = R.layout.item_list_chat

    override fun onBindViewHolder(holder: BaseViewHolder<ItemListChatBinding>, @SuppressLint("RecyclerView") position: Int) {
        if (holder.v.swipeLayout.isOpened) binderHelper.closeLayout(indexOpenSwipe.toString())
        binderHelper.bind(holder.v.swipeLayout, position.toString())
        binderHelper.setOpenOnlyOne(true)
        holder.initView(position)
    }

    override fun onViewRecycled(holder: BaseViewHolder<ItemListChatBinding>) {
        holder.clearGroupTypingListener()
        super.onViewRecycled(holder)
    }

    fun updateDiffConversation(conversations : ArrayList<Conversation>) {
        updateDiffList(conversations,
            compareItem = { old, new ->
                old.friendId == new.friendId && old.isGroupThread() == new.isGroupThread()
            },
            compareContent = { old, new -> old == new }
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updatePresenceMap(map: Map<String, UserPresence>) {
        if (presenceMap == map) return
        presenceMap = map
        notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun BaseViewHolder<ItemListChatBinding>.initView(position: Int) {
        val conversation = items[position]
        v.tvNameFriend.text = conversation.name
        clearGroupTypingListener()
        if (conversation.isGroupThread()) {
            val registration = FireBaseInstance.observeGroupTyping(
                groupId = conversation.friendId,
                myUserId = userId,
            ) { isTyping ->
                v.typingView.isVisible = isTyping
                v.tvMessage.isVisible = !isTyping
            }
            itemView.setTag(R.id.tag_group_typing_listener, registration)
        } else {
            FireBaseInstance.getConversationRlt(conversation.friendId, userId) { cvt ->
                v.typingView.isVisible = cvt.typing
                v.tvMessage.isVisible = !cvt.typing
            }
        }
        v.tvMessage.text = "${conversation.person}: ${conversation.message}"
        v.tvTime.text = DateUtils.formatTime(conversation.time)
        bindOnlineIndicator(conversation)
        this.handleWhenConversationIsChanged(conversation)
        if (conversation.isGroupThread()) {
            if (conversation.friendImage.isNotBlank()) {
                itemView.context.loadImg(conversation.friendImage, v.avatarFriend)
                itemView.context.loadImg(conversation.friendImage, v.avtSeen)
            } else {
                v.avatarFriend.setImageResource(R.drawable.bg_grey_equal)
                v.avtSeen.setImageResource(R.drawable.bg_grey_equal)
            }
        } else {
            FireBaseInstance.getInfoUser(conversation.friendId) { user ->
                itemView.context.loadImg(user.avatar.toString(), v.avatarFriend)
                itemView.context.loadImg(user.avatar.toString(), v.avtSeen)
            }
        }
        v.itemChat.setOnClickListener {
            onClickView?.invoke(conversation)
        }
        v.seeMore.setOnClickListener {
            notifyDataSetChanged()
            showOptionConversation?.invoke()
        }
        v.swipeLayout.setSwipeListener(object : SwipeRevealLayout.SwipeListener {
            override fun onClosed(view: SwipeRevealLayout?) {
                v.itemChat.isClickable = true
                if (indexOpenSwipe == position) indexOpenSwipe = null
            }

            override fun onOpened(view: SwipeRevealLayout?) {
                v.itemChat.isClickable = false
                if (indexOpenSwipe != position) {
                    indexOpenSwipe = position
                }
            }

            override fun onSlide(view: SwipeRevealLayout?, slideOffset: Float) {
                v.itemChat.isClickable = false
            }

        })
    }

    private fun BaseViewHolder<ItemListChatBinding>.clearGroupTypingListener() {
        (itemView.getTag(R.id.tag_group_typing_listener) as? ListenerRegistration)?.remove()
        itemView.setTag(R.id.tag_group_typing_listener, null)
    }

    private fun BaseViewHolder<ItemListChatBinding>.bindOnlineIndicator(conversation: Conversation) {
        if (conversation.isGroupThread()) {
            v.onlineIndicator.isVisible = false
            return
        }
        v.onlineIndicator.isVisible = presenceMap[conversation.friendId]?.online == true
    }

    /**
     * This function is used to handle the change in the conversation
     */
    private fun BaseViewHolder<ItemListChatBinding>.handleWhenConversationIsChanged(conversation: Conversation) {
        if (conversation.isGroupThread()) {
            if (conversation.numberUnSeen > 0) {
                v.tvMessage.setTextColor(itemView.context.getColor(R.color.text_common))
                v.tvTime.setTextColor(itemView.context.getColor(R.color.text_common))
            } else {
                v.tvMessage.setTextColor(itemView.context.getColor(R.color.grey_1))
                v.tvTime.setTextColor(itemView.context.getColor(R.color.grey_1))
            }
            if (conversation.numberUnSeen > 0) {
                v.newMessage.isVisible = true
                showMultiMessage(conversation.numberUnSeen > 1)
                v.tvMultiMessage.text = conversation.numberUnSeen.toString()
            } else {
                hideNewMessage()
            }
            v.avtSeen.isVisible = false
            return
        }
        if (conversation.numberUnSeen > 0) {
            v.tvMessage.setTextColor(itemView.context.getColor(R.color.text_common))
            v.tvTime.setTextColor(itemView.context.getColor(R.color.text_common))
        } else {
            v.tvMessage.setTextColor(itemView.context.getColor(R.color.grey_1))
            v.tvTime.setTextColor(itemView.context.getColor(R.color.grey_1))
        }

        if (conversation.isMessageFromFriend()) {
            v.newMessage.isVisible = !conversation.isSeenMessage()
            if (!conversation.isSeenMessage()) {
                showMultiMessage(conversation.numberUnSeen > 1)
                v.tvMultiMessage.text = conversation.numberUnSeen.toString()
            } else {
                hideNewMessage()
            }
            this.v.avtSeen.isVisible = false
        } else {
            hideNewMessage()
            v.avtSeen.isVisible = conversation.isSeenMessage()
        }
    }

    /**
     * This function is used to show or hide text multiple unread messages
     */
    private fun BaseViewHolder<ItemListChatBinding>.showMultiMessage(isShow: Boolean) {
        this.v.tvMultiMessage.isVisible = isShow
        this.v.singMessage.isVisible = !isShow
    }

    /**
     * This function is used to hide view new message
     */
    private fun BaseViewHolder<ItemListChatBinding>.hideNewMessage() {
        this.v.tvMultiMessage.isVisible = false
        this.v.singMessage.isVisible = false
    }

    /**
     * Only if you need to restore open/close state when the orientation is changed.
     * Call this method in [android.app.Activity.onSaveInstanceState]
     */
    fun saveStates(outState: Bundle?) {
        binderHelper.saveStates(outState)
    }

    /**
     * Only if you need to restore open/close state when the orientation is changed.
     * Call this method in [android.app.Activity.onRestoreInstanceState]
     */
    fun restoreStates(inState: Bundle?) {
        binderHelper.restoreStates(inState)
    }
}