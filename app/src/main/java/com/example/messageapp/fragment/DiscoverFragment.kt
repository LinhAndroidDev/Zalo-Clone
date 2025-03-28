package com.example.messageapp.fragment

import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentDiscoverBinding
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.viewmodel.DiscoverFragmentViewModel

class DiscoverFragment : BaseFragment<FragmentDiscoverBinding, DiscoverFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_discover

    override fun initView() {
        super.initView()

        binding?.imgAd?.let {
            activity?.loadImg(
                "https://media.istockphoto.com/id/1487894858/vi/anh/bi%E1%BB%83u-%C4%91%E1%BB%93-n%E1%BA%BFn-v%C3%A0-d%E1%BB%AF-li%E1%BB%87u-c%E1%BB%A7a-th%E1%BB%8B-tr%C6%B0%E1%BB%9Dng-t%C3%A0i-ch%C3%ADnh.jpg?s=612x612&w=0&k=20&c=QEOshW25DopLeCe4bEQXXLbQB2pbIiy4bm0T27UJR4g=",
                it
            )
        }

        binding?.viewParent?.let { AnimatorUtils.fadeInViewItem(requireActivity(), it) }
    }
}