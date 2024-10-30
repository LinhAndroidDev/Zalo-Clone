package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemChatReceiverBinding
import com.example.messageapp.databinding.ItemChatSenderBinding
import com.example.messageapp.model.Message
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FireBaseInstance

const val VIEW_SENDER = 0
const val VIEW_RECEIVER = 1

class ChatAdapter(
    private val friendId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var messages = arrayListOf<Message>()
    var longClickItemSender: ((Pair<View, Message>) -> Unit)? = null
    var longClickItemReceiver: ((Pair<View, Message>) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setMessage(list: ArrayList<Message>) {
        val startIndex = messages.size
        messages.clear()
        messages.addAll(list)
        notifyDataSetChanged()
//        notifyItemRangeChanged(startIndex, list.size)
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
            }

            else -> {
                holder as ReceiverViewHolder
                holder.v.tvReceiver.text = message.message
                holder.v.tvTime.text = DateUtils.convertTimeToHour(message.time)
                holder.v.viewBottom.isVisible = position == messages.size - 1
                FireBaseInstance.getInfoUser(friendId) { user ->
                    Glide.with(holder.v.root)
                        .load(user.avatar)
                        .error(R.mipmap.ic_launcher)
                        .into(holder.v.avatarReceiver)
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