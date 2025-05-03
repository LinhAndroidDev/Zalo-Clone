package com.example.messageapp.fragment

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.example.messageapp.PersonalActivity
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentPersonalBinding
import com.example.messageapp.utils.AnimatorUtils
import com.example.messageapp.utils.FileUtils.loadImg
import com.example.messageapp.utils.FirebaseAnalyticsInstance
import com.example.messageapp.utils.setOnSingleClickListener
import com.example.messageapp.viewmodel.PersonalFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonalFragment : BaseFragment<FragmentPersonalBinding, PersonalFragmentViewModel>() {
    override val layoutResId = R.layout.fragment_personal

    override fun initView() {
        super.initView()
        // log event: screen_personal
        FirebaseAnalyticsInstance.logPersonalScreen()

        viewModel?.getInfoUser()
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel?.user?.collect { user ->
                binding?.let { binding ->
                    activity?.loadImg(user?.avatar.toString(), binding.avatarUser)
                    binding.tvNameUser.text = user?.name.toString()
                }
            }
        }

        binding?.viewParent?.let { AnimatorUtils.transitionInViewItem(requireActivity(), it) }
    }

    override fun onClickView() {
        super.onClickView()

        binding?.personal?.setOnSingleClickListener {
            val intent = Intent(requireActivity(), PersonalActivity::class.java)
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}