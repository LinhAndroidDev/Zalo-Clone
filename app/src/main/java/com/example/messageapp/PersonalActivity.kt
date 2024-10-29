package com.example.messageapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.messageapp.databinding.ActivityPersonalBinding
import com.example.messageapp.model.User
import com.example.messageapp.viewmodel.PersonalActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonalActivity : AppCompatActivity() {
    private val binding by lazy { ActivityPersonalBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<PersonalActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
        onClickView()
    }

    private fun onClickView() {
        binding.back.setOnClickListener { onBackPressed() }
    }

    private fun handleDataUser(user: User) {
        binding.nameUser.text = user.name
        Glide.with(this)
            .load(user.avatar)
            .error(R.mipmap.ic_launcher)
            .into(binding.avatarUser)
    }

    private fun initView() {
        setUpFullScreen()

        viewModel.getInfoUser()
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.user.collect { user ->
                user?.let {
                    handleDataUser(user)
                }
            }
        }
    }

    private fun setUpFullScreen() {
        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT
    }


}