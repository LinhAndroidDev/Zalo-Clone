package com.example.messageapp.fragment

/**
 * Created by Nguyen Huu Linh in 2024/10/01
 */

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.messageapp.R
import com.example.messageapp.adapter.ChatAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentChatBinding
import com.example.messageapp.helper.screenHeight
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Message
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.viewmodel.ChatFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : BaseFragment<FragmentChatBinding, ChatFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_chat

    private var conversation: Conversation? = null
    private var chatAdapter: ChatAdapter? = null
    private var scrollPosition = 0

    companion object {
        private const val REQUEST_CODE_MULTI_PICTURE = 1
        private const val SELECT_MULTI_PICTURE = "SELECT_MULTI_PICTURE"
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
                    scrollPosition = binding?.rcvChat?.computeVerticalScrollOffset()
                        ?: 0 // Cập nhật vị trí scroll
                }
            }
        }

        conversation = ChatFragmentArgs.fromBundle(requireArguments()).conversation
        conversation?.let {
            chatAdapter =
                ChatAdapter(conversation?.friendId.toString(), viewModel?.shared?.getAuth() ?: "")
            chatAdapter?.longClickItemSender = { data ->
                showPopupOption(data.first, data.second)
            }
            chatAdapter?.longClickItemReceiver = { data ->
                showPopupOption(data.first, data.second, false)
            }
//            chatAdapter?.seenMessage = {
//                binding?.rcvChat?.scrollToPosition(chatAdapter?.itemCount?.minus(1) ?: 0)
//            }
            binding?.rcvChat?.adapter = chatAdapter
            binding?.header?.setTitleChatView(conversation?.name ?: "")
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

    /**
     * This function is used to show popup option for each message:
     * + The position of the popup show depends on the coordinates of each message item.
     * + When the position of the message item plus its height is greater than the height of the screen,
     * the popup will show above the message item, otherwise it will show below.
     * This is how to calculate so that the popup does not lose view when it is near the bottom of the screen.
     */
    @SuppressLint("MissingInflatedId", "InflateParams")
    private fun showPopupOption(anchor: View, message: Message, isItemSender: Boolean = true) {
        // Lấy LayoutInflater để inflate layout của PopupWindow
        val inflater = requireActivity().getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_option_chat, null)
        val layoutSender: LinearLayout = popupView.findViewById(R.id.layoutSender)
        val layoutReceiver: LinearLayout = popupView.findViewById(R.id.layoutReceiver)
        val btnCopy: LinearLayout = popupView.findViewById(R.id.btnCopy)

        if (isItemSender) {
            layoutSender.isVisible = true
            popupView.findViewById<TextView>(R.id.tvSender).text = message.message
            popupView.findViewById<TextView>(R.id.tvTimeSender).text =
                DateUtils.convertTimeToHour(message.time)
        } else {
            layoutReceiver.isVisible = true
            popupView.findViewById<TextView>(R.id.tvReceiver).text = message.message
            popupView.findViewById<TextView>(R.id.tvTimeReceiver).text =
                DateUtils.convertTimeToHour(message.time)
        }

        // Tạo PopupWindow với chiều rộng và chiều cao
        val popupWindow = PopupWindow(
            popupView,
            resources.getDimensionPixelSize(R.dimen.width_popup_options),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // True để Popup có thể bị tắt khi bấm ra ngoài
        )

        popupWindow.setBackgroundDrawable(
            ContextCompat.getDrawable(requireActivity(), android.R.color.transparent)
        )

        binding?.viewCoverPopupOptions?.isVisible = true
        popupWindow.setOnDismissListener {
            binding?.viewCoverPopupOptions?.isVisible = false
        }

        // Lấy vị trí của item trên màn hình
        val itemLocation = IntArray(2)
        anchor.getLocationOnScreen(itemLocation)
        val itemYPosition = itemLocation[1]

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val height = popupView.measuredHeight

        // Kiểm tra vị trí của item so với chiều cao của màn hình
        if (itemYPosition + height > screenHeight) {
            // Nếu item nằm ở nửa dưới màn hình, hiển thị PopupWindow phía trên item
            popupWindow.showAsDropDown(anchor, 0, -height)
        } else {
            // Nếu item nằm ở nửa trên màn hình, hiển thị PopupWindow bình thường bên dưới item
            popupWindow.showAsDropDown(anchor, 0, -anchor.height)
        }

        btnCopy.setOnClickListener {
            val clipboard: ClipboardManager? =
                activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("label", message.message)
            clipboard?.setPrimaryClip(clip)
            popupWindow.dismiss()
            Toast.makeText(requireActivity(), "Bạn đã sao chép tin nhắn", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_MULTI_PICTURE -> {
                    if (data.clipData != null) {
                        val count: Int = data.clipData!!.itemCount
                    }
                }
            }
        }
    }

    override fun bindData() {
        super.bindData()

        conversation?.let { cvt->
            viewModel?.getMessage(friendId = cvt.friendId)

            lifecycleScope.launch(Dispatchers.Main) {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel?.messages?.collect { messages ->
                        messages?.let { msg ->
                            chatAdapter?.setMessage(msg)
                            binding?.rcvChat?.scrollToPosition(chatAdapter?.itemCount?.minus(1) ?: 0)
                            FireBaseInstance.getConversationRlt(
                                friendId = conversation?.friendId ?: "",
                                userId = viewModel?.shared?.getAuth() ?: "",
                                success = { cvt ->
                                    if(cvt.seen == "1") {
                                        FireBaseInstance.getInfoUser(conversation?.friendId ?: "") { user ->
                                            chatAdapter?.seen = true
                                            chatAdapter?.notifyItemChanged(msg.lastIndex)
                                        }
                                    }
                                }
                            )
                            conversation?.let {  viewModel?.updateSeenMessage(msg, it) }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onClickView() {
        super.onClickView()

        binding?.btnSend?.setOnClickListener {
            conversation?.let { cvt ->
                val msg = binding?.edtMessage?.text.toString()
                val receiver = cvt.friendId
                val sender = viewModel?.shared?.getAuth().toString()
                val time = DateUtils.getTimeCurrent()
                val message = Message(msg, receiver, sender, time)
                viewModel?.sendMessage(message = message, time = time, conversation = cvt)
                binding?.edtMessage?.setText("")
            }
        }

        binding?.btnSelectImage?.setOnClickListener {
            val intent = Intent().apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                action = Intent.ACTION_GET_CONTENT
            }
            startActivityForResult(
                Intent.createChooser(intent, SELECT_MULTI_PICTURE),
                REQUEST_CODE_MULTI_PICTURE
            )
        }
    }
}