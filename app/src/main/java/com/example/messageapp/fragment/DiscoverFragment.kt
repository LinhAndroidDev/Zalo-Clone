package com.example.messageapp.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentDiscoverBinding
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.viewmodel.DiscoverFragmentViewModel
import com.example.messageapp.bottom_sheet.GalleryBottomSheet
import com.google.android.material.snackbar.Snackbar

class DiscoverFragment : BaseFragment<FragmentDiscoverBinding, DiscoverFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_discover

    private var galleryBottomSheet: GalleryBottomSheet? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showGalleryBottomSheet()
        } else {
            Snackbar.make(
                binding?.root ?: return@registerForActivityResult,
                "Cần quyền truy cập thư viện để xem ảnh và video",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun initView() {
        super.initView()
        // log event: screen_discover
        FirebaseAnalyticsInstance.logDiscoverScreen()

        binding?.imgAd?.let {
            activity?.loadImg(
                "https://media.istockphoto.com/id/1487894858/vi/anh/bi%E1%BB%83u-%C4%91%E1%BB%93-n%E1%BA%BFn-v%C3%A0-d%E1%BB%AF-li%E1%BB%87u-c%E1%BB%A7a-th%E1%BB%8B-tr%C6%B0%E1%BB%9Dng-t%C3%A0i-ch%C3%ADnh.jpg?s=612x612&w=0&k=20&c=QEOshW25DopLeCe4bEQXXLbQB2pbIiy4bm0T27UJR4g=",
                it
            )
        }

        binding?.viewParent?.let { AnimatorUtils.fadeInViewItem(requireActivity(), it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding?.btnGallery?.setOnClickListener {
            checkPermissionAndShowGallery()
        }
    }

    private fun checkPermissionAndShowGallery() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                showGalleryBottomSheet()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                Snackbar.make(
                    binding?.root ?: return,
                    "Cần quyền truy cập thư viện để xem ảnh và video",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun showGalleryBottomSheet() {
        galleryBottomSheet = GalleryBottomSheet(
            context = requireContext(),
            coroutineScope = lifecycleScope
        )
        
        galleryBottomSheet?.show { selectedItems ->
            // Handle selected items
            Snackbar.make(
                binding?.root ?: return@show,
                "Đã chọn ${selectedItems.size} items",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        galleryBottomSheet?.dismiss()
        galleryBottomSheet = null
    }
}