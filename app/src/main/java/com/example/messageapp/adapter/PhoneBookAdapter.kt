package com.example.messageapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SectionIndexer
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.custom.sticky_header.StickyAdapter
import com.example.messageapp.databinding.HeaderGroupPhoneBookBinding
import com.example.messageapp.databinding.HeaderPhoneBookBinding
import com.example.messageapp.databinding.ItemPhoneBookBinding
import com.example.messageapp.helper.Helpers.Companion.sectionsHelper
import com.example.messageapp.model.PhoneBook
import java.lang.IllegalArgumentException
import java.util.Locale

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

class PhoneBookAdapter : StickyAdapter<PhoneBookAdapter.HeaderGroupViewHolder, RecyclerView.ViewHolder>(), SectionIndexer {
    var phoneBooks = arrayListOf<PhoneBook>()
    private var sectionsTranslator = HashMap<Int, Int>()
    var onClickPhoneBook: (() -> Unit)? = null
    var onClickFriendRequest: (() -> Unit)? = null

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

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        return phoneBooks[itemPosition].headerPosition
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderGroupViewHolder {
        return HeaderGroupViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.header_group_phone_book,
                parent,
                false
            )
        )
    }

    override fun onBindHeaderViewHolder(holder: HeaderGroupViewHolder, headerPosition: Int) {
        val nameFriend = phoneBooks[headerPosition].nameFriend
        if(nameFriend.isNotEmpty()) {
            holder.v.nameGroup.text = phoneBooks[headerPosition].nameFriend[0].toString()
        }
        holder.v.nameGroup.setBackgroundColor(ContextCompat.getColor(holder.v.root.context, R.color.white))
    }

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
                holder.v.friendRequest.setOnClickListener {
                    onClickFriendRequest?.invoke()
                }
            }

            TypePhoneBook.HEADER_GROUP_PHONE_BOOK -> {
                holder as HeaderGroupViewHolder
                holder.v.nameGroup.text = phoneBook.nameFriend
            }

            else -> {
                holder as ItemViewHolder
                holder.itemView.setOnClickListener {
                    onClickPhoneBook?.invoke()
                }
                holder.v.nameFriend.text = phoneBook.nameFriend
                Glide.with(holder.v.root)
                    .load(phoneBook.avatar)
                    .into(holder.v.avatarFriend)
            }
        }
    }

    override fun getSections(): Array<String> {
        val alphabetFull = ArrayList<String>()
        val sections = ArrayList<String>()
        val headerGroups =
            phoneBooks.filter { pBook -> pBook.type == TypePhoneBook.HEADER_GROUP_PHONE_BOOK }
                .map { it.nameFriend }
        run {
            var i = 0
            while (i < headerGroups.size) {
                val section = headerGroups[i][0].toString().uppercase(Locale.getDefault())
                if (!sections.contains(section)) {
                    sections.add(section)
                }
                i++
            }
        }
        headerGroups.forEach { element -> alphabetFull.add(element) }
        sectionsTranslator = sectionsHelper(sections, alphabetFull)
        return alphabetFull.toTypedArray()
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        val headerGroups =
            phoneBooks.filter { pBook -> pBook.type == TypePhoneBook.HEADER_GROUP_PHONE_BOOK }
        return phoneBooks.indexOf(headerGroups[sectionIndex])
    }

    override fun getSectionForPosition(position: Int): Int = 0
}