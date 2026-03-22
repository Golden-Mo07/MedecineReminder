package com.example.medicinereminder.data

import kotlinx.coroutines.flow.Flow

class MedicineRepository(private val medicineDao: MedicineDao) {

    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicines()
    
    val intervalMedicines: Flow<List<Medicine>> = medicineDao.getIntervalMedicines()

    suspend fun insert(medicine: Medicine): Long {
        return medicineDao.insert(medicine)
    }

    suspend fun update(medicine: Medicine) {
        medicineDao.update(medicine)
    }

    suspend fun delete(medicine: Medicine) {
        medicineDao.delete(medicine)
    }

    suspend fun getMedicineById(id: Int): Medicine? {
        return medicineDao.getMedicineById(id)
    }
}