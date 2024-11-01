package com.example.messageapp.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentSplashBinding
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.User
import com.example.messageapp.viewmodel.SplashFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding, SplashFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_splash
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var runnable: Runnable? = null

    private var dataFromMainActivity: Conversation? = null
    companion object {
        const val DATA_FRIEND = "DATA_FRIEND"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val friendData: User? = arguments?.getParcelable(DATA_FRIEND)
        friendData?.let {
            dataFromMainActivity = Conversation(friendData)
        }

        runnable = Runnable {
            if (dataFromMainActivity != null) {
                goToChatFragment(dataFromMainActivity!!)
            } else {
                if(viewModel?.isLogin == true) {
                    findNavController().navigate(R.id.homeFragment)
                } else {
                    findNavController().navigate(R.id.introFragment)
                }
            }
        }
        runnable?.let { handler.postDelayed(it, 2000) }
    }

    private fun goToChatFragment(conversation: Conversation) {
        val action = SplashFragmentDirections.actionSplashFragmentToChatFragment(conversation)
        findNavController().navigate(action)
    }

    override fun onResume() {
        super.onResume()
        runnable?.let {
            handler.removeCallbacks(it)
            handler.postDelayed(it, 2000)
        }
    }

    override fun onStop() {
        super.onStop()
        runnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
    }
}