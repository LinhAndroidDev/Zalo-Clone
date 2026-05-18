package com.example.messageapp.fragment

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messageapp.PersonalActivity
import com.example.messageapp.R
import com.example.messageapp.adapter.DiaryPostAdapter
import com.example.messageapp.model.DiaryPost
import com.example.messageapp.bottom_sheet.BottomSheetDiaryComments
import com.example.messageapp.dialog.StatusImagePreviewDialog
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentDiaryBinding
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FireBaseInstance
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.utils.SharePreferenceRepository
import com.example.messageapp.viewmodel.DiaryFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TypeNews {
    Camera, Video, Edit
}

@AndroidEntryPoint
class DiaryFragment : BaseFragment<FragmentDiaryBinding, DiaryFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_diary

    @Inject
    lateinit var shared: SharePreferenceRepository

    private val diaryPostAdapter by lazy { DiaryPostAdapter() }

    override fun initView() {
        super.initView()
        FirebaseAnalyticsInstance.logDiaryScreen()

        binding?.rcvDiaryFeed?.layoutManager = LinearLayoutManager(requireContext())
        binding?.rcvDiaryFeed?.adapter = diaryPostAdapter
        diaryPostAdapter.onOpenImagePreview = { uris, index ->
            StatusImagePreviewDialog.newInstance(uris, index)
                .show(childFragmentManager, "StatusImagePreviewDialog")
        }
        diaryPostAdapter.onToggleLike = { post -> viewModel?.toggleDiaryPostLike(post) }
        diaryPostAdapter.onOpenComments = { post ->
            BottomSheetDiaryComments.newInstance(post.id)
                .show(childFragmentManager, "BottomSheetDiaryComments")
        }
        diaryPostAdapter.currentUserId = shared.getAuth()
        diaryPostAdapter.onEditPost = { post ->
            findNavController().navigate(
                R.id.action_diaryFragment_to_statusFragment,
                bundleOf("postId" to post.id)
            )
        }
        diaryPostAdapter.onDeletePost = { post -> confirmDeletePost(post) }
        diaryPostAdapter.onOpenAuthorProfile = { authorId ->
            if (authorId.isNotBlank()) {
                val intent = Intent(requireActivity(), PersonalActivity::class.java)
                if (authorId != shared.getAuth()) {
                    intent.putExtra(PersonalActivity.FRIEND_ID_KEY, authorId)
                }
                startActivity(intent)
            }
        }

        viewModel?.getInfoUser()
        viewModel?.startDiaryFeed()
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.user?.collect { user ->
                binding?.let { binding ->
                    activity?.loadImg(user?.avatar.toString(), binding.avatarUser)
                    activity?.loadImg(user?.avatar.toString(), binding.imgCreateNews)
                }
            }
        }

        AnimatorUtils.scaleNews(binding?.iconNews, TypeNews.Camera)
    }

    override fun bindData() {
        super.bindData()
        lifecycleScope.launch {
            viewModel?.diaryPosts?.collect { posts ->
                diaryPostAdapter.submitList(posts)
                binding?.tvFeedEmpty?.isVisible = posts.isEmpty()
            }
        }
        // Lỗi: BaseFragment.initView() đã collect errorState + Toast — không collect lại ở đây (tránh toast trùng/spam).
    }

    override fun onClickView() {
        super.onClickView()

        binding?.avatarUser?.setOnClickListener {
            val intent = Intent(requireActivity(), PersonalActivity::class.java)
            startActivity(intent)
        }

        binding?.addStatus?.setOnClickListener {
            findNavController().navigate(
                R.id.action_diaryFragment_to_statusFragment,
                bundleOf("postId" to "")
            )
        }
    }

    private fun confirmDeletePost(post: DiaryPost) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.diary_post_delete_title)
            .setMessage(R.string.diary_post_delete_message)
            .setNegativeButton(R.string.diary_post_delete_cancel, null)
            .setPositiveButton(R.string.diary_post_delete_confirm) { _, _ ->
                val uid = shared.getAuth()
                FireBaseInstance.deleteDiaryPost(
                    postId = post.id,
                    editorUserId = uid,
                    success = {},
                    failure = { msg -> viewModel?.showError(msg) }
                )
            }
            .show()
    }
}
