package com.example.medicinereminder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medicinereminder.data.Medicine
import com.example.medicinereminder.databinding.ItemMedicineBinding

class MedicineAdapter(private val onItemClick: (Medicine) -> Unit) :
    ListAdapter<Medicine, MedicineAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val binding = ItemMedicineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = getItem(position)
        holder.bind(medicine)
        holder.itemView.setOnClickListener { onItemClick(medicine) }
    }

    class MedicineViewHolder(private val binding: ItemMedicineBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(medicine: Medicine) {
            binding.textViewName.text = medicine.name
            if (medicine.isInterval) {
                if (medicine.intervalMinutes >= 60) {
                    val hours = medicine.intervalMinutes / 60
                    val remainingMinutes = medicine.intervalMinutes % 60
                    if (remainingMinutes == 0) {
                        binding.textViewTime.text = "Every $hours h"
                    } else {
                        binding.textViewTime.text = "Every $hours h $remainingMinutes m"
                    }
                } else {
                    binding.textViewTime.text = "Every ${medicine.intervalMinutes} m"
                }
            } else {
                binding.textViewTime.text = medicine.time
            }
        }
    }

    class MedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine) = oldItem == newItem
    }
}
