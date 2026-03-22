package com.example.medicinereminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val time: String = "", // stored as "HH:mm" (24-hour format) for FIXED type
    val isInterval: Boolean = false,
    val intervalMinutes: Int = 0, // for INTERVAL type
    val lastTriggeredTime: Long = System.currentTimeMillis() // To calculate time left
)
