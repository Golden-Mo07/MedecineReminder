package com.example.medicinereminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.medicinereminder.R
import com.example.medicinereminder.ui.AddEditMedicineActivity
import com.example.medicinereminder.MainActivity

class MedicineWidgetProvider : AppWidgetProvider() {

    companion object {
        fun updateWidget(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MedicineWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            val intent = Intent(context, MedicineWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MedicineWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.medicine_list)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)

        val views = RemoteViews(context.packageName, R.layout.medicine_widget)

        // Select resources based on the app's dark mode setting
        val bgColor = if (isDarkMode) context.getColor(R.color.widget_bg_dark) else context.getColor(R.color.widget_bg_light)
        val textColor = if (isDarkMode) context.getColor(R.color.widget_text_main_dark) else context.getColor(R.color.widget_text_main_light)
        val subTextColor = if (isDarkMode) context.getColor(R.color.widget_text_sub_dark) else context.getColor(R.color.widget_text_sub_light)
        val accentColor = if (isDarkMode) context.getColor(R.color.widget_accent_dark) else context.getColor(R.color.widget_accent_light)
        val bgDrawable = if (isDarkMode) R.drawable.widget_background_dark else R.drawable.widget_background_light

        // Apply background drawable
        views.setInt(R.id.widget_root, "setBackgroundResource", bgDrawable)
        
        // Apply text and icon colors directly
        views.setTextColor(R.id.widget_title, textColor)
        views.setTextColor(R.id.empty_view, subTextColor)
        views.setInt(R.id.button_add_reminder, "setColorFilter", accentColor)

        // Set up the intent for the Add Reminder button
        val addIntent = Intent(context, AddEditMedicineActivity::class.java)
        val addPendingIntent = PendingIntent.getActivity(
            context, 2, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.button_add_reminder, addPendingIntent)

        // Title click opens the app
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 1, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, mainPendingIntent)

        // Set up the collection (ListView)
        val serviceIntent = Intent(context, MedicineWidgetService::class.java)
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
        views.setRemoteAdapter(R.id.medicine_list, serviceIntent)
        views.setEmptyView(R.id.medicine_list, R.id.empty_view)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
