package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.adapter.StoryMusicAdapter
import com.example.messageapp.databinding.BottomSheetStoryMusicBinding
import com.example.messageapp.domain.usecase.story.LoadJamendoTracksUseCase
import com.example.messageapp.mapper.StoryUiMapper
import com.example.messageapp.model.MusicTrackItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BottomSheetStoryMusic : BottomSheetDialogFragment() {

    @Inject lateinit var loadJamendoTracksUseCase: LoadJamendoTracksUseCase

    private var _binding: BottomSheetStoryMusicBinding? = null
    private val binding get() = _binding!!
    private val adapter = StoryMusicAdapter()
    private var nextOffset = 0
    private var isLoading = false
    private var hasMore = true

    var onTrackSelected: ((MusicTrackItem?) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetStoryMusicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet = (dialog as? BottomSheetDialog)
            ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return
        val topGapPx = (56 * resources.displayMetrics.density).toInt()
        bottomSheet.layoutParams.height = resources.displayMetrics.heightPixels - topGapPx
        bottomSheet.requestLayout()
        BottomSheetBehavior.from(bottomSheet).apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rcvMusic.layoutManager = layoutManager
        binding.rcvMusic.adapter = adapter
        adapter.onTrackClick = { track ->
            onTrackSelected?.invoke(track)
            dismiss()
        }
        binding.rcvMusic.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || isLoading || !hasMore) return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (lastVisible >= adapter.itemCount - 3) {
                    loadTracks(append = true)
                }
            }
        })
        loadTracks(append = false)
    }

    private fun loadTracks(append: Boolean) {
        if (isLoading || (append && !hasMore)) return
        if (!append) {
            nextOffset = 0
            hasMore = true
        }
        isLoading = true
        lifecycleScope.launch {
            try {
                loadJamendoTracksUseCase(offset = if (append) nextOffset else 0)
                    .onSuccess { page ->
                        hasMore = page.nextOffset != null && page.tracks.isNotEmpty()
                        nextOffset = page.nextOffset ?: (adapter.itemCount + page.tracks.size)
                        val uiTracks = StoryUiMapper.toUiTracks(page.tracks)
                        if (append) adapter.appendTracks(uiTracks) else adapter.updateDiff(uiTracks)
                    }
            } finally {
                isLoading = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = BottomSheetStoryMusic()
    }
}
