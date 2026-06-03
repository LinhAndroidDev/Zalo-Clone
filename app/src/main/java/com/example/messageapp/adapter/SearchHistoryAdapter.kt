package com.example.messageapp.adapter

import com.example.messageapp.R
import com.example.messageapp.base.BaseAdapter
import com.example.messageapp.databinding.ItemSearchHistoryBinding
import com.example.messageapp.model.User
import com.example.messageapp.utils.FileUtils.loadImg

class SearchHistoryAdapter : BaseAdapter<User, ItemSearchHistoryBinding>() {

    var onItemClick: ((User) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_search_history

    override fun onBindViewHolder(holder: BaseViewHolder<ItemSearchHistoryBinding>, position: Int) {
        val user = items[position]
        holder.v.root.context.loadImg(
            user.avatar.orEmpty(),
            holder.v.avtHistory,
            R.drawable.bg_grey_equal
        )
        holder.v.tvNameHistory.text = user.name
        holder.v.root.setOnClickListener { onItemClick?.invoke(user) }
    }

    fun updateDiff(newList: List<User>) {
        updateDiffList(
            newList,
            compareItem = { old, new -> old.keyAuth == new.keyAuth },
            compareContent = { old, new -> old == new }
        )
    }
}
