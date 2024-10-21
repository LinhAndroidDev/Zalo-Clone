package com.example.messageapp.fragment

import android.annotation.SuppressLint
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.adapter.RequestFriendAdapter
import com.example.messageapp.adapter.SuggestFriendRequestAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentFriendRequestBinding
import com.example.messageapp.helper.avatars
import com.example.messageapp.model.Friend
import com.example.messageapp.viewmodel.FragmentFriendRequestViewModel

class FriendRequestFragment : BaseFragment<FragmentFriendRequestBinding, FragmentFriendRequestViewModel>() {
    override val layoutResId: Int = R.layout.fragment_friend_request
    private val requestFriendAdapter by lazy { RequestFriendAdapter() }
    private var friends = mutableListOf<Friend>()

    override fun initView() {
        super.initView()

        val names = mutableListOf("An", "Bảo", "Brian", "Alex", "Aiden", "Finn", "Khánh", "Duy", "Emma", "Samuel")
        names.forEachIndexed { index, s ->
            friends.add(Friend(s, avatars[index]))
        }
        val list = friends.take(2).toMutableList()
        requestFriendAdapter.items = list
        binding?.rcvRequestFriend?.adapter = requestFriendAdapter

        val suggestFriendAdapter = SuggestFriendRequestAdapter()
        suggestFriendAdapter.items = friends.drop(8).toMutableList()
        binding?.rcvSuggestFriendRequest?.adapter = suggestFriendAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onClickView() {
        super.onClickView()

        binding?.viewSeeMore?.setOnClickListener {
            binding?.viewSeeMore?.isVisible = false
            val list = friends.drop(2) as ArrayList<Friend>
            requestFriendAdapter.addItems(list)
        }
    }
}