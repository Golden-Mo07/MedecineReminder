package com.example.medicinereminder.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.medicinereminder.ui.MedicineListFragment
import com.example.medicinereminder.ui.ReminderTimersFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ReminderTimersFragment() // Now at the top
            else -> MedicineListFragment() // Now at the bottom
        }
    }
}
