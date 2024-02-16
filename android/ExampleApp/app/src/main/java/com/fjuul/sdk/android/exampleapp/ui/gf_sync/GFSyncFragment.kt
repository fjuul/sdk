package com.fjuul.sdk.android.exampleapp.ui.gf_sync

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.databinding.GfSyncFragmentBinding
import java.lang.Exception
import java.time.Duration
import java.time.LocalDate

class GFSyncFragment : Fragment() {

    companion object {
        fun newInstance() = GFSyncFragment()
    }

    private lateinit var viewModel: GFSyncViewModel
    private lateinit var binding: GfSyncFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GfSyncFragmentBinding.bind(inflater.inflate(R.layout.gf_sync_fragment, container, false))
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GFSyncViewModel::class.java)

        binding.intradaySectionText.sectionText.text = "Intraday metrics"
        binding.sessionsSectionText.sectionText.text = "Sessions"
        binding.profileSectionText.sectionText.text = "Profile"

        viewModel.startDate.observe(
            viewLifecycleOwner
        ) { date ->
            binding.startDateValueText.text = date.toString()
        }
        viewModel.endDate.observe(
            viewLifecycleOwner
        ) {
            binding.endDateValueText.text = it.toString()
        }
        viewModel.syncingIntradayMetrics.observe(
            viewLifecycleOwner
        ) { syncing ->
            binding.intradaySyncProgressBar.visibility = when (syncing) {
                true -> View.VISIBLE
                false -> View.INVISIBLE
            }
        }

        viewModel.syncingSessions.observe(
            viewLifecycleOwner
        ) { syncing ->
            binding.sessionsSyncProgressBar.visibility = when (syncing) {
                true -> View.VISIBLE
                false -> View.INVISIBLE
            }
        }
        viewModel.syncingProfile.observe(
            viewLifecycleOwner
        ) { syncing ->
            binding.profileSyncProgressBar.visibility = when (syncing) {
                true -> View.VISIBLE
                false -> View.INVISIBLE
            }
        }
        viewModel.errorMessage.observe(
            viewLifecycleOwner,
            Observer {
                if (it == null) {
                    return@Observer
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage(it)
                    .show()
                viewModel.resetErrorMessage()
            }
        )

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
            viewModel.runIntradaySync(calories, heartRate, steps)
        }

        binding.runSessionsSyncButton.setOnClickListener {
            val minutesDurationText = binding.minSessionDurationTextEdit.text.toString()
            var minSessionDuration: Duration? = null
            try {
                val minutes = minutesDurationText.toInt()
                if (minutes > 0) {
                    minSessionDuration = Duration.ofMinutes(minutes.toLong())
                }
            } catch (exc: Exception) { }
            if (minSessionDuration == null) {
                return@setOnClickListener
            }
            viewModel.runSessionsSync(minSessionDuration)
        }

        binding.runProfileSyncButton.setOnClickListener {
            val height = binding.heightCheckBox.isChecked
            val weight = binding.weightCheckBox.isChecked
            viewModel.runProfileSync(height, weight)
        }
    }
}
