package com.example.messageapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.databinding.ItemChatReceiverBinding
import com.example.messageapp.databinding.ItemChatSenderBinding

data class MessageData(
    val message: String,
    val isSender: Boolean,
    val avatar: String
)

const val VIEW_SENDER = 0
const val VIEW_RECEIVER = 1

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages = listOf(
        MessageData("Chào bạn mình tên Toàn", false, "https://live.staticflickr.com/65535/51348774357_379be59623.jpg"),
        MessageData("Chào bạn, rất vui được gặp bạn", true, "https://media.baobinhphuoc.com.vn/upload/news/7_2023/68992f500e9e44c4a2f9511e9ae8cdd3.jpg"),
        MessageData("Mình tên Linh", true, "https://media.baobinhphuoc.com.vn/upload/news/7_2023/68992f500e9e44c4a2f9511e9ae8cdd3.jpg"),
        MessageData("Bạn ở đâu thế?", false, "https://live.staticflickr.com/65535/51348774357_379be59623.jpg"),
    )

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
            }

            else -> {
                holder as ReceiverViewHolder
                holder.v.tvReceiver.text = message.message
                Glide.with(holder.v.root)
                    .load(message.avatar)
                    .into(holder.v.avatarReceiver)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSender) VIEW_SENDER else VIEW_RECEIVER
    }

    class SenderViewHolder(val v: ItemChatSenderBinding) : RecyclerView.ViewHolder(v.root)

    class ReceiverViewHolder(val v: ItemChatReceiverBinding) : RecyclerView.ViewHolder(v.root)
}