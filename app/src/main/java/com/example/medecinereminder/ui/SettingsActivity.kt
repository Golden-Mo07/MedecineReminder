package com.example.medicinereminder.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.medicinereminder.R
import com.example.medicinereminder.data.MedicineDatabase
import com.example.medicinereminder.databinding.ActivitySettingsBinding
import com.example.medicinereminder.utils.AlarmUtils
import com.example.medicinereminder.widget.MedicineWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Backup shared!", Toast.LENGTH_SHORT).show()
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importBackup(it) }
    }

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
            
            // Set night mode immediately - this will recreate the activity
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            
            // Update widget to reflect theme change
            MedicineWidgetProvider.updateWidget(this)
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

        // Backup & Restore
        binding.buttonBackup.setOnClickListener { exportBackup() }
        binding.buttonRestore.setOnClickListener { importLauncher.launch(arrayOf("*/*")) }

        // Vibration
        binding.switchVibration.isChecked = sharedPrefs.getBoolean("vibration", true)
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("vibration", isChecked).apply()
        }

        binding.buttonBack.setOnClickListener { finish() }
    }

    private fun exportBackup() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dbFile = getDatabasePath("medicine_database")
                if (dbFile.exists()) {
                    val backupFile = File(cacheDir, "medicine_backup.db")
                    FileInputStream(dbFile).use { input ->
                        FileOutputStream(backupFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val contentUri: Uri = FileProvider.getUriForFile(
                        this@SettingsActivity,
                        "${packageName}.fileprovider",
                        backupFile
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Medicine Reminder Backup")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    withContext(Dispatchers.Main) {
                        startActivity(Intent.createChooser(shareIntent, "Export Backup"))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity, "No data to backup", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun importBackup(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get the DB file path
                val dbFile = getDatabasePath("medicine_database")
                
                // Close the database properly before overwriting
                MedicineDatabase.getInstance(this@SettingsActivity).close()

                // Copy the backup file to the database location
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Force re-initialization of the database
                val db = MedicineDatabase.getInstance(this@SettingsActivity)
                val medicines = db.medicineDao().getAllMedicinesSync()
                
                // Reschedule all alarms
                medicines.forEach { AlarmUtils.scheduleAlarm(this@SettingsActivity, it) }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Import successful!", Toast.LENGTH_SHORT).show()
                    recreate()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
