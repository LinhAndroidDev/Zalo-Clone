package com.example.messageapp.fragment

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.adapter.DiaryNotificationAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.bottom_sheet.BottomSheetDiaryComments
import com.example.messageapp.databinding.FragmentDiaryNotificationBinding
import com.example.messageapp.viewmodel.DiaryNotificationFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiaryNotificationFragment :
    BaseFragment<FragmentDiaryNotificationBinding, DiaryNotificationFragmentViewModel>() {

    override val layoutResId: Int = R.layout.fragment_diary_notification

    private val adapter by lazy {
        DiaryNotificationAdapter().apply {
            onItemClick = { item ->
                if (item.postId.isNotBlank()) {
                    findNavController().popBackStack()
                    BottomSheetDiaryComments.newInstance(item.postId)
                        .show(requireActivity().supportFragmentManager, "BottomSheetDiaryComments")
                }
            }
        }
    }

    override fun initView() {
        super.initView()
        binding?.rvNotifications?.adapter = adapter
        viewModel?.startObserving()
    }

    override fun onResume() {
        super.onResume()
        viewModel?.markAllAsRead()
    }

    override fun bindData() {
        super.bindData()
        lifecycleScope.launch {
            viewModel?.notifications?.collect { list ->
                adapter.submitList(list)
                binding?.tvEmpty?.isVisible = list.isEmpty()
                binding?.rvNotifications?.isVisible = list.isNotEmpty()
            }
        }
    }
}
