package com.example.messageapp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.bottom_sheet.BottomSheetSelectImage
import com.example.messageapp.bottom_sheet.BottomSheetSettingViewDiary
import com.example.messageapp.databinding.ActivityPersonalBinding
import com.example.messageapp.model.User
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.setOnSingleClickListener
import com.example.messageapp.viewmodel.PersonalActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonalActivity : AppCompatActivity() {
    private val binding by lazy { ActivityPersonalBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<PersonalActivityViewModel>()
    private var updateAvatar = true
    private var currentAvatarUrl: String = ""
    private var currentCoverUrl: String = ""

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 2
        const val FRIEND_ID_KEY = "FRIEND_ID_KEY"
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            viewModel.uploadPhoto(this, uri, updateAvatar)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
        onClickView()
    }

    private fun onClickView() {
        binding.back.setOnClickListener { onBackPressed() }
        binding.avatarUser.setOnSingleClickListener {
            if (viewModel.isInfoUser.value) {
                showDialogSelectPhoto(true, currentAvatarUrl)
            } else {
                startPreviewAvatarWithTransition(
                    currentAvatarUrl,
                    binding.avatarUser,
                    PreviewAvatarActivity.TRANSITION_AVATAR
                )
            }
        }

        binding.imgCover.setOnSingleClickListener {
            if (viewModel.isInfoUser.value) {
                showDialogSelectPhoto(false, currentCoverUrl)
            } else {
                startPreviewAvatarWithTransition(
                    currentCoverUrl,
                    binding.imgCover,
                    PreviewAvatarActivity.TRANSITION_COVER
                )
            }
        }

        binding.btnViewDiary.setOnSingleClickListener {
            val bottomSheet = BottomSheetSettingViewDiary()
            bottomSheet.show(supportFragmentManager, "")
        }
    }

    private fun startPreviewAvatarWithTransition(imageUrl: String, sharedView: View, transitionName: String) {
        if (imageUrl.isBlank()) return
        sharedView.transitionName = transitionName
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            sharedView,
            transitionName
        )
        startActivity(
            PreviewAvatarActivity.createIntent(this, imageUrl, transitionName),
            options.toBundle()
        )
    }

    private fun showDialogSelectPhoto(isAvatar: Boolean, image: String) {
        updateAvatar = isAvatar
        val bottomSheetSelectImage = BottomSheetSelectImage()
        val bundle = Bundle()
        bundle.putBoolean(BottomSheetSelectImage.BOTTOM_SHEET_AVATAR, isAvatar)
        bottomSheetSelectImage.arguments = bundle
        bottomSheetSelectImage.show(supportFragmentManager, "")
        bottomSheetSelectImage.seeImage = {
            if (image.isNotBlank()) {
                if (isAvatar) {
                    startPreviewAvatarWithTransition(
                        image,
                        binding.avatarUser,
                        PreviewAvatarActivity.TRANSITION_AVATAR
                    )
                } else {
                    startPreviewAvatarWithTransition(
                        image,
                        binding.imgCover,
                        PreviewAvatarActivity.TRANSITION_COVER
                    )
                }
            }
        }
        bottomSheetSelectImage.takeNewPhoto = {
            dispatchTakePictureIntent()
        }
        bottomSheetSelectImage.selectPhotoOnDevice = {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun handleDataUser(user: User) {
        binding.nameUser.text = user.name
        binding.txtWhatHappy.text = getString(R.string.what_happy_today, user.name)
        currentAvatarUrl = user.avatar.toString()
        currentCoverUrl = user.imageCover.toString()
        loadImg(user.avatar.toString(), binding.avatarUser)
        loadImg(user.imageCover.toString(), binding.imgCover, imgDefault = R.drawable.bg_grey_horizontal)
    }

    private fun initView() {
        setUpFullScreen()

        val arg = intent.getStringExtra(FRIEND_ID_KEY)
        viewModel.getInfoUser(arg)
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.user.collect { user ->
                user?.let {
                    handleDataUser(user)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.isInfoUser.collect { isInfoUser ->
                if (isInfoUser) {
                    showViewUser()
                } else {
                    showViewFriend()
                }

            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }

        })
    }

    private fun showViewFriend() {
        binding.btnViewDiary.isVisible = false
        binding.viewUpdateProfile.isVisible = false
        binding.layoutInfoUser.isVisible = false
        binding.txtNoteFriend.isVisible = true
    }

    private fun showViewUser() {
        binding.btnViewDiary.isVisible = true
        binding.viewUpdateProfile.isVisible = true
        binding.layoutInfoUser.isVisible = true
        binding.txtNoteFriend.isVisible = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                viewModel.uploadPhoto(this, uri, updateAvatar)
            }
        }
    }

    private fun setUpFullScreen() {
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT
    }

}