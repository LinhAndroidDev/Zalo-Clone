package com.example.messageapp.model

import com.example.messageapp.adapter.TypePhoneBook

data class GroupPhoneBook(
    val nameGroup: String,
    val phoneBooks: List<PhoneBook>
)

data class PhoneBook(
    val nameFriend: String,
    val avatar: String,
    var type: TypePhoneBook = TypePhoneBook.ITEM_PHONE_BOOK
)