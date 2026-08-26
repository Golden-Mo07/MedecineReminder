package com.example.medicinereminder

import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.viewpager2.widget.ViewPager2
import com.example.medicinereminder.adapter.MainPagerAdapter
import com.example.medicinereminder.databinding.ActivityMainBinding
import com.example.medicinereminder.ui.BaseActivity

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setupViewPager()
        setupBackNavigation()
    }

    private fun setupViewPager() {
        val pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        
        // Open the app on the standard medicine list (index 1)
        binding.viewPager.setCurrentItem(1, false)
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If we are on the Interval Reminders page (index 0), go back to Main page (index 1)
                if (binding.viewPager.currentItem == 0) {
                    binding.viewPager.setCurrentItem(1, true)
                } else {
                    // If we are already on index 1, exit the app
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}
