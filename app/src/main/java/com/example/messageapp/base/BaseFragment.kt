package com.example.messageapp.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<VB : ViewDataBinding, VM: BaseViewModel> : Fragment(), CoreInterface.AndroidView {
    protected var binding: VB? = null
    protected var viewModel: VM? = null
    abstract val layoutResId: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel =
            ViewModelProvider(this)[(this::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>]
        binding = DataBindingUtil.inflate(inflater, layoutResId, container, false)
        binding?.lifecycleOwner = viewLifecycleOwner
        showError()
        initView()
        bindData()
        onClickView()
        return binding?.root
    }

    private fun showError() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.errorState?.collect { error ->
                if (error.isNotEmpty()) {
                    Toast.makeText(requireActivity(), error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}