package com.example.messageapp.fragment

/**
 * Created by Nguyen Huu Linh in 2024/10/01
 */

import android.annotation.SuppressLint
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.MainActivity
import com.example.messageapp.R
import com.example.messageapp.adapter.PhoneBookAdapter
import com.example.messageapp.adapter.TypePhoneBook
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.library.sticky_header.StickyHeaderItemDecorator
import com.example.messageapp.databinding.FragmentPhoneBookBinding
import com.example.messageapp.helper.avatars
import com.example.messageapp.helper.capitalLetters
import com.example.messageapp.model.GroupPhoneBook
import com.example.messageapp.model.PhoneBook
import com.example.messageapp.viewmodel.PhoneBookFragmentViewModel

/**
 * This list use Sticky Header combined with Fast Scroll Alphabet
 * Below is a description how to get data to list:
 * + Filter list name user by letter if have data, add header group then import data into group phone book
 * + Then get data PhoneBook by GroupPhoneBook and import into recyclerview, need to add header group before
 */
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
        var indexGroupHeader = 1
        capitalLetters.forEach { letter ->
            val namePhoneBooks = names.filter { name -> name.startsWith(letter, true) }
            if(namePhoneBooks.isNotEmpty()) {
                val phoneBooks = arrayListOf<PhoneBook>()
                phoneBooks.add(PhoneBook(letter, "", TypePhoneBook.HEADER_GROUP_PHONE_BOOK, indexGroupHeader))
                namePhoneBooks.forEach { namePhoneBook ->
                    phoneBooks.add(
                        PhoneBook(
                            namePhoneBook,
                            avatar = avatars[names.indexOf(namePhoneBook)],
                            TypePhoneBook.ITEM_PHONE_BOOK,
                            indexGroupHeader
                        )
                    )
                }
                indexGroupHeader += phoneBooks.size
                groupPhoneBooks.add(GroupPhoneBook(letter, phoneBooks))
            }
        }

        val phoneBookDatas = arrayListOf<PhoneBook>()
        phoneBookDatas.add(PhoneBook("", "", TypePhoneBook.HEADER_PHONE_BOOK, -1))
        groupPhoneBooks.forEach { group ->
            phoneBookDatas.addAll(group.phoneBooks)
        }

        val phoneBookAdapter = PhoneBookAdapter()
        phoneBookAdapter.onClickPhoneBook = {

        }
        phoneBookAdapter.onClickFriendRequest = {
            findNavController().navigate(R.id.action_phoneBookFragment_to_friendRequestFragment)
        }
        phoneBookAdapter.phoneBooks = phoneBookDatas
        binding?.rcvPhoneBook?.adapter = phoneBookAdapter
        val stickyHeaderDecoration =
            StickyHeaderItemDecorator(
                phoneBookAdapter
            )
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

        binding?.rcvPhoneBook?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                /**
                 * Find the first position the recyclerview scrolls to,
                 * get the first position of headerGroups
                 * if its headerPosition is equal to the first position the recyclerview scrolls to
                 */
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val headerGroups = phoneBookDatas.filter { it.type == TypePhoneBook.HEADER_GROUP_PHONE_BOOK }
                var positionOfHeaderGroup =
                    headerGroups.indexOfFirst { phoneBook -> phoneBook.headerPosition == phoneBookDatas[firstVisibleItemPosition].headerPosition }

                positionOfHeaderGroup = if(positionOfHeaderGroup == -1) 0 else positionOfHeaderGroup
                binding?.rcvPhoneBook?.setUpPositionScrollValue(positionOfHeaderGroup)
            }
        })
    }

    override fun onClickView() {
        super.onClickView()
    }
}