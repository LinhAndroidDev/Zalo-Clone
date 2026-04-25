package com.example.messageapp.bottom_sheet

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import com.example.messageapp.adapter.GalleryAdapter
import com.example.messageapp.databinding.BottomSheetGalleryBinding
import com.example.messageapp.model.GalleryItem
import com.example.messageapp.utils.GalleryUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GalleryBottomSheet(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {

    private var bottomSheetDialog: BottomSheetDialog? = null
    private var onItemSelected: ((List<GalleryItem>) -> Unit)? = null

    fun show(onItemSelected: ((List<GalleryItem>) -> Unit)? = null) {
        this.onItemSelected = onItemSelected
        
        bottomSheetDialog = BottomSheetDialog(context)
        val binding = BottomSheetGalleryBinding.inflate(
            bottomSheetDialog!!.layoutInflater
        )
        bottomSheetDialog?.setContentView(binding.root)

        // Configure BottomSheetDialog behavior
        bottomSheetDialog?.behavior?.apply {
            isDraggable = true
            isHideable = true
            expandedOffset = 0
            halfExpandedRatio = 0.8f
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        setupRecyclerView(binding)
        loadGalleryItems(binding)
        
        bottomSheetDialog?.show()
    }

    private fun setupRecyclerView(binding: BottomSheetGalleryBinding) {
        val adapter = GalleryAdapter(
            context = context,
            onItemChecked = { item, isChecked ->
                // Handle checkbox state change
                Snackbar.make(
                    binding.root,
                    if (isChecked) "Đã chọn ${item.name}" else "Đã bỏ chọn ${item.name}",
                    Snackbar.LENGTH_SHORT
                ).show()
                
                // Notify selected items
//                onItemSelected?.invoke(adapter.getSelectedItems())
            }
        )

        binding.rvGallery.apply {
            layoutManager = GridLayoutManager(context, 3)
            this.adapter = adapter
            // Enable nested scrolling
            isNestedScrollingEnabled = true
        }
    }

    private fun loadGalleryItems(binding: BottomSheetGalleryBinding) {
        coroutineScope.launch {
            try {
                val items = GalleryUtils.getGalleryItems(context)
                val adapter = binding.rvGallery.adapter as? GalleryAdapter
                adapter?.updateItems(items)
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Không thể tải thư viện: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    fun dismiss() {
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
    }

    fun isShowing(): Boolean = bottomSheetDialog?.isShowing == true
}
