package com.example.messageapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.messageapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var isDoubleTab = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding?.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding?.bottomNav?.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.phoneBookFragment, R.id.discoverFragment, R.id.diaryFragment, R.id.personalFragment -> {
                    binding?.bottomNav?.visibility = View.VISIBLE
                }
                else -> binding?.bottomNav?.visibility = View.GONE
            }
        }
    }

    private fun isFragmentCurrent(fragmentId: Int): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
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
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(isFragmentNav()) {
            if(!isDoubleTab) {
                isDoubleTab = true
                Toast.makeText(this@MainActivity,"Nhấn nút Back lần nữa để thoát", Toast.LENGTH_SHORT).show()

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
        } else {
            super.onBackPressed()
        }
    }
}