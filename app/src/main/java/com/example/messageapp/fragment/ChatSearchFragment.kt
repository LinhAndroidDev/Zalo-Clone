package com.example.messageapp.fragment

import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.messageapp.R
import com.example.messageapp.adapter.ChatSearchResultAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentChatSearchBinding
import com.example.messageapp.utils.hideKeyboard
import com.example.messageapp.utils.showKeyboard
import com.example.messageapp.viewmodel.ChatSearchFragmentViewModel
import com.example.messageapp.viewmodel.ChatSearchUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatSearchFragment : BaseFragment<FragmentChatSearchBinding, ChatSearchFragmentViewModel>() {

    override val layoutResId: Int = R.layout.fragment_chat_search

    private val args by navArgs<ChatSearchFragmentArgs>()
    private val resultAdapter = ChatSearchResultAdapter()

    override fun initView() {
        super.initView()

        viewModel?.init(args.conversation)

        binding?.rcvChatSearchResults?.adapter = resultAdapter
        resultAdapter.onItemClick = { item ->
            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set(SCROLL_TO_MESSAGE_TIME_KEY, item.messageTime)
            findNavController().popBackStack()
        }
        resultAdapter.onMissingAvatar = { userId, onLoaded ->
            viewModel?.loadMissingSender(userId) { onLoaded() }
        }
        resultAdapter.resolveSenderAvatar = { userId ->
            viewModel?.senderInfo(userId)?.second.orEmpty()
        }

        binding?.root?.post {
            binding?.edtChatSearchQuery?.showKeyboard()
        }

        binding?.btnSubmitSearch?.setOnClickListener { submitSearch() }
        binding?.edtChatSearchQuery?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch()
                true
            } else {
                false
            }
        }
    }

    override fun bindData() {
        super.bindData()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel?.uiState?.collect { state ->
                renderState(state)
            }
        }
    }

    private fun submitSearch() {
        val query = binding?.edtChatSearchQuery?.text?.toString().orEmpty()
        viewModel?.performSearch(query)
        hideKeyboard()
    }

    private fun renderState(state: ChatSearchUiState) {
        if (!state.hasSearched) {
            binding?.tvEmptyState?.isVisible = true
            binding?.tvEmptyState?.text = getString(R.string.chat_search_empty_prompt)
            binding?.tvResultCount?.isVisible = false
            binding?.rcvChatSearchResults?.isVisible = false
            resultAdapter.updateResults(emptyList())
            return
        }

        if (state.results.isEmpty()) {
            binding?.tvEmptyState?.isVisible = true
            binding?.tvEmptyState?.text = getString(R.string.chat_search_no_results)
            binding?.tvResultCount?.isVisible = false
            binding?.rcvChatSearchResults?.isVisible = false
            resultAdapter.updateResults(emptyList())
            return
        }

        binding?.tvEmptyState?.isVisible = false
        binding?.tvResultCount?.isVisible = true
        binding?.tvResultCount?.text = getString(R.string.chat_search_result_count, state.results.size)
        binding?.rcvChatSearchResults?.isVisible = true

        val enrichedResults = state.results.map { item ->
            if (item.senderAvatar.isNotBlank()) {
                item
            } else {
                val (name, avatar) = viewModel?.senderInfo(item.senderId) ?: (item.senderName to "")
                item.copy(
                    senderName = name.ifBlank { item.senderName },
                    senderAvatar = avatar,
                )
            }
        }
        resultAdapter.updateResults(enrichedResults)
    }

    companion object {
        const val SCROLL_TO_MESSAGE_TIME_KEY = "scrollToMessageTime"
    }
}
