package com.example.messageapp.base

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<VB : ViewDataBinding, VM: BaseViewModel> : Fragment(), CoreInterface.AndroidView {
    protected var binding: VB? = null
    protected var viewModel: VM? = null
    abstract val layoutResId: Int

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

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
        bindData()
        onClickView()
        return binding?.root
    }

    override fun initView() {
        super.initView()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermissionApp(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        checkPermissionApp(android.Manifest.permission.CAMERA)
        showError()
    }

    private fun checkPermissionApp(permission: String) {
        when {
            ContextCompat.checkSelfPermission(
                requireActivity(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(permission)
            }
        }
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