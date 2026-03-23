package com.example.medicinereminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medicinereminder.data.MedicineDatabase
import com.example.medicinereminder.utils.AlarmUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val db = MedicineDatabase.getInstance(context)
            val pendingResult = goAsync()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val medicines = db.medicineDao().getAllMedicinesSync()
                    medicines.forEach { medicine ->
                        AlarmUtils.scheduleAlarm(context, medicine)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
