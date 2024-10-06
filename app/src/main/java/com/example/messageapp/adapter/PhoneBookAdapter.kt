package com.example.messageapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.example.messageapp.R
import com.example.messageapp.databinding.HeaderPhoneBookBinding
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

    inner class HeaderGroupViewHolder(val v: HeaderPhoneBookBinding) : RecyclerView.ViewHolder(v.root)

    inner class ItemViewHolder(val v: HeaderPhoneBookBinding) : RecyclerView.ViewHolder(v.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(TypePhoneBook.of(viewType)) {
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
        return when(phoneBooks[position].type) {
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(TypePhoneBook.of(holder.itemViewType)) {
            TypePhoneBook.HEADER_PHONE_BOOK -> {
                holder as HeaderViewHolder
            }

            TypePhoneBook.HEADER_GROUP_PHONE_BOOK -> {
                holder as HeaderGroupViewHolder
            }

            else -> {
                holder as ItemViewHolder
            }
        }
    }
}