package com.example.messageapp.bottom_sheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.messageapp.R
import com.example.messageapp.adapter.StickerAdapter
import com.example.messageapp.databinding.BottomSheetStickerBinding
import com.example.messageapp.model.Sticker
import com.example.messageapp.viewmodel.BottomSheetStickerViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class BottomSheetSticker : BottomSheetDialogFragment() {
    private var binding: BottomSheetStickerBinding? = null
    private lateinit var viewModel: BottomSheetStickerViewModel
    private var isTabClick = false

    override fun onStart() {
        super.onStart()
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)

            // Lấy chiều cao màn hình
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels

            // Giới hạn chiều cao max = 50% màn hình
            val maxHeight = (screenHeight * 0.5).toInt()
            it.layoutParams.height = maxHeight
            it.requestLayout()

            // Cho phép expand nhưng không vượt quá 50%
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetStickerBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[BottomSheetStickerViewModel::class.java]
        viewModel.initData()

        bindData()
        initTab()
    }

    private fun initTab() {
        binding?.tabLayout?.apply {
            addTab(newTab().setIcon(Sticker.HELLO.icon))
            addTab(newTab().setIcon(Sticker.LOVE.icon))
            addTab(newTab().setIcon(Sticker.CONGRATULATION.icon))
            addTab(newTab().setIcon(Sticker.ANGRY.icon))
            addTab(newTab().setIcon(Sticker.SAD.icon))
            addTab(newTab().setIcon(Sticker.SORRY.icon))
        }

        binding?.tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isTabClick = true
                val targetView = binding?.containerSticker?.getChildAt(tab?.position ?: 0)
                targetView?.scrollTo()
                // Sau khi cuộn xong thì reset flag
                binding?.scrollView?.postDelayed({
                    isTabClick = false
                    binding?.tabLayout?.getTabAt(tab?.position ?: 0)?.select()
                }, 300) // delay ~ thời gian smoothScroll
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding?.scrollView?.post {
            binding?.scrollView?.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                if (!isTabClick) {
                    var selectedIndex = 0
                    var minDiff = Int.MAX_VALUE

                    val count = binding?.containerSticker?.childCount ?: 0
                    for (i in 0 until count) {
                        val child = binding?.containerSticker?.getChildAt(i)
                        val childTop = child?.top ?: 0
                        val diff = kotlin.math.abs(scrollY - childTop)

                        if (diff < minDiff) {
                            minDiff = diff
                            selectedIndex = i
                        }
                    }

                    if (binding?.tabLayout?.selectedTabPosition != selectedIndex) {
                        binding?.tabLayout?.getTabAt(selectedIndex)?.select()
                    }
                }
            }
        }
    }

    private fun bindData() {
        lifecycleScope.launch {
            viewModel.stickerHellos.collect { hellos ->
                Log.e("StickerList", "bindData: $hellos")
                hellos?.let { setStickerList(Sticker.HELLO, it) }
            }
        }

        lifecycleScope.launch {
            viewModel.stickerLoves.collect { loves ->
                loves?.let { setStickerList(Sticker.LOVE, it) }
            }
        }

        lifecycleScope.launch {
            viewModel.stickerCongratulations.collect { congratulations ->
                congratulations?.let { setStickerList(Sticker.CONGRATULATION, it) }
            }
        }

        lifecycleScope.launch {
            viewModel.stickerAngries.collect { angries ->
                angries?.let { setStickerList(Sticker.ANGRY, it) }
            }
        }

        lifecycleScope.launch {
            viewModel.stickerSads.collect { sads ->
                sads?.let { setStickerList(Sticker.SAD, it) }
            }
        }

        lifecycleScope.launch {
            viewModel.stickerSorries.collect { sorries ->
                sorries?.let { setStickerList(Sticker.SORRY, it) }
            }
        }
    }

    private fun setStickerList(sticker: Sticker, stickers: MutableList<String>) {
        val stickerListView = View.inflate(requireContext(), R.layout.sticker_list_view, null)
        val titleSticker = stickerListView.findViewById<TextView>(R.id.txtTitleSticker)
        val stickerList = stickerListView.findViewById<RecyclerView>(R.id.rcvSticker)

        titleSticker.text = sticker.value
        val stickerAdapter = StickerAdapter()
        stickerAdapter.onClickItem = {

        }
        stickerAdapter.items = stickers
        stickerList.adapter = stickerAdapter

        when (sticker) {
            Sticker.HELLO -> addViewToStickerContainer(stickerListView, Sticker.HELLO.index)
            Sticker.LOVE -> addViewToStickerContainer(stickerListView, Sticker.LOVE.index)
            Sticker.CONGRATULATION -> addViewToStickerContainer(stickerListView, Sticker.CONGRATULATION.index)
            Sticker.ANGRY -> addViewToStickerContainer(stickerListView, Sticker.ANGRY.index)
            Sticker.SAD -> addViewToStickerContainer(stickerListView, Sticker.SAD.index)
            Sticker.SORRY -> addViewToStickerContainer(stickerListView, Sticker.SORRY.index)
        }
    }

    private fun addViewToStickerContainer(view: View, position: Int) {
        if (binding?.containerSticker?.childCount == position) {
            binding?.containerSticker?.addView(view)
        } else {
            binding?.containerSticker?.removeViewAt(position)
            binding?.containerSticker?.addView(view, position)
        }
    }

    private fun View.scrollTo() {
        val y = this.y.toInt()
        binding?.scrollView?.post {
            binding?.scrollView?.smoothScrollTo(0, y)
        }
    }
}