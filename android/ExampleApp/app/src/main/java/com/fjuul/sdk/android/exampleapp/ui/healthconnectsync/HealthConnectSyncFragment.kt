package com.fjuul.sdk.android.exampleapp.ui.healthconnectsync

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.databinding.HealthConnectSyncFragmentBinding
import java.time.LocalDate

class HealthConnectSyncFragment : Fragment() {

    companion object {
        fun newInstance() = HealthConnectSyncFragment()
    }

    private lateinit var viewModel: HealthConnectSyncViewModel
    private lateinit var binding: HealthConnectSyncFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HealthConnectSyncFragmentBinding.bind(inflater.inflate(R.layout.health_connect_sync_fragment, container, false))
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(HealthConnectSyncViewModel::class.java)

        binding.intradaySectionText.sectionText.text = "Intraday metrics"
        binding.sessionsSectionText.sectionText.text = "Sessions"
        binding.profileSectionText.sectionText.text = "Profile"

        viewModel.startDate.observe(viewLifecycleOwner) { date ->
            binding.startDateValueText.text = date.toString()
        }
        viewModel.endDate.observe(viewLifecycleOwner) {
            binding.endDateValueText.text = it.toString()
        }
        viewModel.syncingIntradayMetrics.observe(viewLifecycleOwner) { syncing ->
            binding.intradaySyncProgressBar.visibility = if (syncing) View.VISIBLE else View.INVISIBLE
        }
        viewModel.syncingSessions.observe(viewLifecycleOwner) { syncing ->
            binding.sessionsSyncProgressBar.visibility = if (syncing) View.VISIBLE else View.INVISIBLE
        }
        viewModel.syncingProfile.observe(viewLifecycleOwner) { syncing ->
            binding.profileSyncProgressBar.visibility = if (syncing) View.VISIBLE else View.INVISIBLE
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) {
            it?.let { error ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage(error)
                    .show()
                viewModel.resetErrorMessage()
            }
        }

        binding.startDateInputLayout.setOnClickListener {
            val date = viewModel.startDate.value ?: LocalDate.now()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    viewModel.setupDateRange(startDate = LocalDate.of(year, month + 1, dayOfMonth))
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }

        binding.endDateInputLayout.setOnClickListener {
            val date = viewModel.endDate.value ?: LocalDate.now()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    viewModel.setupDateRange(endDate = LocalDate.of(year, month + 1, dayOfMonth))
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }

        binding.minSessionDurationTextEdit.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.minSessionDurationTextEdit.text.isNullOrEmpty()) {
                binding.minSessionDurationTextEdit.setText("3")
            }
        }

        binding.runIntradaySyncButton.setOnClickListener {
            val calories = binding.caloriesCheckBox.isChecked
            val steps = binding.stepsCheckBox.isChecked
            val heartRate = binding.heartRateCheckBox.isChecked
            val distance = binding.distanceCheckBox.isChecked
            viewModel.runIntradaySync(calories, heartRate, steps, distance)
        }

        binding.runSessionsSyncButton.setOnClickListener {
            val minutesDurationText = binding.minSessionDurationTextEdit.text.toString()
            val minSessionDuration = try {
                val minutes = minutesDurationText.toInt()
                if (minutes > 0) java.time.Duration.ofMinutes(minutes.toLong()) else null
            } catch (exc: Exception) { null }

            if (minSessionDuration == null) return@setOnClickListener
            viewModel.runSessionsSync(minSessionDuration)
        }

        binding.runProfileSyncButton.setOnClickListener {
            val height = binding.heightCheckBox.isChecked
            val weight = binding.weightCheckBox.isChecked
            viewModel.runProfileSync(height, weight)
        }
    }
}
