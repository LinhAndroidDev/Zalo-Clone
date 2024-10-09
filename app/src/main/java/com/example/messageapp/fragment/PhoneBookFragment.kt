package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.messageapp.R
import com.example.messageapp.adapter.PhoneBookAdapter
import com.example.messageapp.adapter.TypePhoneBook
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.custom.StickyHeaderItemDecorator
import com.example.messageapp.databinding.FragmentPhoneBookBinding
import com.example.messageapp.helper.avatars
import com.example.messageapp.helper.capitalLetters
import com.example.messageapp.model.GroupPhoneBook
import com.example.messageapp.model.PhoneBook
import com.example.messageapp.viewmodel.PhoneBookFragmentViewModel

class PhoneBookFragment : BaseFragment<FragmentPhoneBookBinding, PhoneBookFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_phone_book

    private var indexTouch = 0

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
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
        val stickyHeaderDecoration = StickyHeaderItemDecorator(phoneBookAdapter)
        stickyHeaderDecoration.attachToRecyclerView(binding?.rcvPhoneBook)
        binding?.rcvPhoneBook?.addItemDecoration(stickyHeaderDecoration)

        val headerPhoneBooks = groupPhoneBooks.map { it.nameGroup }.filter { it.isNotEmpty() }
        headerPhoneBooks.forEach { header ->
            val view: View = LayoutInflater.from(context).inflate(R.layout.item_header_group_phone_book, null)
            val tvHeaderGroupPhoneBook = view.findViewById<TextView>(R.id.tvHeaderGroup)
            if(indexTouch == headerPhoneBooks.indexOf(header)) {
                tvHeaderGroupPhoneBook.setTextColor(ContextCompat.getColor(requireActivity(), R.color.text_common))
            } else {
                tvHeaderGroupPhoneBook.setTextColor(ContextCompat.getColor(requireActivity(), R.color.grey_1))
            }
            tvHeaderGroupPhoneBook.text = header
            binding?.listHeaderGroup?.addView(view)
        }

        binding?.rcvPhoneBook?.setOnTouchListener { view, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_MOVE || motionEvent.action == MotionEvent.ACTION_DOWN) {
                for(i in headerPhoneBooks.indices) {
                    val xItem = binding?.listHeaderGroup?.getChildAt(i)?.x ?: 0f
                    val yItem = binding?.listHeaderGroup?.getChildAt(i)?.y ?: 0f
                    val widthItem = binding?.listHeaderGroup?.getChildAt(i)?.width?.toFloat() ?: 0f
                    val heightItem = binding?.listHeaderGroup?.getChildAt(i)?.height?.toFloat() ?: 0f
                    if(motionEvent.x >= xItem && motionEvent.x <= (xItem + widthItem) && motionEvent.y >= yItem && motionEvent.y <= yItem + heightItem) {
                        indexTouch = i
                        binding?.listHeaderGroup?.invalidate()
                        break
                    }
                }
            }
            true
        }
    }

    override fun onClickView() {
        super.onClickView()
    }
}