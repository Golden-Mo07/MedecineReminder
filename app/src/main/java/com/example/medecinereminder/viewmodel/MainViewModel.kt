package com.example.medicinereminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.medicinereminder.data.Medicine
import com.example.medicinereminder.data.MedicineDatabase
import com.example.medicinereminder.data.MedicineRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MedicineRepository
    val allMedicines: LiveData<List<Medicine>>
    val intervalMedicines: LiveData<List<Medicine>>

    init {
        val db = MedicineDatabase.getInstance(application)
        repository = MedicineRepository(db.medicineDao())
        allMedicines = repository.allMedicines.asLiveData()
        intervalMedicines = repository.intervalMedicines.asLiveData()
    }
}
