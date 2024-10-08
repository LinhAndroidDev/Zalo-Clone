package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.databinding.HeaderGroupPhoneBookBinding
import com.example.messageapp.databinding.HeaderPhoneBookBinding
import com.example.messageapp.databinding.ItemPhoneBookBinding
import com.example.messageapp.model.PhoneBook
import java.lang.IllegalArgumentException

enum class TypePhoneBook {
    HEADER_PHONE_BOOK,
    HEADER_GROUP_PHONE_BOOK,
    ITEM_PHONE_BOOK;

    companion object {
        fun of(value: Int): TypePhoneBook {
            return entries.firstOrNull { it.ordinal == value }
                ?: throw IllegalArgumentException("Unknown Data")
        }
    }
}

class PhoneBookAdapter : Adapter<RecyclerView.ViewHolder>() {
    var phoneBooks = arrayListOf<PhoneBook>()

    inner class HeaderViewHolder(val v: HeaderPhoneBookBinding) : RecyclerView.ViewHolder(v.root)

    inner class HeaderGroupViewHolder(val v: HeaderGroupPhoneBookBinding) :
        RecyclerView.ViewHolder(v.root)

    inner class ItemViewHolder(val v: ItemPhoneBookBinding) : RecyclerView.ViewHolder(v.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (TypePhoneBook.of(viewType)) {
            TypePhoneBook.HEADER_PHONE_BOOK -> {
                HeaderViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.header_phone_book,
                        parent,
                        false
                    )
                )
            }

            TypePhoneBook.HEADER_GROUP_PHONE_BOOK -> {
                HeaderGroupViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.header_group_phone_book,
                        parent,
                        false
                    )
                )
            }

            else -> {
                ItemViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_phone_book,
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int = phoneBooks.size

    override fun getItemViewType(position: Int): Int {
        return when (phoneBooks[position].type) {
            TypePhoneBook.HEADER_PHONE_BOOK -> {
                TypePhoneBook.HEADER_PHONE_BOOK.ordinal
            }

            TypePhoneBook.HEADER_GROUP_PHONE_BOOK -> {
                TypePhoneBook.HEADER_GROUP_PHONE_BOOK.ordinal
            }

            TypePhoneBook.ITEM_PHONE_BOOK -> {
                TypePhoneBook.ITEM_PHONE_BOOK.ordinal
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val phoneBook = phoneBooks[position]
        val itemPhoneBooks =
            phoneBooks.filter { pBook -> pBook.type == TypePhoneBook.ITEM_PHONE_BOOK }
        when (TypePhoneBook.of(holder.itemViewType)) {
            TypePhoneBook.HEADER_PHONE_BOOK -> {
                holder as HeaderViewHolder
                holder.v.tvAllPhoneBook.text = "Tất cả  ${itemPhoneBooks.size}"
            }

            TypePhoneBook.HEADER_GROUP_PHONE_BOOK -> {
                holder as HeaderGroupViewHolder
                holder.v.nameGroup.text = phoneBook.nameFriend
            }

            else -> {
                holder as ItemViewHolder
                holder.v.nameFriend.text = phoneBook.nameFriend
                Glide.with(holder.v.root)
                    .load(phoneBook.avatar)
                    .into(holder.v.avatarFriend)
            }
        }
    }
}