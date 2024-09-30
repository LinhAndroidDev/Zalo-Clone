package com.example.messageapp.adapter

import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemSuggestFriendBinding

class SuggestFriendAdapter : BaseAdapter<Int, ItemSuggestFriendBinding>() {
    override fun getLayout(): Int = R.layout.item_suggest_friend

    override fun getItemCount(): Int = 3

    override fun onBindViewHolder(holder: BaseViewHolder<ItemSuggestFriendBinding>, position: Int) {

    }
}