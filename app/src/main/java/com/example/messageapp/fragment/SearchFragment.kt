package com.example.messageapp.fragment

import androidx.lifecycle.lifecycleScope
import com.example.messageapp.R
import com.example.messageapp.adapter.SearchAdapter
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.custom.CustomHeaderView
import com.example.messageapp.databinding.FragmentSearchBinding
import com.example.messageapp.viewmodel.SearchFragmentViewModel
import kotlinx.coroutines.launch

class SearchFragment : BaseFragment<FragmentSearchBinding, SearchFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_search
    private val searchAdapter = SearchAdapter()

    override fun initView() {
        super.initView()

        binding?.root?.post {
            binding?.header?.focusSearch()
        }

        binding?.header?.setOnTypeSearch(object : CustomHeaderView.OnTypeSearchListener {
            override fun callBackKeySearch(keySearch: String) {
                viewModel?.searchFriend(keySearch)
            }

        })

        binding?.rcvSearchFriend?.adapter = searchAdapter
    }

    override fun bindData() {
        super.bindData()

        lifecycleScope.launch {
            viewModel?.users?.collect {
                binding?.tvNumberFriend?.text =
                    String.format(getString(R.string.you_may_be_familiar_with), it.size.toString())
                searchAdapter.resetList(it)
            }
        }
    }

    override fun onClickView() {
        super.onClickView()
    }
}