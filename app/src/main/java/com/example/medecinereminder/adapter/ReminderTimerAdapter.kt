package com.example.medicinereminder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medicinereminder.data.Medicine
import com.example.medicinereminder.databinding.ItemReminderTimerBinding
import java.util.concurrent.TimeUnit

class ReminderTimerAdapter : ListAdapter<Medicine, ReminderTimerAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReminderTimerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemReminderTimerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(medicine: Medicine) {
            binding.textViewMedicineName.text = medicine.name
            
            val intervalText = if (medicine.intervalMinutes >= 60) {
                val hours = medicine.intervalMinutes / 60
                val remainingMinutes = medicine.intervalMinutes % 60
                if (remainingMinutes == 0) {
                    "Every $hours h"
                } else {
                    "Every $hours h $remainingMinutes m"
                }
            } else {
                "Every ${medicine.intervalMinutes} m"
            }
            binding.textViewInterval.text = intervalText
            
            val currentTime = System.currentTimeMillis()
            val nextTriggerTime = medicine.lastTriggeredTime + TimeUnit.MINUTES.toMillis(medicine.intervalMinutes.toLong())
            val timeLeftMillis = nextTriggerTime - currentTime
            
            if (timeLeftMillis > 0) {
                val hours = TimeUnit.MILLISECONDS.toHours(timeLeftMillis)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftMillis) % 60
                binding.textViewTimeLeft.text = String.format("%02dh %02dm left", hours, minutes)
            } else {
                binding.textViewTimeLeft.text = "Due now"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine) = oldItem == newItem
    }
}
