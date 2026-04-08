package com.example.messageapp.dialog

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.example.messageapp.adapter.StatusPreviewPagerAdapter
import com.example.messageapp.databinding.DialogStatusImagePreviewBinding

class StatusImagePreviewDialog : DialogFragment() {
    private var binding: DialogStatusImagePreviewBinding? = null

    companion object {
        private const val KEY_URIS = "key_uris"
        private const val KEY_INDEX = "key_index"

        fun newInstance(uris: List<Uri>, selectedIndex: Int): StatusImagePreviewDialog {
            val dialog = StatusImagePreviewDialog()
            dialog.arguments = bundleOf(
                KEY_URIS to uris.map { it.toString() }.toTypedArray(),
                KEY_INDEX to selectedIndex
            )
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogStatusImagePreviewBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uriStrings = arguments?.getStringArray(KEY_URIS)?.toList().orEmpty()
        val uris = uriStrings.map { Uri.parse(it) }
        val startIndex = arguments?.getInt(KEY_INDEX, 0) ?: 0

        if (uris.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }

        binding?.viewPagerPreview?.adapter = StatusPreviewPagerAdapter(uris)
        binding?.viewPagerPreview?.setCurrentItem(startIndex.coerceIn(0, uris.lastIndex), false)
        updateIndex(startIndex, uris.size)

        binding?.viewPagerPreview?.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndex(position, uris.size)
            }
        })

        binding?.btnClose?.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun updateIndex(position: Int, total: Int) {
        binding?.txtIndex?.text = "${position + 1}/$total"
    }
}

