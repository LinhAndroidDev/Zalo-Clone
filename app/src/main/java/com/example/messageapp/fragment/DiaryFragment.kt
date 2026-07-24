package com.example.messageapp.fragment

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messageapp.MainActivity
import com.example.messageapp.PersonalActivity
import com.example.messageapp.R
import com.example.messageapp.adapter.DiaryPostAdapter
import com.example.messageapp.adapter.StoryRingAdapter
import com.example.messageapp.model.StoryRingItem
import com.example.messageapp.model.StoryViewerCache
import com.example.messageapp.model.DiaryNavigationTarget
import com.example.messageapp.model.DiaryPost
import com.example.messageapp.bottom_sheet.BottomSheetDiaryComments
import com.example.messageapp.dialog.StatusImagePreviewDialog
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentDiaryBinding
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.viewmodel.DiaryFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class DiaryFragment : BaseFragment<FragmentDiaryBinding, DiaryFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_diary

    private val diaryPostAdapter by lazy { DiaryPostAdapter() }
    private val storyRingAdapter by lazy { StoryRingAdapter() }

    override fun initView() {
        super.initView()
        FirebaseAnalyticsInstance.logDiaryScreen()

        binding?.rcvDiaryFeed?.layoutManager = LinearLayoutManager(requireContext())
        binding?.rcvDiaryFeed?.adapter = diaryPostAdapter

        binding?.rcvStoryRings?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.rcvStoryRings?.adapter = storyRingAdapter
        storyRingAdapter.onMyStoryClick = { ring ->
            if (ring == null) {
                navigateToCreateStory()
            } else {
                openStoryViewer(ring.authorId)
            }
        }
        storyRingAdapter.onFriendStoryClick = { ring ->
            openStoryViewer(ring.authorId)
        }
        diaryPostAdapter.onOpenImagePreview = { uris, index ->
            StatusImagePreviewDialog.newInstance(uris, index)
                .show(childFragmentManager, "StatusImagePreviewDialog")
        }
        diaryPostAdapter.onToggleLike = { post -> viewModel?.toggleDiaryPostLike(post) }
        diaryPostAdapter.onOpenComments = { post ->
            BottomSheetDiaryComments.newInstance(post.id)
                .show(childFragmentManager, "BottomSheetDiaryComments")
        }
        diaryPostAdapter.currentUserId = viewModel?.currentUserId().orEmpty()
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
                if (authorId != viewModel?.currentUserId()) {
                    intent.putExtra(PersonalActivity.FRIEND_ID_KEY, authorId)
                }
                startActivity(intent)
            }
        }

        viewModel?.getInfoUser()
        viewModel?.startDiaryFeed()
        viewModel?.startStoryRings()
        viewModel?.startNotificationBadge()

        binding?.header?.onAddStoryClick = { navigateToCreateStory() }
        binding?.header?.onDiaryNotificationClick = {
            findNavController().navigate(R.id.action_diaryFragment_to_diaryNotificationFragment)
        }

        diaryPostAdapter.onSetReaction = { post, type ->
            viewModel?.setDiaryPostReaction(post, type)
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.user?.collect { user ->
                binding?.let { binding ->
                    activity?.loadImg(user?.avatar.toString(), binding.avatarUser)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.storyRings?.collect { rings ->
                storyRingAdapter.updateDiff(rings)
            }
        }
    }

    private fun navigateToCreateStory() {
        findNavController().navigate(R.id.action_diaryFragment_to_createStoryFragment)
    }

    private fun openStoryViewer(startAuthorId: String) {
        viewModel?.storyRings?.value?.let { StoryViewerCache.update(it) }
        findNavController().navigate(
            R.id.action_diaryFragment_to_storyViewerFragment,
            bundleOf(
                "startAuthorId" to startAuthorId,
                "ringAuthorIds" to (viewModel?.ringAuthorIds() ?: arrayOf(startAuthorId)),
            ),
        )
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.consumePendingDiaryTarget()?.let { target ->
            handleDiaryNavigationTarget(target)
        }
    }

    private fun handleDiaryNavigationTarget(target: DiaryNavigationTarget) {
        lifecycleScope.launch {
            val posts = withTimeoutOrNull(5_000L) {
                viewModel?.diaryPosts?.first { list -> list.any { it.id == target.postId } }
            }
            val index = posts?.indexOfFirst { it.id == target.postId } ?: -1
            if (index >= 0) {
                binding?.rcvDiaryFeed?.smoothScrollToPosition(index)
                delay(300L)
            }
            openCommentsSheet(target)
        }
    }

    private fun openCommentsSheet(target: DiaryNavigationTarget) {
        BottomSheetDiaryComments.newInstance(
            postId = target.postId,
            commentId = target.commentId,
            replyId = target.replyId,
        ).show(childFragmentManager, "BottomSheetDiaryComments")
    }

    override fun bindData() {
        super.bindData()
        lifecycleScope.launch {
            viewModel?.diaryPosts?.collect { posts ->
                diaryPostAdapter.submitList(posts)
                binding?.tvFeedEmpty?.isVisible = posts.isEmpty()
            }
        }
        lifecycleScope.launch {
            viewModel?.unreadNotificationCount?.collect { count ->
                binding?.header?.setNotificationBadge(count)
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
                viewModel?.deleteDiaryPost(post.id) {}
            }
            .show()
    }
}
