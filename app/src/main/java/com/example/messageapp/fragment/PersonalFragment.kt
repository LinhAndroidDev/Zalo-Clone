package com.example.messageapp.fragment

import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentPersonalBinding
import com.example.messageapp.viewmodel.PersonalFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PersonalFragment : BaseFragment<FragmentPersonalBinding, PersonalFragmentViewModel>() {
    override val layoutResId = R.layout.fragment_personal

    override fun initView() {
        super.initView()

        binding?.let { binding ->
            Glide.with(requireActivity())
                .load(viewModel?.shared?.getAvatarUser())
                .circleCrop()
                .error(R.mipmap.ic_launcher)
                .into(binding.avatarUser)

            binding.tvNameUser.text = viewModel?.shared?.getNameUser()
        }
    }

    override fun onClickView() {
        super.onClickView()

        binding?.personal?.setOnClickListener {

        }
    }
}