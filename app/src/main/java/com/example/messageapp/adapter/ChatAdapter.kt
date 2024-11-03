package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemChatReceiverBinding
import com.example.messageapp.databinding.ItemChatSenderBinding
import com.example.messageapp.model.Message
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.loadImg

const val VIEW_SENDER = 0
const val VIEW_RECEIVER = 1

class ChatAdapter(
    private val friendId: String,
    private val userId: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var messages = arrayListOf<Message>()
    var longClickItemSender: ((Pair<View, Message>) -> Unit)? = null
    var longClickItemReceiver: ((Pair<View, Message>) -> Unit)? = null
    var seenMessage: (() -> Unit)? = null
    private var seen: Boolean = false

    @SuppressLint("NotifyDataSetChanged")
    fun setMessage(list: ArrayList<Message>) {
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
                holder.v.tvSender.text = message.message
                holder.v.tvTime.text = DateUtils.convertTimeToHour(message.time)
                holder.v.viewBottom.isVisible = position == messages.size - 1
                holder.v.layoutMessage.setOnLongClickListener {
                    longClickItemSender?.invoke(it to message)
                    true
                }
                FireBaseInstance.getConversationRlt(
                    friendId = friendId,
                    userId = userId,
                    success = { cvt ->
                        if(cvt.seen == "1" && position == messages.size - 1) {
                            seen = true
                            FireBaseInstance.getInfoUser(friendId) { user ->
                                holder.itemView.context.loadImg(
                                    user.avatar.toString(),
                                    holder.v.avtSeen
                                )
                                seenMessage?.invoke()
                            }
                        } else {
                            seen = false
                        }
                        notifyDataSetChanged()
                    }
                )
                holder.v.avtSeen.isVisible = seen
            }

            else -> {
                holder as ReceiverViewHolder
                holder.v.tvReceiver.text = message.message
                holder.v.tvTime.text = DateUtils.convertTimeToHour(message.time)
                holder.v.viewBottom.isVisible = position == messages.size - 1
                FireBaseInstance.getInfoUser(friendId) { user ->
                    holder.itemView.context.loadImg(
                        user.avatar.toString(),
                        holder.v.avatarReceiver
                    )
                }
                holder.v.layoutMessage.setOnLongClickListener {
                    longClickItemReceiver?.invoke(it to message)
                    true
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender != friendId) VIEW_SENDER else VIEW_RECEIVER
    }

    class SenderViewHolder(val v: ItemChatSenderBinding) : RecyclerView.ViewHolder(v.root)

    class ReceiverViewHolder(val v: ItemChatReceiverBinding) : RecyclerView.ViewHolder(v.root)
}