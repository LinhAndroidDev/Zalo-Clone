package com.example.messageapp.fragment

import android.graphics.Rect
import android.util.Log
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.R
import com.example.messageapp.adapter.ChatAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentChatBinding
import com.example.messageapp.model.Message
import com.example.messageapp.model.User
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.viewmodel.ChatFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : BaseFragment<FragmentChatBinding, ChatFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_chat

    private var friendData: User? = null
    private var chatAdapter: ChatAdapter? = null
    private var scrollPosition = 0

    companion object {
        const val FRIEND_DATA = "FRIEND_DATA"
    }

    override fun initView() {
        super.initView()

        binding?.root?.let { rootView ->
            rootView.viewTreeObserver.addOnGlobalLayoutListener {
                val r = Rect()
                rootView.getWindowVisibleDisplayFrame(r)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - r.bottom

                if (keypadHeight > screenHeight * 0.15) {
                    Log.e("ChatFragment", "Bàn phím đã xuất hiện")
                    // Bàn phím đã xuất hiện
                    val position = binding?.rcvChat?.computeVerticalScrollOffset() ?: 0
//                    binding?.rcvChat?.scrollBy(0, scrollPosition - position)
                    binding?.rcvChat?.scrollToPosition(chatAdapter?.itemCount?.minus(1) ?: 0)
                } else {
                    Log.e("ChatFragment", "Bàn phím đã ẩn")
                    // Bàn phím đã xuất hiện
                    scrollPosition = binding?.rcvChat?.computeVerticalScrollOffset() ?: 0 // Cập nhật vị trí scroll
                }
            }
        }

        friendData = arguments?.getParcelable(FRIEND_DATA)
        friendData?.let {
            chatAdapter = ChatAdapter(friendData?.avatar.toString(), friendData?.keyAuth.toString())
            binding?.rcvChat?.adapter = chatAdapter
            binding?.header?.setTitleChatView(friendData?.name ?: "")
        }
        binding?.edtMessage?.doOnTextChanged { text, _, _, _ ->
            if (text?.isNotEmpty() == true) {
                binding?.viewOptions?.isVisible = false
                binding?.btnSend?.isVisible = true
            } else {
                binding?.viewOptions?.isVisible = true
                binding?.btnSend?.isVisible = false
            }
        }
    }

    override fun bindData() {
        super.bindData()

        friendData?.let { friend ->
            viewModel?.getMessage(friendId = friend.keyAuth.toString())

            lifecycleScope.launch(Dispatchers.Main) {
                viewModel?.messages?.collect { messages ->
                    messages?.let {
                        chatAdapter?.setMessage(it)
                        binding?.rcvChat?.scrollToPosition(chatAdapter?.itemCount?.minus(1) ?: 0)
                    }
                }
            }
        }
    }

    override fun onClickView() {
        super.onClickView()

        binding?.btnSend?.setOnClickListener {
            friendData?.let { friend ->
                val msg = binding?.edtMessage?.text.toString()
                val receiver = friend.keyAuth.toString()
                val sender = viewModel?.shared?.getAuth().toString()
                val time = DateUtils.getTimeCurrent()
                val message = Message(msg, receiver, sender, time)
                viewModel?.sendMessage(message = message, time = time, friend = friend)
                binding?.edtMessage?.setText("")
            }
        }
    }
}