package com.example.messageapp.bottom_sheet

import android.R.attr
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.databinding.BottomSheetSettingViewDiaryBinding
import com.example.messageapp.library.wheel_picker.WheelPicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class BottomSheetSettingViewDiary : BottomSheetDialogFragment(), WheelPicker.OnItemSelectedListener {
    val binding by lazy { BottomSheetSettingViewDiaryBinding.inflate(LayoutInflater.from(context)) }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAllDiary.setOnClickListener {
            unCheckAll()
            binding.radioAllDiary.setChecked()
        }

        binding.btnLast7Days.setOnClickListener {
            unCheckAll()
            binding.radioLast7Days.setChecked()
        }

        binding.btn1MonthAgo.setOnClickListener {
            unCheckAll()
            binding.radio1MonthAgo.setChecked()
        }

        binding.btn6MonthAgo.setOnClickListener {
            unCheckAll()
            binding.radio6MonthAgo.setChecked()
        }

        binding.btnCustom.setOnClickListener {
            unCheckAll()
            binding.radioCustom.setChecked()
            binding.viewSelectDate.isVisible = true
            val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right)
            binding.viewSelectDate.startAnimation(fadeIn)
            binding.viewSettingDiary.visibility = View.INVISIBLE
            binding.icBack.isVisible = true
        }

        binding.icBack.setOnClickListener {
            binding.viewSelectDate.isVisible = false
            val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_right)
            binding.viewSelectDate.startAnimation(fadeOut)
            binding.viewSettingDiary.visibility = View.VISIBLE
            binding.icBack.isVisible = false
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun unCheckAll() {
        binding.radioAllDiary.setUnChecked()
        binding.radioLast7Days.setUnChecked()
        binding.radio1MonthAgo.setUnChecked()
        binding.radio6MonthAgo.setUnChecked()
        binding.radioCustom.setUnChecked()
    }

    override fun onItemSelected(picker: WheelPicker?, data: Any?, position: Int) {
        var text = ""
        when (picker!!.id) {
            R.id.main_wheel_left -> text = "Left:"
            R.id.main_wheel_center -> text = "Center:"
            R.id.main_wheel_right -> text = "Right:"
        }
        Toast.makeText(requireActivity(), text + attr.data.toString(), Toast.LENGTH_SHORT).show()
    }
}