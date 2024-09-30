package com.example.messageapp.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.reflect.ParameterizedType

@Suppress("UNREACHABLE_CODE")
abstract class BaseFragment<VB : ViewDataBinding, VM: ViewModel> : Fragment(), CoreInterface.AndroidView {
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
        initView()
        onClickView()
        return binding?.root
    }
}