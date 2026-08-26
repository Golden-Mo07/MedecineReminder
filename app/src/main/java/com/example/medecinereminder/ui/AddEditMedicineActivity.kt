package com.example.medicinereminder.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.medicinereminder.R
import com.example.medicinereminder.data.Medicine
import com.example.medicinereminder.data.MedicineDatabase
import com.example.medicinereminder.data.MedicineRepository
import com.example.medicinereminder.databinding.ActivityAddEditMedicineBinding
import com.example.medicinereminder.utils.AlarmUtils
import com.example.medicinereminder.widget.MedicineWidgetProvider
import kotlinx.coroutines.launch
import java.util.Calendar

class AddEditMedicineActivity : BaseActivity() {

    private lateinit var binding: ActivityAddEditMedicineBinding
    private lateinit var repository: MedicineRepository
    private var medicineId: Int = 0
    
    // Fixed Time selection
    private var selectedHour = 8
    private var selectedMinute = 0

    // Interval Start Time selection
    private var intervalStartHour = 8
    private var intervalStartMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditMedicineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = MedicineDatabase.getInstance(this)
        repository = MedicineRepository(db.medicineDao())

        setupNumberPickers()
        setupDefaultTimes()

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

        binding.editTextTime.setOnClickListener { showFixedTimePicker() }
        binding.editTextIntervalStartTime.setOnClickListener { showIntervalStartTimePicker() }
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

    private fun setupDefaultTimes() {
        val now = Calendar.getInstance()
        selectedHour = now.get(Calendar.HOUR_OF_DAY)
        selectedMinute = now.get(Calendar.MINUTE)
        
        intervalStartHour = selectedHour
        intervalStartMinute = selectedMinute
        
        binding.editTextTime.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
        binding.editTextIntervalStartTime.setText(String.format("%02d:%02d", intervalStartHour, intervalStartMinute))
    }

    private fun loadMedicine() {
        lifecycleScope.launch {
            val medicine = repository.getMedicineById(medicineId)
            if (medicine != null) {
                binding.editTextName.setText(medicine.name)
                binding.editTextComment.setText(medicine.comment)
                if (medicine.isInterval) {
                    binding.radioInterval.isChecked = true
                    val hours = medicine.intervalMinutes / 60
                    val minutes = medicine.intervalMinutes % 60
                    binding.numberPickerHours.value = hours
                    binding.numberPickerMinutes.value = minutes
                    
                    // For intervals, we use the lastTriggeredTime (which stores the schedule base)
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = medicine.lastTriggeredTime + (medicine.intervalMinutes * 60 * 1000L)
                    intervalStartHour = cal.get(Calendar.HOUR_OF_DAY)
                    intervalStartMinute = cal.get(Calendar.MINUTE)
                    binding.editTextIntervalStartTime.setText(String.format("%02d:%02d", intervalStartHour, intervalStartMinute))
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

    private fun showFixedTimePicker() {
        TimePickerDialog(this, { _, hourOfDay, minute ->
            selectedHour = hourOfDay
            selectedMinute = minute
            binding.editTextTime.setText(String.format("%02d:%02d", hourOfDay, minute))
        }, selectedHour, selectedMinute, true).show()
    }

    private fun showIntervalStartTimePicker() {
        TimePickerDialog(this, { _, hourOfDay, minute ->
            intervalStartHour = hourOfDay
            intervalStartMinute = minute
            binding.editTextIntervalStartTime.setText(String.format("%02d:%02d", hourOfDay, minute))
        }, intervalStartHour, intervalStartMinute, true).show()
    }

    private fun saveMedicine() {
        val name = binding.editTextName.text.toString().trim()
        val comment = binding.editTextComment.text.toString().trim()
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

                // Calculate base time from chosen "Starting from"
                val startCal = Calendar.getInstance()
                startCal.set(Calendar.HOUR_OF_DAY, intervalStartHour)
                startCal.set(Calendar.MINUTE, intervalStartMinute)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                
                // We set lastTriggeredTime such that lastTriggeredTime + totalMinutes = Starting from time
                val baseTime = startCal.timeInMillis - (totalMinutes * 60 * 1000L)
                
                Medicine(
                    id = medicineId, 
                    name = name, 
                    comment = comment,
                    isInterval = true, 
                    intervalMinutes = totalMinutes,
                    lastTriggeredTime = baseTime
                )
            } else {
                val time = binding.editTextTime.text.toString().trim()
                if (time.isEmpty()) {
                    Toast.makeText(this@AddEditMedicineActivity, "Please enter time", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                Medicine(
                    id = medicineId, 
                    name = name, 
                    comment = comment,
                    time = time, 
                    isInterval = false
                )
            }

            val id = if (medicineId == 0) {
                repository.insert(medicine).toInt()
            } else {
                repository.update(medicine)
                medicineId
            }
            
            val savedMedicine = medicine.copy(id = id)
            AlarmUtils.scheduleAlarm(this@AddEditMedicineActivity, savedMedicine)
            
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
                AlarmUtils.cancelAlarm(this@AddEditMedicineActivity, medicine)
                
                // Update the widget
                MedicineWidgetProvider.updateWidget(this@AddEditMedicineActivity)
            }
            finish()
        }
    }
}
