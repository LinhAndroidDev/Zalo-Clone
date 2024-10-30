package com.example.messageapp.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentSplashBinding
import com.example.messageapp.viewmodel.SplashFragmentViewModel

class SplashFragment : BaseFragment<FragmentSplashBinding, SplashFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_splash
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var runnable: Runnable = Runnable { findNavController().navigate(R.id.introFragment) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handler.postDelayed(runnable, 2000)
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, 2000)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}