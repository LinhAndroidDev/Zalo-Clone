package com.example.messageapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.messageapp.databinding.ActivityMainBinding
import com.example.messageapp.fragment.SplashFragment
import com.example.messageapp.model.User
import com.example.messageapp.service.ReceiverMessageService
import com.example.messageapp.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var isDoubleTab = false
    private val viewModel by viewModels<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
    }

    private fun initView() {
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding?.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.navigation_main)
        navGraph.setStartDestination(R.id.splashFragment)

        //Receive Data from Notification
        val friendData: User? = intent.getParcelableExtra(ReceiverMessageService.OBJECT_FRIEND)
        val bundle = Bundle()
        bundle.putParcelable(SplashFragment.DATA_FRIEND, friendData)
        navController.setGraph(navGraph, bundle)

        binding?.bottomNav?.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.phoneBookFragment, R.id.discoverFragment, R.id.diaryFragment, R.id.personalFragment -> {
                    binding?.viewBottomNav?.visibility = View.VISIBLE
                }

                else -> binding?.viewBottomNav?.visibility = View.GONE
            }
        }

        getDataNumberUnSeen()
    }

    private fun getDataNumberUnSeen() {
        viewModel.getNumberUnSeen()
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.numberMsgUnSeen.collect { num ->
                setUpNumberMessage(num)
            }
        }
    }

    private fun setUpNumberMessage(num: Int) {
        val badge = binding?.bottomNav?.getOrCreateBadge(R.id.homeFragment)
        badge?.isVisible = num > 0
        badge?.number = num
        badge?.backgroundColor = getColor(R.color.red)
        badge?.badgeTextColor = getColor(R.color.text_white)
    }

    internal fun getHeightBottomNav(): Int {
        return binding?.viewBottomNav?.height ?: 0
    }

    private fun isFragmentCurrent(fragmentId: Int): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val currentDestination = navController.currentDestination

        return currentDestination?.id == fragmentId
    }

    private fun isFragmentNav(): Boolean {
        return isFragmentCurrent(R.id.homeFragment)
                || isFragmentCurrent(R.id.phoneBookFragment)
                || isFragmentCurrent(R.id.discoverFragment)
                || isFragmentCurrent(R.id.diaryFragment)
                || isFragmentCurrent(R.id.personalFragment)
                || isFragmentCurrent(R.id.introFragment)
    }

    private fun backToFragment(destinationFragment: Int) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(
            destinationFragment, null,
            NavOptions.Builder().setPopUpTo(navController.graph.startDestinationId, true)
                .build()
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isFragmentNav()) {
            handleDoubleTabBackPress()
        } else {
            if (isFragmentCurrent(R.id.loginFragment)) {
                backToFragment(R.id.introFragment)
            } else if(isFragmentCurrent(R.id.chatFragment)) {
                backToFragment(R.id.homeFragment)
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun handleDoubleTabBackPress() {
        if (!isDoubleTab) {
            isDoubleTab = true
            Toast.makeText(
                this@MainActivity,
                "Nhấn nút Back lần nữa để thoát",
                Toast.LENGTH_SHORT
            ).show()
            object : CountDownTimer(2000, 2000) {
                override fun onTick(millisUntilFinished: Long) {

                }

                override fun onFinish() {
                    isDoubleTab = false
                }

            }.start()
        } else {
            finish()
        }
    }
}