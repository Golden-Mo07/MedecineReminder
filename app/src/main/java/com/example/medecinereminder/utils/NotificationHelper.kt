package com.example.medicinereminder.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.medicinereminder.MainActivity
import com.example.medicinereminder.R

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "medicine_reminder_channel"
        const val BASE_NOTIFICATION_ID = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(medicineId: Int, medicineName: String, comment: String = "") {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, medicineId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val customTitle = sharedPrefs.getString("notification_title", context.getString(R.string.notification_title))
        val customPrefix = sharedPrefs.getString("notification_message", context.getString(R.string.notification_text_prefix))

        val message = "$customPrefix $medicineName".trim()
        val contentText = if (comment.isNotEmpty()) {
            "$message\n$comment"
        } else {
            message
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(customTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Use medicineId to allow multiple distinct notifications
        notificationManager.notify(BASE_NOTIFICATION_ID + medicineId, notification)
    }
}