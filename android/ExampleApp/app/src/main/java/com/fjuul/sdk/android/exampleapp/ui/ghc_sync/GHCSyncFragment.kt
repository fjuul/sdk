package com.fjuul.sdk.android.exampleapp.ui.ghc_sync

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
import com.fjuul.sdk.android.exampleapp.databinding.GhcSyncFragmentBinding
import java.lang.Exception
import java.time.Duration
import java.time.LocalDate

class GHCSyncFragment : Fragment() {

    companion object {
        fun newInstance() = GHCSyncFragment()
    }

    private lateinit var viewModel: GHCSyncViewModel
    private lateinit var binding: GhcSyncFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Data binding added because kotlin extension is deprecated
        binding = GhcSyncFragmentBinding.bind(inflater.inflate(R.layout.ghc_sync_fragment, container, false))
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GHCSyncViewModel::class.java)

        binding.intradaySectionText.sectionText.text = "Intraday metrics"
        binding.sessionsSectionText.sectionText.text = "Sessions"
        binding.profileSectionText.sectionText.text = "Profile"

        viewModel.syncingIntradayMetrics.observe(
            viewLifecycleOwner,
            Observer { syncing ->
                binding.intradaySyncProgressBar.visibility = when (syncing) {
                    true -> View.VISIBLE
                    false -> View.INVISIBLE
                }
            }
        )

        viewModel.syncingSessions.observe(
            viewLifecycleOwner,
            Observer { syncing ->
                binding.sessionsSyncProgressBar.visibility = when (syncing) {
                    true -> View.VISIBLE
                    false -> View.INVISIBLE
                }
            }
        )
        viewModel.syncingProfile.observe(
            viewLifecycleOwner,
            Observer { syncing ->
                binding.profileSyncProgressBar.visibility = when (syncing) {
                    true -> View.VISIBLE
                    false -> View.INVISIBLE
                }
            }
        )
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

        binding.minSessionDurationTextEdit.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
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
