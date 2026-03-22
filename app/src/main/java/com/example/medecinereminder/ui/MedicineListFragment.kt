package com.example.medicinereminder.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicinereminder.R
import com.example.medicinereminder.adapter.MedicineAdapter
import com.example.medicinereminder.databinding.FragmentMedicineListBinding
import com.example.medicinereminder.viewmodel.MainViewModel
import com.example.medicinereminder.viewmodel.MainViewModelFactory
import java.util.concurrent.TimeUnit

class MedicineListFragment : Fragment() {

    private var _binding: FragmentMedicineListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: MedicineAdapter
    
    private val handler = Handler(Looper.getMainLooper())
    private var isShowingAppName = true
    private val animationRunnable = object : Runnable {
        override fun run() {
            if (isShowingAppName) {
                // Fade out app name, show time left
                fadeOutAndIn(getString(R.string.app_name), getTimeLeftString())
            } else {
                // Fade out time left, show app name
                fadeOutAndIn(getTimeLeftString(), getString(R.string.app_name))
            }
            isShowingAppName = !isShowingAppName
            handler.postDelayed(this, if (isShowingAppName) 4000 else 2000)
        }
    }

    private fun fadeOutAndIn(fromText: String, toText: String) {
        binding.toolbarTitle.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.toolbarTitle.text = toText
                binding.toolbarTitle.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }

    private fun getTimeLeftString(): String {
        val medicines = viewModel.intervalMedicines.value ?: return getString(R.string.app_name)
        if (medicines.isEmpty()) return getString(R.string.app_name)
        
        val firstMedicine = medicines.sortedBy { 
            it.lastTriggeredTime + TimeUnit.MINUTES.toMillis(it.intervalMinutes.toLong())
        }.firstOrNull() ?: return getString(R.string.app_name)

        val nextTriggerTime = firstMedicine.lastTriggeredTime + TimeUnit.MINUTES.toMillis(firstMedicine.intervalMinutes.toLong())
        val timeLeftMillis = nextTriggerTime - System.currentTimeMillis()
        
        return if (timeLeftMillis > 0) {
            val hours = TimeUnit.MILLISECONDS.toHours(timeLeftMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftMillis) % 60
            String.format("%s: %02dh %02dm left", firstMedicine.name, hours, minutes)
        } else {
            "${firstMedicine.name}: Due now"
        }
    }

    private val editActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Data might have changed, ViewModel will auto-refresh
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicineListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Toolbar menu
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(requireContext(), SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        adapter = MedicineAdapter { medicine ->
            val intent = Intent(requireContext(), AddEditMedicineActivity::class.java).apply {
                putExtra("medicine_id", medicine.id)
            }
            editActivityLauncher.launch(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        val factory = MainViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(requireActivity(), factory)[MainViewModel::class.java]

        viewModel.allMedicines.observe(viewLifecycleOwner) { medicines ->
            adapter.submitList(medicines)
            binding.textViewEmpty.visibility = if (medicines.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fab.setOnClickListener {
            val intent = Intent(requireContext(), AddEditMedicineActivity::class.java)
            editActivityLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(animationRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(animationRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
