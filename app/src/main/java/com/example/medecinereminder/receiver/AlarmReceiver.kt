package com.example.medicinereminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medicinereminder.data.MedicineDatabase
import com.example.medicinereminder.utils.AlarmUtils
import com.example.medicinereminder.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("medicine_name") ?: "Medicine"
        val medicineComment = intent.getStringExtra("medicine_comment") ?: ""
        val medicineId = intent.getIntExtra("medicine_id", 0)

        // Show the notification with the medicineId and custom text from settings
        NotificationHelper(context).showNotification(medicineId, medicineName, medicineComment)
        
        if (medicineId != 0) {
            val pendingResult = goAsync()
            val db = MedicineDatabase.getInstance(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val medicine = db.medicineDao().getMedicineById(medicineId)
                    if (medicine != null) {
                        // Update last triggered time to now to maintain exact intervals
                        val updatedMedicine = medicine.copy(lastTriggeredTime = System.currentTimeMillis())
                        db.medicineDao().update(updatedMedicine)
                        
                        // Use unified scheduling logic for reliability and exactness
                        AlarmUtils.scheduleAlarm(context, updatedMedicine)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
