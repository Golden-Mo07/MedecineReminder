package com.example.medicinereminder.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.medicinereminder.R
import com.example.medicinereminder.data.Medicine
import com.example.medicinereminder.data.MedicineDatabase
import com.example.medicinereminder.data.MedicineRepository
import com.example.medicinereminder.databinding.ActivityAddEditMedicineBinding
import com.example.medicinereminder.receiver.AlarmReceiver
import com.example.medicinereminder.widget.MedicineWidgetProvider
import kotlinx.coroutines.launch
import java.util.Calendar

class AddEditMedicineActivity : BaseActivity() {

    private lateinit var binding: ActivityAddEditMedicineBinding
    private lateinit var repository: MedicineRepository
    private var medicineId: Int = 0
    private var selectedHour = 8
    private var selectedMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditMedicineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = MedicineDatabase.getInstance(this)
        repository = MedicineRepository(db.medicineDao())

        setupNumberPickers()

        // Check if we are editing
        medicineId = intent.getIntExtra("medicine_id", 0)
        if (medicineId != 0) {
            title = getString(R.string.edit_medicine)
            binding.buttonDelete.visibility = View.VISIBLE
            loadMedicine()
        } else {
            title = getString(R.string.add_medicine)
        }

        binding.radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioFixed) {
                binding.layoutFixedTime.visibility = View.VISIBLE
                binding.layoutInterval.visibility = View.GONE
            } else {
                binding.layoutFixedTime.visibility = View.GONE
                binding.layoutInterval.visibility = View.VISIBLE
            }
        }

        binding.editTextTime.setOnClickListener { showTimePicker() }
        binding.buttonSave.setOnClickListener { saveMedicine() }
        binding.buttonDelete.setOnClickListener { deleteMedicine() }
        binding.buttonCancel.setOnClickListener { finish() }
    }

    private fun setupNumberPickers() {
        binding.numberPickerHours.apply {
            minValue = 0
            maxValue = 23
            value = 0
        }
        binding.numberPickerMinutes.apply {
            minValue = 0
            maxValue = 59
            value = 30 // Default 30 mins
        }
    }

    private fun loadMedicine() {
        lifecycleScope.launch {
            val medicine = repository.getMedicineById(medicineId)
            if (medicine != null) {
                binding.editTextName.setText(medicine.name)
                if (medicine.isInterval) {
                    binding.radioInterval.isChecked = true
                    val hours = medicine.intervalMinutes / 60
                    val minutes = medicine.intervalMinutes % 60
                    binding.numberPickerHours.value = hours
                    binding.numberPickerMinutes.value = minutes
                } else {
                    binding.radioFixed.isChecked = true
                    binding.editTextTime.setText(medicine.time)
                    val parts = medicine.time.split(":")
                    if (parts.size == 2) {
                        selectedHour = parts[0].toInt()
                        selectedMinute = parts[1].toInt()
                    }
                }
            }
        }
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, hourOfDay, minute ->
            selectedHour = hourOfDay
            selectedMinute = minute
            binding.editTextTime.setText(String.format("%02d:%02d", hourOfDay, minute))
        }, selectedHour, selectedMinute, true).show()
    }

    private fun saveMedicine() {
        val name = binding.editTextName.text.toString().trim()
        val isInterval = binding.radioInterval.isChecked
        
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val medicine = if (isInterval) {
                val intervalHours = binding.numberPickerHours.value
                val intervalMinutes = binding.numberPickerMinutes.value
                val totalMinutes = (intervalHours * 60) + intervalMinutes
                
                if (totalMinutes == 0) {
                    Toast.makeText(this@AddEditMedicineActivity, "Interval cannot be 0", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                Medicine(
                    id = medicineId, 
                    name = name, 
                    isInterval = true, 
                    intervalMinutes = totalMinutes,
                    lastTriggeredTime = System.currentTimeMillis()
                )
            } else {
                val time = binding.editTextTime.text.toString().trim()
                if (time.isEmpty()) {
                    Toast.makeText(this@AddEditMedicineActivity, "Please enter time", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                Medicine(id = medicineId, name = name, time = time, isInterval = false)
            }

            val id = if (medicineId == 0) {
                repository.insert(medicine).toInt()
            } else {
                repository.update(medicine)
                medicineId
            }
            
            val savedMedicine = medicine.copy(id = id)
            scheduleAlarm(savedMedicine)
            
            // Update the widget
            MedicineWidgetProvider.updateWidget(this@AddEditMedicineActivity)
            
            finish()
        }
    }

    private fun deleteMedicine() {
        lifecycleScope.launch {
            val medicine = repository.getMedicineById(medicineId)
            if (medicine != null) {
                repository.delete(medicine)
                cancelAlarm(medicine)
                
                // Update the widget
                MedicineWidgetProvider.updateWidget(this@AddEditMedicineActivity)
            }
            finish()
        }
    }

    private fun scheduleAlarm(medicine: Medicine) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("medicine_name", medicine.name)
            putExtra("medicine_id", medicine.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        if (medicine.isInterval) {
            val intervalMillis = medicine.intervalMinutes * 60 * 1000L
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                intervalMillis,
                pendingIntent
            )
        } else {
            val parts = medicine.time.split(":")
            calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            calendar.set(Calendar.MINUTE, parts[1].toInt())
            calendar.set(Calendar.SECOND, 0)
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        }
    }

    private fun cancelAlarm(medicine: Medicine) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            medicine.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
