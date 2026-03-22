package com.example.medicinereminder.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.medicinereminder.adapter.ReminderTimerAdapter
import com.example.medicinereminder.databinding.FragmentReminderTimersBinding
import com.example.medicinereminder.viewmodel.MainViewModel
import com.example.medicinereminder.viewmodel.MainViewModelFactory
import java.util.concurrent.TimeUnit

class ReminderTimersFragment : Fragment() {

    private var _binding: FragmentReminderTimersBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: ReminderTimerAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            adapter.notifyDataSetChanged()
            handler.postDelayed(this, 60000) // Update every minute
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReminderTimersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = ReminderTimerAdapter()
        binding.recyclerViewTimers.adapter = adapter
        
        val factory = MainViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(requireActivity(), factory)[MainViewModel::class.java]
        
        viewModel.intervalMedicines.observe(viewLifecycleOwner) { medicines ->
            // Sort by time left ascending (least time left at the top)
            val sortedMedicines = medicines.sortedBy { medicine ->
                val nextTriggerTime = medicine.lastTriggeredTime + TimeUnit.MINUTES.toMillis(medicine.intervalMinutes.toLong())
                nextTriggerTime - System.currentTimeMillis()
            }
            adapter.submitList(sortedMedicines)
            binding.textViewNoIntervals.visibility = if (medicines.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
