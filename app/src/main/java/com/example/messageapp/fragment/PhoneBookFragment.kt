package com.example.messageapp.fragment

import android.annotation.SuppressLint
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar.LayoutParams
import com.example.messageapp.MainActivity
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

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun initView() {
        super.initView()

        binding?.root?.post {
            binding?.viewBottom?.layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                (activity as MainActivity).getHeightBottomNav()
            )
        }
        val names = listOf("An", "Bảo", "Brian", "Alex", "Aiden", "Finn", "Khánh", "Duy", "Emma", "Samuel", "Oscar", "Hannah", "Cindy", "Chris", "Phát", "Isaac", "George", "Lucy", "Việt", "Yen", "Jack", "Jenny", "Michael", "Ryan", "Harry", "Sophia", "Paula", "Thomas", "Liam", "Minh", "Oanh", "James", "David", "Linh", "Phương", "Quinn", "Rạng", "Quang", "Nina", "Rachel", "Walter", "Wendy", "Xander", "Ximena", "Tina", "Vera", "Victor", "Zach", "Zoe", "Zara", "Ivy", "Diana", "Peter", "Uyên", "Yuri", "Fiona", "Isabella", "Noah", "Grace", "Kevin", "Kathy")
        val groupPhoneBooks = arrayListOf<GroupPhoneBook>()
        capitalLetters.forEach { letter ->
            val namePhoneBooks = names.filter { name -> letter == name[0].toString() }
            if(namePhoneBooks.isNotEmpty()) {
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
        binding?.rcvPhoneBook?.apply {
            setIndexTextSize(12)
            setIndexBarCornerRadius(8)
            setIndexBarTransparentValue(1f)
            setIndexBarBackGroundColor(R.color.white)
            setIndexBarTopMargin(15f)
            setIndexBarBottomMargin(15f)
            setPreviewPadding(0)
            setIndexBarTextColor(R.color.grey_1)
            setPreviewTextSize(22)
            setPreviewColor(R.color.blue1)
            setPreviewTextColor(R.color.text_white)
            setPreviewTransparentValue(1f)
            setPreviewCornerRadiusValue(8)
            setIndexBarVisibility(true)
            setIndexBarStrokeVisibility(false)
            setIndexBarStrokeWidth(0)
            setIndexBarStrokeColor(R.color.grey_1)
            setIndexBarHighLightTextColor(R.color.text_common)
            setIndexBarHighLightTextVisibility(true)
        }
    }

    override fun onClickView() {
        super.onClickView()
    }
}