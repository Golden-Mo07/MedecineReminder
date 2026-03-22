package com.example.medicinereminder.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medicinereminder.data.Medicine
import com.example.medicinereminder.data.MedicineDatabase
import com.example.medicinereminder.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("medicine_name") ?: "Medicine"
        val medicineId = intent.getIntExtra("medicine_id", 0)
        
        // Show the notification immediately
        NotificationHelper(context).showNotification(medicineName)
        
        if (medicineId != 0) {
            val pendingResult = goAsync()
            val db = MedicineDatabase.getInstance(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val medicine = db.medicineDao().getMedicineById(medicineId)
                    if (medicine != null) {
                        // Update last triggered time to now
                        val updatedMedicine = medicine.copy(lastTriggeredTime = System.currentTimeMillis())
                        db.medicineDao().update(updatedMedicine)
                        
                        // Reschedule the next alarm
                        rescheduleAlarm(context, updatedMedicine)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun rescheduleAlarm(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("medicine_name", medicine.name)
            putExtra("medicine_id", medicine.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (medicine.isInterval) {
            val intervalMillis = medicine.intervalMinutes * 60 * 1000L
            val triggerTime = System.currentTimeMillis() + intervalMillis
            
            // Using setExactAndAllowWhileIdle for reliability on interval
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            // For fixed time, schedule for the same time tomorrow
            val triggerTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}
