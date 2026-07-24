package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messageapp.adapter.StoryFriendPickerAdapter
import com.example.messageapp.databinding.BottomSheetStoryCustomFriendsBinding
import com.example.messageapp.domain.usecase.social.GetFriendsUseCase
import com.example.messageapp.mapper.SocialUiMapper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BottomSheetStoryCustomFriends : BottomSheetDialogFragment() {

    @Inject lateinit var getFriendsUseCase: GetFriendsUseCase

    private var _binding: BottomSheetStoryCustomFriendsBinding? = null
    private val binding get() = _binding!!
    private val adapter = StoryFriendPickerAdapter()
    private val selectedIds = linkedSetOf<String>()

    var onFriendsSelected: ((List<String>) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetStoryCustomFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedIds.clear()
        selectedIds.addAll(arguments?.getStringArrayList(ARG_SELECTED).orEmpty())

        binding.rcvFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvFriends.adapter = adapter
        adapter.onToggle = { friendId, checked ->
            if (checked) selectedIds.add(friendId) else selectedIds.remove(friendId)
        }

        binding.btnDone.setOnClickListener {
            onFriendsSelected?.invoke(selectedIds.toList())
            dismiss()
        }

        lifecycleScope.launch {
            getFriendsUseCase(
                onSuccess = { friends ->
                    val uiFriends = SocialUiMapper.toUiFriends(friends)
                    adapter.submit(uiFriends, selectedIds)
                },
                onFailure = {},
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SELECTED = "selected"

        fun newInstance(selectedIds: List<String>) = BottomSheetStoryCustomFriends().apply {
            arguments = bundleOf(ARG_SELECTED to ArrayList(selectedIds))
        }
    }
}
