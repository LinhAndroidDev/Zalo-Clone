package com.example.messageapp.adapter

import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemListChatBinding

class ListChatAdapter : BaseAdapter<Int, ItemListChatBinding>() {
    var onClickView: (() -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_list_chat

    override fun getItemCount(): Int = 14

    override fun onBindViewHolder(holder: BaseViewHolder<ItemListChatBinding>, position: Int) {
        holder.itemView.setOnClickListener {
            onClickView?.invoke()
        }
    }
}