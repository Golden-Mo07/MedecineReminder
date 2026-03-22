package com.example.medicinereminder.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.example.medicinereminder.R
import com.example.medicinereminder.databinding.ActivitySettingsBinding

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Settings"

        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        // Dark Mode
        binding.switchDarkMode.isChecked = sharedPrefs.getBoolean("dark_mode", false)
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Font Size
        val currentFontSize = sharedPrefs.getString("font_size", "Normal")
        when (currentFontSize) {
            "Normal" -> binding.radioNormal.isChecked = true
            "Large" -> binding.radioLarge.isChecked = true
            "X Large (for tablets)" -> binding.radioExtraLarge.isChecked = true
        }
        binding.radioGroupFontSize.setOnCheckedChangeListener { _, checkedId ->
            val newSize = when (checkedId) {
                R.id.radioNormal -> "Normal"
                R.id.radioLarge -> "Large"
                R.id.radioExtraLarge -> "X Large (for tablets)"
                else -> "Normal"
            }
            sharedPrefs.edit().putString("font_size", newSize).apply()
            recreate()
        }

        // Vibration
        binding.switchVibration.isChecked = sharedPrefs.getBoolean("vibration", true)
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("vibration", isChecked).apply()
        }

        binding.buttonBack.setOnClickListener { finish() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
