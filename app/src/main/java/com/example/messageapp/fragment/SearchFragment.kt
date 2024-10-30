package com.example.messageapp.fragment

import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentSearchBinding
import com.example.messageapp.viewmodel.SearchFragmentViewModel

class SearchFragment : BaseFragment<FragmentSearchBinding, SearchFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_search

    override fun initView() {
        super.initView()

        binding?.root?.post {
            binding?.header?.focusSearch()
        }
    }

    override fun onClickView() {
        super.onClickView()
    }
}