package com.fjuul.sdk.android.exampleapp.ui.healthconnectsync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.databinding.HealthConnectSyncFragmentBinding

class HealthConnectSyncFragment : Fragment() {
    private lateinit var viewModel: HealthConnectSyncViewModel
    private lateinit var binding: HealthConnectSyncFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HealthConnectSyncFragmentBinding.bind(
            inflater.inflate(
                R.layout.health_connect_sync_fragment,
                container,
                false
            )
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(HealthConnectSyncViewModel::class.java)

        binding.intradaySectionText.sectionText.text = "Intraday"
        binding.dailySectionText.sectionText.text = "Daily"
        binding.profileSectionText.sectionText.text = "Profile"

        viewModel.syncingIntradayData.observe(viewLifecycleOwner) { syncing ->
            binding.intradaySyncProgressBar.isVisible = syncing
        }
        viewModel.syncingDailyData.observe(viewLifecycleOwner) { syncing ->
            binding.dailySyncProgressBar.isVisible = syncing
        }
        viewModel.syncingProfileData.observe(viewLifecycleOwner) { syncing ->
            binding.profileSyncProgressBar.isVisible = syncing
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

        binding.runIntradaySyncButton.setOnClickListener {
            val сalories = binding.caloriesCheckBox.isChecked
            val heartRate = binding.heartRateCheckBox.isChecked
            viewModel.runIntradaySync(сalories, heartRate)
        }

        binding.runDailySyncButton.setOnClickListener {
            val steps = binding.stepsCheckBox.isChecked
            val restingHeartRate = binding.restingHeartRateCheckBox.isChecked
            viewModel.runDailySync(steps, restingHeartRate)
        }

        binding.runProfileSyncButton.setOnClickListener {
            val height = binding.heightCheckBox.isChecked
            val weight = binding.weightCheckBox.isChecked
            viewModel.runProfileSync(height, weight)
        }

        binding.btnClearAllChangesTokens.setOnClickListener {
            viewModel.clearAllChangesTokens()
        }
    }
}
