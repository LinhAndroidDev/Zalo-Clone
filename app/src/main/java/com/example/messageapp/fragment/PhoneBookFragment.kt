package com.example.messageapp.fragment

import com.example.messageapp.R
import com.example.messageapp.adapter.PhoneBookAdapter
import com.example.messageapp.adapter.TypePhoneBook
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentPhoneBookBinding
import com.example.messageapp.helper.avatars
import com.example.messageapp.helper.capitalLetters
import com.example.messageapp.model.GroupPhoneBook
import com.example.messageapp.model.PhoneBook
import com.example.messageapp.viewmodel.PhoneBookFragmentViewModel

class PhoneBookFragment : BaseFragment<FragmentPhoneBookBinding, PhoneBookFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_phone_book

    override fun initView() {
        super.initView()
        val names = listOf("An", "Bảo", "Brian", "Alex", "Aiden", "Finn", "Khánh", "Duy", "Emma", "Samuel", "Oscar", "Hannah", "Cindy", "Chris", "Phát", "Isaac", "George", "Lucy", "Việt", "Yen", "Jack", "Jenny", "Michael", "Ryan", "Harry", "Sophia", "Paula", "Thomas", "Liam", "Minh", "Oanh", "James", "David", "Linh", "Phương", "Quinn", "Rạng", "Quang", "Nina", "Rachel", "Walter", "Wendy", "Xander", "Ximena", "Tina", "Vera", "Victor", "Zach", "Zoe", "Zara", "Ivy", "Diana", "Peter", "Uyên", "Yuri", "Fiona", "Isabella", "Noah", "Grace", "Kevin", "Kathy")
        val groupPhoneBooks = arrayListOf<GroupPhoneBook>()
        capitalLetters.forEach { letter ->
            val namePhoneBooks = names.filter { name -> letter == name[0].toString() }
            val phoneBooks = arrayListOf<PhoneBook>()
            phoneBooks.add(PhoneBook(letter, "", TypePhoneBook.HEADER_GROUP_PHONE_BOOK))
            namePhoneBooks.forEach { namePhoneBook ->
                phoneBooks.add(
                    PhoneBook(
                        namePhoneBook,
                        avatar = avatars[names.indexOf(namePhoneBook)],
                        TypePhoneBook.ITEM_PHONE_BOOK
                    )
                )
            }
            groupPhoneBooks.add(GroupPhoneBook(letter, phoneBooks))
        }

        val phoneBookDatas = arrayListOf<PhoneBook>()
        phoneBookDatas.add(PhoneBook("", "", TypePhoneBook.HEADER_PHONE_BOOK))
        groupPhoneBooks.forEach { group ->
            phoneBookDatas.addAll(group.phoneBooks)
        }

        val phoneBookAdapter = PhoneBookAdapter()
        phoneBookAdapter.phoneBooks = phoneBookDatas
        binding?.rcvPhoneBook?.adapter = phoneBookAdapter
    }

    override fun onClickView() {
        super.onClickView()
    }
}