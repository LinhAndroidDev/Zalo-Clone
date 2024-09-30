package com.example.messageapp.fragment

import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentDiscoverBinding
import com.example.messageapp.viewmodel.DiscoverFragmentViewModel

class DiscoverFragment : BaseFragment<FragmentDiscoverBinding, DiscoverFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_discover
}