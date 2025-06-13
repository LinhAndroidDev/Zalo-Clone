package com.example.messageapp.fragment

import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentStatusBinding
import com.example.messageapp.utils.showViewAboveKeyBoard
import com.example.messageapp.viewmodel.StatusFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StatusFragment : BaseFragment<FragmentStatusBinding, StatusFragmentViewModel>() {
    override val layoutResId: Int
        get() = R.layout.fragment_status

    override fun initView() {
        super.initView()

        binding?.root?.apply {
            binding?.footerViewStatus?.showViewAboveKeyBoard(this)
        }
    }

    override fun onClickView() {
        super.onClickView()

        binding?.btnClose?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

}