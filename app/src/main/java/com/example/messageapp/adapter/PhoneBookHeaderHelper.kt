package com.example.messageapp.adapter

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.messageapp.R
import com.example.messageapp.databinding.HeaderPhoneBookBinding

object PhoneBookHeaderHelper {

    fun bind(
        binding: HeaderPhoneBookBinding,
        totalFriendCount: Int,
        onlineCount: Int,
        filter: PhoneBookFilter,
        friendRequestCount: Int,
        onFilterChanged: (PhoneBookFilter) -> Unit,
        onClickFriendRequest: () -> Unit,
    ) {
        val context = binding.root.context
        binding.tvAllPhoneBook.text = context.getString(
            R.string.phone_book_filter_all,
            totalFriendCount,
        )
        binding.tvRecentPhoneBook.text = context.getString(
            R.string.phone_book_filter_recent_online,
            onlineCount,
        )
        styleFilterTab(binding.tvAllPhoneBook, filter == PhoneBookFilter.ALL)
        styleFilterTab(binding.tvRecentPhoneBook, filter == PhoneBookFilter.ONLINE)
        binding.tvAllPhoneBook.setOnClickListener {
            if (filter != PhoneBookFilter.ALL) {
                onFilterChanged(PhoneBookFilter.ALL)
            }
        }
        binding.tvRecentPhoneBook.setOnClickListener {
            if (filter != PhoneBookFilter.ONLINE) {
                onFilterChanged(PhoneBookFilter.ONLINE)
            }
        }
        if (friendRequestCount > 0) {
            binding.tvFriendRequestCount.visibility = View.VISIBLE
            binding.tvFriendRequestCount.text = "($friendRequestCount)"
        } else {
            binding.tvFriendRequestCount.visibility = View.GONE
        }
        binding.friendRequest.setOnClickListener { onClickFriendRequest() }
    }

    private fun styleFilterTab(textView: TextView, selected: Boolean) {
        val context = textView.context
        if (selected) {
            textView.setBackgroundResource(R.drawable.bg_corner_25_blue_light)
            textView.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.text_hint),
            )
            textView.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        } else {
            textView.setBackgroundResource(R.drawable.bg_corner_25_stroke_grey)
            textView.backgroundTintList = null
            textView.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }
    }
}
