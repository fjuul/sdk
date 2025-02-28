package com.fjuul.sdk.android.exampleapp.ui.hc_sync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.databinding.HcSyncFragmentBinding

class HCSyncFragment : Fragment() {

    companion object {
        fun newInstance() = HCSyncFragment()
    }

    private lateinit var viewModel: HCSyncViewModel
    private lateinit var binding: HcSyncFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Data binding added because kotlin extension is deprecated
        binding = HcSyncFragmentBinding.bind(inflater.inflate(R.layout.hc_sync_fragment, container, false))
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(HCSyncViewModel::class.java)

        binding.intradaySectionText.sectionText.text = "Intraday metrics"
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

        binding.runIntradaySyncButton.setOnClickListener {
            val calories = binding.caloriesCheckBox.isChecked
            val steps = binding.stepsCheckBox.isChecked
            val heartRate = binding.heartRateCheckBox.isChecked
            viewModel.runIntradaySync(calories, heartRate, steps)
        }

        binding.runProfileSyncButton.setOnClickListener {
            val height = binding.heightCheckBox.isChecked
            val weight = binding.weightCheckBox.isChecked
            viewModel.runProfileSync(height, weight)
        }
    }
}
