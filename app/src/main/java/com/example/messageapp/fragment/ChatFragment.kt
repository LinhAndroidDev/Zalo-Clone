package com.example.messageapp.fragment

/**
 * Created by Nguyen Huu Linh in 2024/10/01
 */

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.messageapp.PreviewPhotoActivity
import com.example.messageapp.R
import com.example.messageapp.adapter.ChatAdapter
import com.example.messageapp.adapter.ClickPhotoModel
import com.example.messageapp.argument.PreviewPhotoArgument
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetOptionPhoto
import com.example.messageapp.bottom_sheet.BottomSheetRecord
import com.example.messageapp.databinding.FragmentChatBinding
import com.example.messageapp.helper.screenHeight
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Emotion
import com.example.messageapp.model.Message
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.FileUtils
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.utils.hideKeyboard
import com.example.messageapp.viewmodel.ChatFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ChatFragment : BaseFragment<FragmentChatBinding, ChatFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_chat

    private var conversation: Conversation? = null
    private var chatAdapter: ChatAdapter? = null
    private var stateScrollable = true
    private var isChatScreenActive = false
    private var isMessageEmpty = true

    companion object {
        private const val REQUEST_CODE_MULTI_PICTURE = 1
        private const val SELECT_MULTI_PICTURE = "SELECT_MULTI_PICTURE"
    }

    private val mCallBackClickItem = object : ChatAdapter.CallBackClickItem {
        override fun onSenderLongClick(data: Pair<View, Message>) {
            showPopupOption(data.first, data.second)
        }

        override fun onReceiverLongClick(data: Pair<View, Message>) {
            showPopupOption(data.first, data.second, false)
        }

        override fun onPhotoClick(data: ClickPhotoModel) {
            val keyId = if (data.fromSender) viewModel?.shared?.getAuth()
                .toString() else conversation?.friendId.toString()
            val intent = Intent(requireActivity(), PreviewPhotoActivity::class.java)
            val previewPhotoArgument = PreviewPhotoArgument(
                message = data.message,
                indexOfPhoto = data.indexOfPhoto,
                keyId = keyId,
                photoData = data.photoData
            )
            intent.putExtra(PreviewPhotoActivity.PREVIEW_PHOTO_ARGUMENT, previewPhotoArgument)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                data.imageView,
                data.message.time
            )
            activity?.startActivity(intent, options.toBundle())
        }

        override fun onOptionMenuClick(msg: Message) {
            val bottomSheetOptionPhoto = BottomSheetOptionPhoto()
            bottomSheetOptionPhoto.show(parentFragmentManager, "")
            bottomSheetOptionPhoto.setOnClickOptionPho(object :
                BottomSheetOptionPhoto.OnClickOptionPhoto {
                override fun savePhotoOrVideo() {
                    when (TypeMessage.of(msg.type)) {
                        TypeMessage.SINGLE_PHOTO -> {
                            lifecycleScope.launch {
                                FileUtils.downloadAndSaveImage(context = requireActivity(), imageUrl = msg.singlePhoto[0])
                            }
                        }

                        TypeMessage.PHOTOS -> {
                            lifecycleScope.launch {
                                viewModel?.saveMultiPhotoWithCombine(requireActivity(), msg.photos)
                            }
                        }

                        TypeMessage.AUDIO -> {
                            lifecycleScope.launch {
                                FileUtils.downloadAudioFile(requireActivity(), msg.audio ?: "")
                            }
                        }

                        else -> {}
                    }
                }

                override fun remove() {
                    conversation?.let { viewModel?.removeMessage(it, msg.time) }
                }

            })
        }

    }

    override fun initView() {
        super.initView()
        // log event: screen_chat
        FirebaseAnalyticsInstance.logChatScreen()

        conversation = ChatFragmentArgs.fromBundle(requireArguments()).conversation
        conversation?.let {
            chatAdapter = ChatAdapter(requireActivity(), conversation?.friendId ?: "")
            chatAdapter?.setOnActionClickItem(mCallBackClickItem)
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

        binding?.edtMessage?.setOnFocusChangeListener { _, hasFocus ->
            viewModel?.updateTyping(conversation?.friendId.toString(), hasFocus)
        }
    }

    /**
     * This function is used to show popup option for each message:
     * + The position of the popup show depends on the coordinates of each message item.
     * + When the position of the message item plus its height is greater than the height of the screen,
     * the popup will show above the message item, otherwise it will show below.
     * This is how to calculate so that the popup does not lose view when it is near the bottom of the screen.
     */
    @SuppressLint("MissingInflatedId", "InflateParams", "ClickableViewAccessibility")
    private fun showPopupOption(anchor: View, message: Message, isItemSender: Boolean = true) {
        // Lấy LayoutInflater để inflate layout của PopupWindow
        val inflater = requireActivity().getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_option_chat, null)
        val layoutSender: LinearLayout = popupView.findViewById(R.id.layoutSender)
        val layoutReceiver: LinearLayout = popupView.findViewById(R.id.layoutReceiver)
        val btnCopy: LinearLayout = popupView.findViewById(R.id.btnCopy)
        val btnRemoveMessage: LinearLayout = popupView.findViewById(R.id.btnRemoveMessage)
        val layoutEmotion: LinearLayout = popupView.findViewById(R.id.layoutEmotion)
        val imgFavourite: ImageView = popupView.findViewById(R.id.imgFavourite)
        val imgLike: ImageView = popupView.findViewById(R.id.imgLike)
        val imgLaugh: ImageView = popupView.findViewById(R.id.imgLaugh)
        val imgCry: ImageView = popupView.findViewById(R.id.imgCry)
        val imgAngry: ImageView = popupView.findViewById(R.id.imgAngry)

        hideKeyboard()
        val soundEmotion = MediaPlayer.create(requireActivity(), R.raw.sound_emotion)
        soundEmotion.start()
        AnimatorUtils.scaleEmotion(requireActivity(), layoutEmotion)

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
        ).apply {
            setBackgroundDrawable(
                ContextCompat.getDrawable(requireActivity(), android.R.color.transparent)
            )
        }

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
            handleCopyMessage(message)
            popupWindow.dismiss()
        }

        btnRemoveMessage.setOnClickListener {
            conversation?.let {
                viewModel?.removeMessage(it, message.time)
                popupWindow.dismiss()
            }
        }

        imgFavourite.setOnClickListener {
            val data = mapOf(viewModel?.shared?.getAuth().toString() to 1)
            val emotion = Emotion(favourite = data)
            viewModel?.releaseEmotion(message.time, conversation?.friendId.toString(), data = emotion)
            popupWindow.dismiss()
        }

        imgLike.setOnClickListener {
            val data = mapOf(viewModel?.shared?.getAuth().toString() to 1)
            val emotion = Emotion(like = data)
            viewModel?.releaseEmotion(message.time, conversation?.friendId.toString(), data = emotion)
            popupWindow.dismiss()
        }

        imgLaugh.setOnClickListener {
            val data = mapOf(viewModel?.shared?.getAuth().toString() to 1)
            val emotion = Emotion(laugh = data)
            viewModel?.releaseEmotion(message.time, conversation?.friendId.toString(), data = emotion)
            popupWindow.dismiss()
        }

        imgCry.setOnClickListener {
            val data = mapOf(viewModel?.shared?.getAuth().toString() to 1)
            val emotion = Emotion(cry = data)
            viewModel?.releaseEmotion(message.time, conversation?.friendId.toString(), data = emotion)
            popupWindow.dismiss()
        }

        imgAngry.setOnClickListener {
            val data = mapOf(viewModel?.shared?.getAuth().toString() to 1)
            val emotion = Emotion(angry = data)
            viewModel?.releaseEmotion(message.time, conversation?.friendId.toString(), data = emotion)
            popupWindow.dismiss()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_MULTI_PICTURE -> {
                    if (data.clipData != null) {
                        val uris = arrayListOf<Uri>()
                        val count: Int = data.clipData!!.itemCount
                        for (i in 0 until count) {
                            uris.add(data.clipData!!.getItemAt(i).uri)
                        }

                        conversation?.let {
                            viewModel?.uploadListPhoto(
                                context = requireActivity(),
                                uris = uris,
                                conversation = it,
                                time = DateUtils.getTimeCurrent(),
                                sendFirst = isMessageEmpty
                            )
                        }
                        stateScrollable = true
                    }
//                    val clipData = data.clipData
//                    if (clipData != null) {
//                        val uris = arrayListOf<Pair<Uri, Int>>()
//                        // Người dùng chọn nhiều file
//                        for (i in 0 until clipData.itemCount) {
//                            val mediaUri = clipData.getItemAt(i).uri
//                            val mimeType = activity?.contentResolver?.getType(mediaUri)
//                            when {
//                                mimeType?.startsWith("image/") == true -> {
//                                    uris.add(Pair(clipData.getItemAt(i).uri, 0))
//                                }
//                                mimeType?.startsWith("video/") == true -> {
//                                    uris.add(Pair(clipData.getItemAt(i).uri, 1))
//                                }
//                            }
//                        }
//                        conversation?.let {
//                            viewModel?.uploadListPhoto(
//                                context = requireActivity(),
//                                uris = uris,
//                                conversation = it,
//                                time = DateUtils.getTimeCurrent()
//                            )
//                        }
//                        stateScrollable = true
                    }

            }
        }
    }

    override fun bindData() {
        super.bindData()

        conversation?.let { cvt ->
            viewModel?.getMessage(friendId = cvt.friendId)
            viewModel?.checkShowTyping(friendId = cvt.friendId)

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel?.messages?.collect { messages ->
                        messages?.let { msg ->
                            chatAdapter?.updateDiffList(msg)
                            if (stateScrollable) {
                                binding?.rcvChat?.scrollToPosition(
                                    chatAdapter?.itemCount?.minus(1) ?: 0
                                )
                                stateScrollable = false
                            }
                            if (isChatScreenActive && messages.isNotEmpty()) {
                                isMessageEmpty = false
                                updateSeenMessage(msg)
                            }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel?.typing?.collect { typing ->
                binding?.typingView?.isVisible = typing
            }
        }
    }

    /**
     * This function update seen message from friend of user:
     * + Show avatar friend seen when item last message is from user
     * and friend seen message
     */
    private fun updateSeenMessage(msg: ArrayList<Message>) {
        val userId = viewModel?.shared?.getAuth() ?: ""
        FireBaseInstance.getConversationRlt(
            friendId = conversation?.friendId ?: "",
            userId = userId,
            success = { cvt ->
                if (cvt.isSeenMessage() && msg[msg.lastIndex].sender == userId) {
                    chatAdapter?.seen = true
                    chatAdapter?.notifyItemChanged(msg.lastIndex)
                } else {
                    chatAdapter?.seen = false
                    chatAdapter?.notifyItemChanged(msg.lastIndex)
                }
                conversation?.let { viewModel?.updateSeenMessage(msg[msg.lastIndex], it) }
            }
        )
    }

    private fun handleCopyMessage(message: Message) {
        val clipboard: ClipboardManager? =
            activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("label", message.message)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(requireActivity(), "Bạn đã sao chép tin nhắn", Toast.LENGTH_SHORT).show()
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
                // log event: send_message
                FirebaseAnalyticsInstance.logSendMessage(messageType = msg, messageLength = msg.length, receiverId = receiver)
                viewModel?.sendMessage(message = message, time = time, conversation = cvt, sendFirst = isMessageEmpty)
                binding?.edtMessage?.setText("")
            }
            stateScrollable = true
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

//            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
//                type = "*/*"
//                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
//                addCategory(Intent.CATEGORY_OPENABLE)
//                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Cho phép chọn nhiều file
//            }
//            startActivityForResult(
//                Intent.createChooser(intent, "select multi"),
//                REQUEST_CODE_MULTI_PICTURE
//            )
        }

        binding?.btnMicro?.setOnClickListener {
            val bottomSheetRecord = BottomSheetRecord()
            bottomSheetRecord.onRecordListener = { path ->
                conversation?.let { cvt ->
                    val file = File(path)
                    viewModel?.uploadAudio(
                        friendId = cvt.friendId,
                        uriAudio = Uri.fromFile(file),
                        time = DateUtils.getTimeCurrent(),
                        conversation = cvt,
                        sendFirst = isMessageEmpty
                    )
                }
            }
            bottomSheetRecord.show(parentFragmentManager, "")
        }
    }

    override fun onResume() {
        super.onResume()
        isChatScreenActive = true
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    override fun onPause() {
        isChatScreenActive = false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        viewModel?.updateTyping(conversation?.friendId.toString(), false)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel?.updateTyping(conversation?.friendId.toString(), false)
    }
}