package com.example.medicinereminder.ui

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.medicinereminder.R

abstract class BaseActivity : AppCompatActivity() {

    private var currentFontSize: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        currentFontSize = sharedPrefs.getString("font_size", "Normal")
        applySettings()
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val fontSize = sharedPrefs.getString("font_size", "Normal")
        if (fontSize != currentFontSize) {
            recreate()
        }
    }

    private fun applySettings() {
        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val darkMode = sharedPrefs.getBoolean("dark_mode", false)
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val fontSize = sharedPrefs.getString("font_size", "Normal")
        when (fontSize) {
            "Large" -> theme.applyStyle(R.style.FontSizeLarge, true)
            "X Large (for tablets)" -> theme.applyStyle(R.style.FontSizeExtraLarge, true)
        }
        return theme
    }
}
