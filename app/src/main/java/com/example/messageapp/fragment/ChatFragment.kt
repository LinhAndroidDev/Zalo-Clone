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
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.messageapp.PersonalActivity
import com.example.messageapp.PreviewPhotoActivity
import com.example.messageapp.R
import com.example.messageapp.adapter.ChatAdapter
import com.example.messageapp.adapter.ClickPhotoModel
import com.example.messageapp.adapter.LongClickPhotoModel
import com.example.messageapp.adapter.MentionSuggestionAdapter
import com.example.messageapp.adapter.ReceiverViewHolder
import com.example.messageapp.adapter.SenderViewHolder
import com.example.messageapp.argument.PreviewPhotoArgument
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetAddGroupMembers
import com.example.messageapp.bottom_sheet.BottomSheetOptionPhoto
import com.example.messageapp.bottom_sheet.BottomSheetRecord
import com.example.messageapp.bottom_sheet.BottomSheetRemoveGroupMembers
import com.example.messageapp.bottom_sheet.BottomSheetSticker
import com.example.messageapp.databinding.FragmentChatBinding
import com.example.messageapp.helper.screenHeight
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.EmotionType
import com.example.messageapp.model.Message
import com.example.messageapp.model.MessageMention
import android.view.inputmethod.InputMethodManager
import com.example.messageapp.model.TypeMessage
import kotlin.math.max
import kotlin.math.min
import com.example.messageapp.model.UserPresence
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.utils.DateUtils
import com.example.messageapp.utils.EmotionBurstEffect
import com.example.messageapp.utils.EmotionReactionDetector
import com.example.messageapp.utils.FileUtils
import com.example.messageapp.utils.FileUtils.isLikelyVideoUrl
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.utils.MentionHelper
import com.example.messageapp.utils.MessageReplyHelper
import com.example.messageapp.utils.hideKeyboard
import com.example.messageapp.viewmodel.ChatFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.io.File
import androidx.core.net.toUri

@AndroidEntryPoint
class ChatFragment : BaseFragment<FragmentChatBinding, ChatFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_chat

    private var conversation: Conversation? = null
    private var chatAdapter: ChatAdapter? = null
    private var stateScrollable = true
    private var isChatScreenActive = false
    private var isMessageEmpty = true
    private var lastFriendPresence: UserPresence? = null
    private var presenceRefreshJob: Job? = null
    private var mentionSuggestionAdapter: MentionSuggestionAdapter? = null
    private val pendingMentions = mutableListOf<MessageMention>()
    private var activeMentionQuery: MentionHelper.MentionQuery? = null
    private var groupMentionMembers: List<MentionHelper.MentionCandidate> = emptyList()
    private var replyingToMessage: Message? = null
    private var replyHighlightScrollListener: RecyclerView.OnScrollListener? = null
    private var lastMessagesSnapshot: List<Message> = emptyList()
    private val allMentionCandidate by lazy {
        MentionHelper.allMentionCandidate(getString(R.string.mention_all_label))
    }

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

        override fun onPhotoLongClick(data: LongClickPhotoModel) {
            showPopupOption(
                anchor = data.anchor,
                message = data.message,
                isItemSender = data.fromSender,
                photoPreviewUrl = data.photoUrl,
                photoIntrinsicWidth = data.intrinsicWidth,
                photoIntrinsicHeight = data.intrinsicHeight,
            )
        }

        override fun onPhotoClick(data: ClickPhotoModel) {
            val url = data.photoData.getOrNull(data.indexOfPhoto) ?: return
            if (isLikelyVideoUrl(url)) {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(url.toUri(), "video/*")
                }
                try {
                    startActivity(Intent.createChooser(viewIntent, null))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), "Không thể mở video", Toast.LENGTH_SHORT).show()
                }
                return
            }
            val keyId = when {
                data.fromSender -> viewModel?.shared?.getAuth().orEmpty()
                conversation?.isGroupThread() == true -> conversation?.friendId.orEmpty()
                else -> conversation?.friendId.orEmpty()
            }
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

        override fun onReplyQuoteClick(messageTime: String) {
            scrollToMessage(messageTime)
        }

    }

    override fun initView() {
        super.initView()
        // log event: screen_chat
        FirebaseAnalyticsInstance.logChatScreen()

        conversation = ChatFragmentArgs.fromBundle(requireArguments()).conversation
        conversation?.let { cvt ->
            val uid = viewModel?.shared?.getAuth().orEmpty()
            chatAdapter = ChatAdapter(
                requireActivity(),
                cvt.friendId,
                cvt.isGroupThread(),
                uid,
                viewModel?.shared?.getNameUser().orEmpty(),
                cvt.name,
                loadUserAvatar = { userId, onResult ->
                    viewModel?.loadUserAvatar(userId, onResult)
                },
            )
            chatAdapter?.updateReplyNameContext(
                myName = viewModel?.shared?.getNameUser().orEmpty(),
                peerDisplayName = cvt.name,
                groupMembers = groupMentionMembers,
            )
            chatAdapter?.setOnActionClickItem(mCallBackClickItem)
            binding?.rcvChat?.adapter = chatAdapter
            binding?.header?.setTitleChatView(cvt.name)
            if (cvt.isGroupThread()) {
                binding?.header?.setFriendStatusVisible(false)
                binding?.header?.setChatMenuVisible(true)
                binding?.header?.onChatMenuClick = { showGroupMenuPopup() }
                setupMentionPicker()
            } else {
                binding?.header?.setFriendStatusVisible(true)
                binding?.header?.setChatMenuVisible(false)
                binding?.header?.onChatMenuClick = null
                viewModel?.startObservingFriendPresence(cvt.friendId)
            }
            binding?.header?.showInfoFriend = if (cvt.isGroupThread()) {
                null
            } else {
                {
                    val intent = Intent(requireActivity(), PersonalActivity::class.java)
                    intent.putExtra(PersonalActivity.FRIEND_ID_KEY, cvt.friendId)
                    startActivity(intent)
                    activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        }
        binding?.edtMessage?.doOnTextChanged { text, _, _, _ ->
            if (text?.isNotEmpty() == true) {
                binding?.viewOptions?.isVisible = false
                binding?.btnSend?.isVisible = true
            } else {
                binding?.viewOptions?.isVisible = true
                binding?.btnSend?.isVisible = false
                pendingMentions.clear()
                hideMentionPicker()
            }
            if (conversation?.isGroupThread() == true) {
                val synced = MentionHelper.syncPendingMentions(text?.toString().orEmpty(), pendingMentions)
                pendingMentions.clear()
                pendingMentions.addAll(synced)
                refreshInputMentionHighlight()
                updateMentionPicker(text)
            }
        }

        binding?.edtMessage?.setOnClickListener {
            if (conversation?.isGroupThread() == true) {
                updateMentionPicker(binding?.edtMessage?.text)
            }
        }

        binding?.edtMessage?.setOnFocusChangeListener { _, hasFocus ->
            conversation?.let { viewModel?.updateTyping(it, hasFocus) }
        }

        binding?.btnCancelReply?.setOnClickListener { clearReply() }
    }

    @SuppressLint("InflateParams")
    private fun showGroupMenuPopup() {
        val groupId = conversation?.friendId.orEmpty()
        if (groupId.isBlank() || conversation?.isGroupThread() != true) return

        val anchor = binding?.header?.getChatMenuAnchor() ?: return
        val inflater = requireActivity().getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_group_menu, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            setBackgroundDrawable(
                ContextCompat.getDrawable(requireActivity(), android.R.color.transparent),
            )
        }

        binding?.viewCoverPopupOptions?.isVisible = true
        popupWindow.setOnDismissListener {
            binding?.viewCoverPopupOptions?.isVisible = false
        }

        popupView.findViewById<View>(R.id.btnAddMembers).setOnClickListener {
            popupWindow.dismiss()
            BottomSheetAddGroupMembers.newInstance(groupId)
                .show(childFragmentManager, BottomSheetAddGroupMembers.TAG)
        }
        popupView.findViewById<View>(R.id.btnRemoveMembers).setOnClickListener {
            popupWindow.dismiss()
            BottomSheetRemoveGroupMembers.newInstance(groupId)
                .show(childFragmentManager, BottomSheetRemoveGroupMembers.TAG)
        }
        popupView.findViewById<View>(R.id.btnLeaveGroup).setOnClickListener {
            popupWindow.dismiss()
            showLeaveGroupConfirmDialog(groupId)
        }

        popupWindow.showAsDropDown(anchor, 0, 0)
    }

    private fun showLeaveGroupConfirmDialog(groupId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.group_leave_confirm_title)
            .setMessage(R.string.group_leave_confirm_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.group_leave_confirm_ok) { _, _ ->
                viewModel?.leaveGroup(groupId) {
                    findNavController().popBackStack()
                }
            }
            .show()
    }

    /**
     * This function is used to show popup option for each message:
     * + The position of the popup show depends on the coordinates of each message item.
     * + When the position of the message item plus its height is greater than the height of the screen,
     * the popup will show above the message item, otherwise it will show below.
     * This is how to calculate so that the popup does not lose view when it is near the bottom of the screen.
     */
    @SuppressLint("MissingInflatedId", "InflateParams", "ClickableViewAccessibility")
    private fun showPopupOption(
        anchor: View,
        message: Message,
        isItemSender: Boolean = true,
        photoPreviewUrl: String? = null,
        photoIntrinsicWidth: Int = 0,
        photoIntrinsicHeight: Int = 0,
    ) {
        // Lấy LayoutInflater để inflate layout của PopupWindow
        val inflater = requireActivity().getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_option_chat, null)
        val btnCopy: LinearLayout = popupView.findViewById(R.id.btnCopy)
        val btnReply: LinearLayout = popupView.findViewById(R.id.btnReply)
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

        bindPopupPreview(
            popupView = popupView,
            message = message,
            isItemSender = isItemSender,
            photoPreviewUrl = photoPreviewUrl,
            photoIntrinsicWidth = photoIntrinsicWidth,
            photoIntrinsicHeight = photoIntrinsicHeight,
        )
        btnCopy.isVisible = photoPreviewUrl == null

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

        btnReply.setOnClickListener {
            startReply(message)
            popupWindow.dismiss()
        }

        btnRemoveMessage.setOnClickListener {
            conversation?.let {
                viewModel?.removeMessage(it, message.time)
                popupWindow.dismiss()
            }
        }

        imgFavourite.setOnClickListener {
            reactToMessage(message, EmotionType.FAVOURITE, anchor)
            popupWindow.dismiss()
        }

        imgLike.setOnClickListener {
            reactToMessage(message, EmotionType.LIKE, anchor)
            popupWindow.dismiss()
        }

        imgLaugh.setOnClickListener {
            reactToMessage(message, EmotionType.LAUGH, anchor)
            popupWindow.dismiss()
        }

        imgCry.setOnClickListener {
            reactToMessage(message, EmotionType.CRY, anchor)
            popupWindow.dismiss()
        }

        imgAngry.setOnClickListener {
            reactToMessage(message, EmotionType.ANGRY, anchor)
            popupWindow.dismiss()
        }
    }

    private fun reactToMessage(message: Message, type: EmotionType, anchor: View) {
        val cvt = conversation ?: return
        viewModel?.toggleMessageReaction(message.time, cvt, type) { appliedType ->
            playEmotionBurst(appliedType, anchor)
        }
    }

    private fun playEmotionBurst(type: EmotionType, anchor: View) {
        val activity = activity ?: return
        EmotionBurstEffect.play(activity, anchor, type)
    }

    private fun playRemoteEmotionBurst(messageTime: String, type: EmotionType) {
        if (!isChatScreenActive) return
        val activity = activity ?: return
        val anchor = findEmotionBurstAnchor(messageTime) ?: return
        EmotionBurstEffect.play(activity, anchor, type)
    }

    private fun findEmotionBurstAnchor(messageTime: String): View? {
        val recyclerView = binding?.rcvChat ?: return null
        val index = chatAdapter?.indexOfMessageTime(messageTime) ?: return null
        val holder = recyclerView.findViewHolderForAdapterPosition(index) ?: return null
        return when (holder) {
            is SenderViewHolder -> if (holder.v.viewReleaseEmotion.isVisible) {
                holder.v.viewReleaseEmotion
            } else {
                holder.v.viewEmotion
            }
            is ReceiverViewHolder -> if (holder.v.viewReleaseEmotion.isVisible) {
                holder.v.viewReleaseEmotion
            } else {
                holder.v.viewEmotion
            }
            else -> holder.itemView
        }
    }

    private fun bindPopupPreview(
        popupView: View,
        message: Message,
        isItemSender: Boolean,
        photoPreviewUrl: String?,
        photoIntrinsicWidth: Int,
        photoIntrinsicHeight: Int,
    ) {
        val timeText = DateUtils.convertTimeToHour(message.time)
        if (isItemSender) {
            val layoutSender = popupView.findViewById<LinearLayout>(R.id.layoutSender)
            val tvTimeSender = popupView.findViewById<TextView>(R.id.tvTimeSender)
            layoutSender.isVisible = true
            popupView.findViewById<LinearLayout>(R.id.layoutReceiver).isVisible = false
            val tvSender = popupView.findViewById<TextView>(R.id.tvSender)
            val cardPreview = popupView.findViewById<View>(R.id.cardPreviewSender)
            val imgPreview = popupView.findViewById<ImageView>(R.id.imgPreviewSender)
            if (photoPreviewUrl.isNullOrBlank()) {
                applyTextBubblePreviewHeader(layoutSender, tvTimeSender, isSender = true)
                tvTimeSender.text = timeText
                tvSender.isVisible = true
                tvSender.text = message.message
                cardPreview.isVisible = false
            } else {
                applyPhotoPreviewHeader(layoutSender, tvTimeSender)
                tvSender.isVisible = false
                bindPhotoPreviewImage(
                    imgPreview = imgPreview,
                    cardPreview = cardPreview,
                    photoUrl = photoPreviewUrl,
                    intrinsicWidth = photoIntrinsicWidth,
                    intrinsicHeight = photoIntrinsicHeight,
                )
            }
            return
        }

        val layoutReceiver = popupView.findViewById<LinearLayout>(R.id.layoutReceiver)
        val tvTimeReceiver = popupView.findViewById<TextView>(R.id.tvTimeReceiver)
        layoutReceiver.isVisible = true
        popupView.findViewById<LinearLayout>(R.id.layoutSender).isVisible = false
        val tvReceiver = popupView.findViewById<TextView>(R.id.tvReceiver)
        val cardPreview = popupView.findViewById<View>(R.id.cardPreviewReceiver)
        val imgPreview = popupView.findViewById<ImageView>(R.id.imgPreviewReceiver)
        if (photoPreviewUrl.isNullOrBlank()) {
            applyTextBubblePreviewHeader(layoutReceiver, tvTimeReceiver, isSender = false)
            tvTimeReceiver.text = timeText
            tvReceiver.isVisible = true
            tvReceiver.text = message.message
            cardPreview.isVisible = false
        } else {
            applyPhotoPreviewHeader(layoutReceiver, tvTimeReceiver)
            tvReceiver.isVisible = false
            bindPhotoPreviewImage(
                imgPreview = imgPreview,
                cardPreview = cardPreview,
                photoUrl = photoPreviewUrl,
                intrinsicWidth = photoIntrinsicWidth,
                intrinsicHeight = photoIntrinsicHeight,
            )
        }
    }

    private fun applyTextBubblePreviewHeader(
        container: LinearLayout,
        tvTime: TextView,
        isSender: Boolean,
    ) {
        container.setBackgroundResource(
            if (isSender) R.drawable.bg_sender else R.drawable.bg_receiver,
        )
        val density = resources.displayMetrics.density
        val horizontalPad = (15 * density).toInt()
        val verticalPad = (7 * density).toInt()
        container.setPadding(horizontalPad, verticalPad, horizontalPad, verticalPad)
        tvTime.isVisible = true
    }

    private fun applyPhotoPreviewHeader(container: LinearLayout, tvTime: TextView) {
        container.background = null
        container.setPadding(0, 0, 0, 0)
        tvTime.isVisible = false
    }

    private fun bindPhotoPreviewImage(
        imgPreview: ImageView,
        cardPreview: View,
        photoUrl: String,
        intrinsicWidth: Int,
        intrinsicHeight: Int,
    ) {
        val (displayW, displayH) = popupPreviewDisplaySize(intrinsicWidth, intrinsicHeight)
        val layoutParams = (imgPreview.layoutParams as? FrameLayout.LayoutParams)
            ?: FrameLayout.LayoutParams(displayW, displayH)
        layoutParams.width = displayW
        layoutParams.height = displayH
        imgPreview.layoutParams = layoutParams
        imgPreview.scaleType = ImageView.ScaleType.FIT_CENTER
        cardPreview.isVisible = true
        requireContext().loadImg(
            photoUrl,
            imgPreview,
            R.drawable.bg_grey_equal,
        )
    }

    private fun popupPreviewDisplaySize(intrinsicW: Int, intrinsicH: Int): Pair<Int, Int> {
        val density = resources.displayMetrics.density
        val endMargin = (15 * density).toInt()
        val maxW = resources.getDimensionPixelSize(R.dimen.width_popup_options) - endMargin
        val maxH = min((screenHeight * 0.48f).toInt(), (300 * density).toInt())

        var w = intrinsicW
        var h = intrinsicH
        if (w <= 0 || h <= 0) {
            w = maxW
            h = (maxW * 0.75f).toInt()
            return w to h
        }

        val scale = min(maxW / w.toFloat(), maxH / h.toFloat())
        return maxOf(1, (w * scale).toInt()) to maxOf(1, (h * scale).toInt())
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_MULTI_PICTURE -> {
                    val uris = arrayListOf<Uri>()
                    if (data.clipData != null) {
                        val count: Int = data.clipData!!.itemCount
                        for (i in 0 until count) {
                            uris.add(data.clipData!!.getItemAt(i).uri)
                        }
                    } else {
                        data.data?.let { uris.add(it) }
                    }
                    if (uris.isNotEmpty()) {
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
                }
            }
        }
    }

    override fun bindData() {
        super.bindData()

        conversation?.let { cvt ->
            viewModel?.getMessage(cvt)
            viewModel?.observeTyping(cvt)
            if (cvt.isGroupThread()) {
                viewModel?.startGroupReadTracking(cvt.friendId)
            } else {
                viewModel?.startObservingPeerConversation(cvt.friendId)
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel?.mentionCandidates?.collect { members ->
                        groupMentionMembers = members
                        chatAdapter?.updateReplyNameContext(
                            myName = viewModel?.shared?.getNameUser().orEmpty(),
                            peerDisplayName = cvt.name,
                            groupMembers = members,
                        )
                        updateMentionPicker(binding?.edtMessage?.text)
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel?.messages?.collect { messages ->
                        messages?.let { msg ->
                            val previousMessages = lastMessagesSnapshot
                            chatAdapter?.updateDiffList(msg)
                            if (isChatScreenActive && previousMessages.isNotEmpty()) {
                                val myUserId = viewModel?.shared?.getAuth().orEmpty()
                                val remoteChanges = EmotionReactionDetector.detectRemoteReactionChanges(
                                    previous = previousMessages,
                                    current = msg,
                                    myUserId = myUserId,
                                )
                                if (remoteChanges.isNotEmpty()) {
                                    binding?.rcvChat?.post {
                                        remoteChanges.forEach { change ->
                                            playRemoteEmotionBurst(change.messageTime, change.type)
                                        }
                                    }
                                }
                            }
                            lastMessagesSnapshot = ArrayList(msg)
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.peerConversation?.collect { conv ->
                    val msg = lastMessagesSnapshot
                    if (conv == null || msg.isEmpty()) return@collect
                    val userId = viewModel?.shared?.getAuth().orEmpty()
                    chatAdapter?.seen = conv.isSeenMessage() && msg.last().sender == userId
                    chatAdapter?.notifyItemChanged(msg.lastIndex)
                }
            }
        }

        lifecycleScope.launch {
            viewModel?.typing?.collect { typing ->
                binding?.typingView?.isVisible = typing
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.friendPresence?.collect { presence ->
                    lastFriendPresence = presence
                    updateFriendStatusHeader(presence)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.groupLastMessageReaders?.collect { readerIds ->
                    chatAdapter?.updateGroupReaders(readerIds)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.cloudUploadProgress?.collect { pct ->
                    val b = binding ?: return@collect
                    b.uploadProgressContainer.isVisible = pct != null
                    if (pct != null) {
                        val p = pct.roundToInt().coerceIn(0, 100)
                        b.uploadProgressBar.progress = p
                        b.uploadProgressPercent.text =
                            getString(R.string.chat_upload_progress, p)
                    }
                }
            }
        }
    }

    private fun updateFriendStatusHeader(presence: UserPresence?) {
        if (conversation?.isGroupThread() == true) {
            binding?.header?.setFriendStatusVisible(false)
            return
        }
        binding?.header?.setFriendStatusVisible(true)
        val currentPresence = presence ?: return
        binding?.header?.setFriendStatus(
            DateUtils.formatLastSeenStatus(currentPresence.online, currentPresence.lastSeen),
        )
    }

    /**
     * This function update seen message from friend of user:
     * + Show avatar friend seen when item last message is from user
     * and friend seen message
     */
    private fun updateSeenMessage(msg: ArrayList<Message>) {
        val cvt = conversation ?: return
        if (msg.isEmpty()) return
        if (cvt.isGroupThread()) {
            chatAdapter?.notifyItemChanged(msg.lastIndex)
        }
        viewModel?.updateSeenMessage(msg[msg.lastIndex], cvt)
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
                val rawText = binding?.edtMessage?.text.toString()
                val mentions = if (cvt.isGroupThread()) {
                    MentionHelper.syncPendingMentions(rawText, pendingMentions)
                } else {
                    emptyList()
                }
                val receiver = cvt.friendId
                val sender = viewModel?.shared?.getAuth().toString()
                val time = DateUtils.getTimeCurrent()
                val message = Message(
                    message = rawText,
                    receiver = receiver,
                    sender = sender,
                    time = time,
                    mentions = mentions,
                    replyTo = replyingToMessage?.let {
                        MessageReplyHelper.buildMessageReply(
                            requireContext(),
                            it,
                            resolveSenderNameForMessage(it.sender),
                        )
                    },
                )
                // log event: send_message
                FirebaseAnalyticsInstance.logSendMessage(messageType = rawText, messageLength = rawText.length, receiverId = receiver)
                viewModel?.sendMessage(message = message, time = time, conversation = cvt, sendFirst = isMessageEmpty)
                binding?.edtMessage?.setText("")
                pendingMentions.clear()
                hideMentionPicker()
                clearReply()
            }
            stateScrollable = true
        }

        binding?.btnSelectImage?.setOnClickListener {
            clearReply()
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(
                Intent.createChooser(intent, SELECT_MULTI_PICTURE),
                REQUEST_CODE_MULTI_PICTURE
            )
        }

        binding?.btnMicro?.setOnClickListener {
            clearReply()
            val bottomSheetRecord = BottomSheetRecord()
            bottomSheetRecord.onRecordListener = { path ->
                conversation?.let { cvt ->
                    val file = File(path)
                    viewModel?.uploadAudio(
                        uriAudio = Uri.fromFile(file),
                        time = DateUtils.getTimeCurrent(),
                        conversation = cvt,
                        sendFirst = isMessageEmpty
                    )
                }
            }
            bottomSheetRecord.show(parentFragmentManager, "")
        }

        binding?.btnSticker?.setOnClickListener {
            val bottomSheetSticker = BottomSheetSticker()
            bottomSheetSticker.show(parentFragmentManager, "")
        }
    }

    override fun onResume() {
        super.onResume()
        isChatScreenActive = true
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        presenceRefreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(60_000L)
                lastFriendPresence?.let { updateFriendStatusHeader(it) }
            }
        }
    }

    override fun onPause() {
        isChatScreenActive = false
        presenceRefreshJob?.cancel()
        presenceRefreshJob = null
        super.onPause()
    }

    override fun onDestroyView() {
        viewModel?.stopObservingFriendPresence()
        viewModel?.stopObservingPeerConversation()
        viewModel?.stopGroupReadTracking()
        pendingMentions.clear()
        hideMentionPicker()
        clearReply()
        replyHighlightScrollListener?.let { binding?.rcvChat?.removeOnScrollListener(it) }
        replyHighlightScrollListener = null
        chatAdapter?.clearReplyHighlight()
        lastMessagesSnapshot = emptyList()
        super.onDestroyView()
    }

    private fun resolveSenderNameForMessage(senderId: String): String {
        MessageReplyHelper.cachedUserDisplayName(senderId)?.let { return it }
        val cvt = conversation ?: return ""
        val myUserId = viewModel?.shared?.getAuth().orEmpty()
        return MessageReplyHelper.resolveSenderName(
            senderId = senderId,
            myUserId = myUserId,
            myName = viewModel?.shared?.getNameUser().orEmpty(),
            groupMembers = groupMentionMembers,
            peerUserId = if (!cvt.isGroupThread()) cvt.friendId else "",
            peerDisplayName = if (!cvt.isGroupThread()) cvt.name else "",
        )
    }

    private fun bindReplySenderName(senderId: String) {
        val syncName = resolveSenderNameForMessage(senderId)
        if (syncName.isNotBlank()) {
            binding?.tvReplySenderName?.text = syncName
            binding?.tvReplySenderName?.isVisible = true
            return
        }
        binding?.tvReplySenderName?.isVisible = false
        MessageReplyHelper.fetchUserDisplayName(senderId) { fetchedName ->
            if (replyingToMessage?.sender != senderId) return@fetchUserDisplayName
            if (fetchedName.isBlank()) return@fetchUserDisplayName
            binding?.tvReplySenderName?.text = fetchedName
            binding?.tvReplySenderName?.isVisible = true
        }
    }

    private fun startReply(message: Message) {
        replyingToMessage = message
        bindReplyBar(message)
        binding?.replyPreviewContainer?.isVisible = true
        binding?.edtMessage?.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding?.edtMessage, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun bindReplyBar(message: Message) {
        val ctx = requireContext()
        bindReplySenderName(message.sender)
        binding?.tvReplyPreview?.text = MessageReplyHelper.buildPreviewText(ctx, message)
        binding?.tvReplyPreview?.maxLines = 1
        binding?.tvReplyPreview?.ellipsize = android.text.TextUtils.TruncateAt.END
        val photoUrl = MessageReplyHelper.firstPhotoUrl(message)
        val imgReplyThumb = binding?.imgReplyThumb
        if (photoUrl != null && MessageReplyHelper.resolveMessageType(message) != TypeMessage.AUDIO) {
            imgReplyThumb?.isVisible = true
            ctx.loadImg(photoUrl, imgReplyThumb!!)
        } else {
            imgReplyThumb?.isVisible = false
        }
    }

    private fun clearReply() {
        replyingToMessage = null
        binding?.replyPreviewContainer?.isVisible = false
        binding?.imgReplyThumb?.isVisible = false
    }

    private fun scrollToMessage(messageTime: String) {
        val index = chatAdapter?.indexOfMessageTime(messageTime) ?: -1
        if (index < 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.reply_original_not_found),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        val recyclerView = binding?.rcvChat ?: return

        replyHighlightScrollListener?.let { recyclerView.removeOnScrollListener(it) }
        replyHighlightScrollListener = null

        val scrollToken = Any()
        var scrollHighlightToken: Any? = scrollToken

        fun finishScrollAndHighlight() {
            if (scrollHighlightToken !== scrollToken) return
            scrollHighlightToken = null
            replyHighlightScrollListener?.let { recyclerView.removeOnScrollListener(it) }
            replyHighlightScrollListener = null
            nudgeToReplyScrollOffset(recyclerView, index) {
                chatAdapter?.flashReplyHighlight(messageTime)
            }
        }

        val listener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                finishScrollAndHighlight()
            }
        }
        replyHighlightScrollListener = listener
        recyclerView.addOnScrollListener(listener)

        recyclerView.post { smoothScrollToMessageWithOffset(recyclerView, index) }

        // Item đã ở đúng vùng nhìn — smoothScroll có thể không chạy.
        recyclerView.postDelayed({
            if (scrollHighlightToken !== scrollToken) return@postDelayed
            if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                finishScrollAndHighlight()
            }
        }, 700L)
    }

    private fun smoothScrollToMessageWithOffset(recyclerView: RecyclerView, index: Int) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: run {
            recyclerView.smoothScrollToPosition(index)
            return
        }
        val scroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START

            override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
                val topOffsetPx = replyScrollTopOffsetPx(recyclerView, index, view.height)
                return layoutManager.getDecoratedTop(view) - layoutManager.paddingTop - topOffsetPx
            }
        }
        scroller.targetPosition = index
        layoutManager.startSmoothScroll(scroller)
    }

    /** Chỉnh nhẹ vị trí sau smooth scroll để khớp offset (vẫn có animation). */
    private fun nudgeToReplyScrollOffset(
        recyclerView: RecyclerView,
        index: Int,
        onComplete: () -> Unit,
    ) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: run {
            onComplete()
            return
        }
        val targetView = layoutManager.findViewByPosition(index)
        if (targetView == null) {
            layoutManager.scrollToPositionWithOffset(
                index,
                replyScrollTopOffsetPx(recyclerView, index),
            )
            recyclerView.post(onComplete)
            return
        }
        val topOffsetPx = replyScrollTopOffsetPx(recyclerView, index, targetView.height)
        val dy = layoutManager.getDecoratedTop(targetView) - layoutManager.paddingTop - topOffsetPx
        if (kotlin.math.abs(dy) <= 2) {
            onComplete()
            return
        }
        recyclerView.smoothScrollBy(0, dy)
        val tuneListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                rv.removeOnScrollListener(this)
                onComplete()
            }
        }
        recyclerView.addOnScrollListener(tuneListener)
    }

    /**
     * Offset từ mép trên RecyclerView tới item đích.
     * Dùng vùng nhìn thấy trên màn hình (không dùng recyclerView.height vì layout wrap_content).
     */
    private fun replyScrollTopOffsetPx(
        recyclerView: RecyclerView,
        index: Int,
        itemHeightPx: Int? = null,
    ): Int {
        val density = recyclerView.resources.displayMetrics.density
        // ~2 dòng tin dưới header.
        val minOffsetPx = (120 * density).toInt()
        val visibleHeight = visibleChatListHeightPx(recyclerView)
        if (visibleHeight <= 0) return minOffsetPx

        val heightForCenter = itemHeightPx
            ?: chatAdapter?.estimateScrollItemHeightPx(index)
            ?: (64 * density).toInt()
        val centeredOffsetPx = (visibleHeight - heightForCenter) / 2
        return max(minOffsetPx, centeredOffsetPx)
    }

    private fun visibleChatListHeightPx(recyclerView: RecyclerView): Int {
        val visibleRect = Rect()
        if (recyclerView.getGlobalVisibleRect(visibleRect) && visibleRect.height() > 0) {
            return visibleRect.height()
        }
        val density = recyclerView.resources.displayMetrics.density
        val headerHeight = binding?.header?.height ?: (56 * density).toInt()
        val bottomBarHeight = (130 * density).toInt()
        return (screenHeight - headerHeight - bottomBarHeight).coerceAtLeast((200 * density).toInt())
    }

    private fun setupMentionPicker() {
        mentionSuggestionAdapter = MentionSuggestionAdapter { candidate ->
            onMentionCandidateSelected(candidate)
        }
        binding?.rcvMentionSuggestions?.adapter = mentionSuggestionAdapter
    }

    private fun updateMentionPicker(text: CharSequence?) {
        if (conversation?.isGroupThread() != true) {
            hideMentionPicker()
            return
        }
        val cursor = binding?.edtMessage?.selectionStart ?: 0
        val query = MentionHelper.detectActiveMentionQuery(text ?: "", cursor)
        activeMentionQuery = query
        if (query == null) {
            hideMentionPicker()
            return
        }
        val filtered = MentionHelper.filterCandidates(
            query = query.query,
            members = groupMentionMembers,
            allCandidate = allMentionCandidate,
        )
        if (filtered.isEmpty()) {
            hideMentionPicker()
            return
        }
        binding?.mentionPickerContainer?.isVisible = true
        mentionSuggestionAdapter?.submitList(filtered)
    }

    private fun hideMentionPicker() {
        activeMentionQuery = null
        binding?.mentionPickerContainer?.isVisible = false
        mentionSuggestionAdapter?.submitList(emptyList())
    }

    private fun onMentionCandidateSelected(candidate: MentionHelper.MentionCandidate) {
        val query = activeMentionQuery ?: return
        val editable = binding?.edtMessage?.text ?: return
        val insertText = MentionHelper.insertTextForCandidate(candidate)
        val newCursor = MentionHelper.insertMentionToken(editable, query, insertText)
        binding?.edtMessage?.setSelection(newCursor)
        pendingMentions.removeAll { it.userId == candidate.userId }
        pendingMentions.add(MentionHelper.toMessageMention(candidate))
        refreshInputMentionHighlight()
        hideMentionPicker()
    }

    private fun refreshInputMentionHighlight() {
        if (conversation?.isGroupThread() != true) return
        val editable = binding?.edtMessage?.text ?: return
        MentionHelper.applyMentionSpansToEditable(
            requireContext(),
            editable,
            pendingMentions,
        )
    }

    override fun onStop() {
        super.onStop()
        conversation?.let { viewModel?.updateTyping(it, false) }
    }

    override fun onDestroy() {
        super.onDestroy()
        conversation?.let { viewModel?.updateTyping(it, false) }
    }
}