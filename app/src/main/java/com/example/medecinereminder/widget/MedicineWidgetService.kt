package com.example.medicinereminder.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.medicinereminder.R
import com.example.medicinereminder.data.Medicine
import com.example.medicinereminder.data.MedicineDatabase
import kotlinx.coroutines.runBlocking

class MedicineWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MedicineRemoteViewsFactory(this.applicationContext)
    }
}

class MedicineRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var medicines: List<Medicine> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Fetch data from Room database synchronously
        runBlocking {
            medicines = MedicineDatabase.getInstance(context).medicineDao().getAllMedicinesSync()
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = medicines.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= medicines.size) return RemoteViews(context.packageName, R.layout.widget_item)
        
        val medicine = medicines[position]
        
        val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)

        val views = RemoteViews(context.packageName, R.layout.widget_item)
        
        // Select colors based on dark mode setting
        val textColor = if (isDarkMode) context.getColor(R.color.widget_text_main_dark) else context.getColor(R.color.widget_text_main_light)
        val subTextColor = if (isDarkMode) context.getColor(R.color.widget_text_sub_dark) else context.getColor(R.color.widget_text_sub_light)
        
        // Apply theme colors to the item views
        views.setTextColor(R.id.widget_item_name, textColor)
        views.setTextColor(R.id.widget_item_time, subTextColor)
        
        views.setTextViewText(R.id.widget_item_name, medicine.name)
        
        val timeText = if (medicine.isInterval) {
            "Every ${medicine.intervalMinutes} min"
        } else {
            "At ${medicine.time}"
        }
        views.setTextViewText(R.id.widget_item_time, timeText)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = medicines[position].id.toLong()

    override fun hasStableIds(): Boolean = true
}
