package com.example.messageapp.fragment

/**
 * Created by Nguyen Huu Linh in 2024/10/01
 */

import android.annotation.SuppressLint
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.lifecycle.lifecycleScope
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
import com.example.messageapp.helper.capitalLetters
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Friend
import com.example.messageapp.model.GroupPhoneBook
import com.example.messageapp.model.PhoneBook
import com.example.messageapp.model.User
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.viewmodel.PhoneBookFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * This list use Sticky Header combined with Fast Scroll Alphabet
 * Below is a description how to get data to list:
 * + Filter list name user by letter if have data, add header group then import data into group phone book
 * + Then get data PhoneBook by GroupPhoneBook and import into recyclerview, need to add header group before
 */
@AndroidEntryPoint
class PhoneBookFragment :
    BaseFragment<FragmentPhoneBookBinding, PhoneBookFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_phone_book

    private val phoneBookAdapter by lazy { PhoneBookAdapter() }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun initView() {
        super.initView()
        FirebaseAnalyticsInstance.logPhoneBookScreen()

        binding?.root?.post {
            binding?.viewBottom?.layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                (activity as MainActivity).getHeightBottomNav()
            )
        }

        phoneBookAdapter.onClickPhoneBook = { phoneBook ->
            val user = User(
                name = phoneBook.nameFriend,
                avatar = phoneBook.avatar,
                keyAuth = phoneBook.keyAuth
            )
            val action = PhoneBookFragmentDirections.actionPhoneBookFragmentToChatFragment(
                Conversation(user)
            )
            findNavController().navigate(action)
        }
        phoneBookAdapter.onClickFriendRequest = {
            findNavController().navigate(R.id.action_phoneBookFragment_to_friendRequestFragment)
        }

        binding?.rcvPhoneBook?.adapter = phoneBookAdapter
        setupStickyHeader()
        setupAlphabetScroll()
        viewModel?.getFriends()
        viewModel?.getPendingRequestCounts()
    }

    override fun bindData() {
        super.bindData()
        lifecycleScope.launch {
            viewModel?.friends?.collect { friends ->
                buildPhoneBookList(friends)
            }
        }
        lifecycleScope.launch {
            viewModel?.totalRequestCount?.collect { count ->
                phoneBookAdapter.friendRequestCount = count
                phoneBookAdapter.notifyItemChanged(0)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun buildPhoneBookList(friends: List<Friend>) {
        val groupPhoneBooks = arrayListOf<GroupPhoneBook>()
        var indexGroupHeader = 1

        capitalLetters.forEach { letter ->
            val matched = friends.filter { it.name.startsWith(letter, ignoreCase = true) }
            if (matched.isNotEmpty()) {
                val phoneBooks = arrayListOf<PhoneBook>()
                phoneBooks.add(
                    PhoneBook(letter, "", TypePhoneBook.HEADER_GROUP_PHONE_BOOK, indexGroupHeader)
                )
                matched.forEach { friend ->
                    phoneBooks.add(
                        PhoneBook(
                            nameFriend = friend.name,
                            avatar = friend.avatar,
                            type = TypePhoneBook.ITEM_PHONE_BOOK,
                            headerPosition = indexGroupHeader,
                            keyAuth = friend.keyAuth
                        )
                    )
                }
                indexGroupHeader += phoneBooks.size
                groupPhoneBooks.add(GroupPhoneBook(letter, phoneBooks))
            }
        }

        val phoneBookDatas = arrayListOf<PhoneBook>()
        phoneBookDatas.add(PhoneBook("", "", TypePhoneBook.HEADER_PHONE_BOOK, -1))
        groupPhoneBooks.forEach { group -> phoneBookDatas.addAll(group.phoneBooks) }

        phoneBookAdapter.phoneBooks = phoneBookDatas
        phoneBookAdapter.notifyDataSetChanged()
    }

    private fun setupStickyHeader() {
        val stickyHeaderDecoration = StickyHeaderItemDecorator(phoneBookAdapter)
        stickyHeaderDecoration.attachToRecyclerView(binding?.rcvPhoneBook)
        binding?.rcvPhoneBook?.addItemDecoration(stickyHeaderDecoration)
    }

    private fun setupAlphabetScroll() {
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
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val phoneBookDatas = phoneBookAdapter.phoneBooks
                if (phoneBookDatas.isEmpty() || firstVisibleItemPosition < 0) return
                val headerGroups =
                    phoneBookDatas.filter { it.type == TypePhoneBook.HEADER_GROUP_PHONE_BOOK }
                var positionOfHeaderGroup =
                    headerGroups.indexOfFirst { pb ->
                        pb.headerPosition == phoneBookDatas[firstVisibleItemPosition].headerPosition
                    }
                positionOfHeaderGroup =
                    if (positionOfHeaderGroup == -1) 0 else positionOfHeaderGroup
                binding?.rcvPhoneBook?.setUpPositionScrollValue(positionOfHeaderGroup)
            }
        })
    }
}
