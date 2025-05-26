package com.example.messageapp.bottom_sheet

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.messageapp.R
import com.example.messageapp.databinding.BottomSheetRecordBinding
import com.example.messageapp.utils.AudioRecorderManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetRecord : BottomSheetDialogFragment() {
    private var binding: BottomSheetRecordBinding? = null
    private var recording = false
    private var audioRecorder: AudioRecorderManager? = null
    var onRecordListener: ((String) -> Unit)? = null
    private var recordedFilePath: String? = null
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startRecord()
            } else {
                Toast.makeText(requireActivity(), "Bạn cần cấp quyền để ghi âm!", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetRecordBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        audioRecorder = AudioRecorderManager(requireActivity())
        binding?.btnRecord?.setOnClickListener {
            if (!recording) {
                checkAndRequestPermission()
            } else {
                recording = false
                binding?.imgRecord?.setImageResource(R.drawable.ic_micro_fill)
                recordedFilePath = audioRecorder?.stopRecording()
                binding?.recordWaveView?.loadDataWaveView(requireActivity(), recordedFilePath ?: "", fromUrl = false)
                binding?.viewRecord?.isVisible = false
                binding?.viewPreview?.isVisible = true
            }
        }

        binding?.btnDeleteRecord?.setOnClickListener {
            binding?.viewRecord?.isVisible = true
            binding?.viewPreview?.isVisible = false
        }

        binding?.btnSendRecord?.setOnClickListener {
            dismiss()
            onRecordListener?.invoke(recordedFilePath ?: "")
        }
    }

    private fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecord()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecord() {
        recording = true
        audioRecorder?.startRecording(requireActivity())
        binding?.imgRecord?.setImageResource(R.drawable.ic_record_wave)
    }
}