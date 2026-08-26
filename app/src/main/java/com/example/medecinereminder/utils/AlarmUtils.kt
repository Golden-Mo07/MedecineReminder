package com.example.medicinereminder.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.medicinereminder.MainActivity
import com.example.medicinereminder.data.Medicine
import com.example.medicinereminder.receiver.AlarmReceiver
import java.util.Calendar

object AlarmUtils {

    /**
     * Schedules the next alarm for a medicine using high-precision AlarmClock.
     * @param context Application context
     * @param medicine The medicine to schedule
     * @param triggerAtTarget Optional target time to maintain exact intervals and avoid drift.
     */
    fun scheduleAlarm(context: Context, medicine: Medicine, triggerAtTarget: Long? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("medicine_name", medicine.name)
            putExtra("medicine_id", medicine.id)
            putExtra("medicine_comment", medicine.comment)
        }

        val triggerAtMillis: Long = if (medicine.isInterval) {
            val intervalMillis = medicine.intervalMinutes * 60 * 1000L
            
            // Use provided target or calculate from last triggered time
            var nextTrigger = triggerAtTarget ?: (medicine.lastTriggeredTime + intervalMillis)
            
            // If the calculation puts the alarm in the past, find the next future occurrence.
            if (nextTrigger < currentTime) {
                val diff = currentTime - nextTrigger
                val missedIntervals = (diff / intervalMillis) + 1
                nextTrigger += missedIntervals * intervalMillis
            }
            nextTrigger
        } else {
            // Fixed Time Alarms
            val calendar = Calendar.getInstance()
            val parts = medicine.time.split(":")
            if (parts.size == 2) {
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                if (calendar.timeInMillis <= currentTime) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                calendar.timeInMillis
            } else {
                currentTime + 60000 // Fallback
            }
        }

        // Pass the intended fire time into the intent so the receiver can calculate the *exact* next slot.
        intent.putExtra("intended_time", triggerAtMillis)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Clear any old alarms for this ID before scheduling new one.
        alarmManager.cancel(pendingIntent)

        // setAlarmClock is synchronized with the phone's system clock and immune to Doze mode.
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            medicine.id,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    fun cancelAlarm(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
