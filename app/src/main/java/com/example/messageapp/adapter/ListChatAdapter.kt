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
import com.example.messageapp.utils.GroupAvatarLoader

class ListChatAdapter(
    private val groupAvatarLoader: GroupAvatarLoader,
) :
    BaseAdapter<Conversation, ItemListChatBinding>() {

    private val binderHelper = ViewBinderHelper()
    var onClickView: ((Conversation) -> Unit)? = null
    var showOptionConversation: (() -> Unit)? = null
    var indexOpenSwipe: Int? = null
    private var presenceMap: Map<String, UserPresence> = emptyMap()
    private var typingMap: Map<String, Boolean> = emptyMap()
    private var avatarMap: Map<String, String> = emptyMap()

    override fun getLayout(): Int = R.layout.item_list_chat

    override fun onBindViewHolder(holder: BaseViewHolder<ItemListChatBinding>, @SuppressLint("RecyclerView") position: Int) {
        if (holder.v.swipeLayout.isOpened) binderHelper.closeLayout(indexOpenSwipe.toString())
        binderHelper.bind(holder.v.swipeLayout, position.toString())
        binderHelper.setOpenOnlyOne(true)
        holder.initView(position)
    }

    override fun onViewRecycled(holder: BaseViewHolder<ItemListChatBinding>) {
        groupAvatarLoader.cancel(holder.v.groupAvatar.tag as? String)
        holder.v.groupAvatar.reset()
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

    @SuppressLint("NotifyDataSetChanged")
    fun updateTypingMap(map: Map<String, Boolean>) {
        if (typingMap == map) return
        typingMap = map
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateAvatarMap(map: Map<String, String>) {
        if (avatarMap == map) return
        avatarMap = map
        notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun BaseViewHolder<ItemListChatBinding>.initView(position: Int) {
        val conversation = items[position]
        v.tvNameFriend.text = conversation.name
        val isTyping = typingMap[conversation.friendId] == true
        v.typingView.isVisible = isTyping
        v.tvMessage.isVisible = !isTyping
        v.tvMessage.text = "${conversation.person}: ${conversation.message}"
        v.tvTime.text = DateUtils.formatTime(conversation.time)
        bindOnlineIndicator(conversation)
        this.handleWhenConversationIsChanged(conversation)
        bindAvatar(conversation)
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

    private fun BaseViewHolder<ItemListChatBinding>.bindAvatar(conversation: Conversation) {
        val friendId = conversation.friendId

        if (conversation.isGroupThread()) {
            v.avatarFriend.isVisible = false
            v.groupAvatar.isVisible = true
            v.groupAvatar.tag = friendId

            if (conversation.friendImage.isNotBlank()) {
                v.groupAvatar.showSinglePhoto(conversation.friendImage)
            } else {
                v.groupAvatar.showPlaceholder()
                groupAvatarLoader.load(
                    groupId = friendId,
                    onReady = { data ->
                        if (v.groupAvatar.tag != friendId) return@load
                        v.groupAvatar.bindMemberAvatars(data.avatarUrls, data.totalCount)
                    },
                    onError = {
                        if (v.groupAvatar.tag != friendId) return@load
                        v.groupAvatar.showPlaceholder()
                    },
                )
            }
            return
        }

        v.groupAvatar.isVisible = false
        v.groupAvatar.reset()
        v.avatarFriend.isVisible = true
        v.avatarFriend.tag = friendId

        val avatarUrl = avatarMap[friendId].orEmpty().ifBlank { conversation.friendImage }
        if (avatarUrl.isNotBlank()) {
            itemView.context.loadImg(avatarUrl, v.avatarFriend)
            itemView.context.loadImg(avatarUrl, v.avtSeen)
        }
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
                v.tvMessage.setTextColor(itemView.context.getColor(R.color.text_primary))
                v.tvTime.setTextColor(itemView.context.getColor(R.color.text_primary))
            } else {
                v.tvMessage.setTextColor(itemView.context.getColor(R.color.text_muted))
                v.tvTime.setTextColor(itemView.context.getColor(R.color.text_muted))
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
            v.tvMessage.setTextColor(itemView.context.getColor(R.color.text_primary))
            v.tvTime.setTextColor(itemView.context.getColor(R.color.text_primary))
        } else {
            v.tvMessage.setTextColor(itemView.context.getColor(R.color.text_muted))
            v.tvTime.setTextColor(itemView.context.getColor(R.color.text_muted))
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
