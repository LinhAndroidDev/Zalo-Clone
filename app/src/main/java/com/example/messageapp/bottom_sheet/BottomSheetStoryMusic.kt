package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messageapp.adapter.StoryMusicAdapter
import com.example.messageapp.databinding.BottomSheetStoryMusicBinding
import com.example.messageapp.domain.usecase.story.LoadJamendoTracksUseCase
import com.example.messageapp.mapper.StoryUiMapper
import com.example.messageapp.model.MusicTrackItem
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

    var onTrackSelected: ((MusicTrackItem?) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetStoryMusicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rcvMusic.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvMusic.adapter = adapter
        adapter.onTrackClick = { track ->
            onTrackSelected?.invoke(track)
            dismiss()
        }
        binding.btnLoadMore.setOnClickListener { loadTracks(append = true) }
        binding.btnRemoveMusic.setOnClickListener {
            onTrackSelected?.invoke(null)
            dismiss()
        }
        loadTracks(append = false)
    }

    private fun loadTracks(append: Boolean) {
        lifecycleScope.launch {
            loadJamendoTracksUseCase(offset = if (append) nextOffset else 0)
                .onSuccess { page ->
                    nextOffset = page.nextOffset ?: (adapter.itemCount + page.tracks.size)
                    val uiTracks = StoryUiMapper.toUiTracks(page.tracks)
                    if (append) adapter.appendTracks(uiTracks) else adapter.updateDiff(uiTracks)
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
