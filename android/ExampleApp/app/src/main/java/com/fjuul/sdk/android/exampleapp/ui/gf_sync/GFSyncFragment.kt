package com.fjuul.sdk.android.exampleapp.ui.gf_sync

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fjuul.sdk.android.exampleapp.R
import kotlinx.android.synthetic.main.gf_sync_fragment.*
import java.lang.Exception
import java.time.Duration
import java.time.LocalDate

class GFSyncFragment : Fragment() {

    companion object {
        fun newInstance() = GFSyncFragment()
    }

    private lateinit var viewModel: GFSyncViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.gf_sync_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GFSyncViewModel::class.java)

        (intraday_section_text as TextView).text = "Intraday metrics"
        (sessions_section_text as TextView).text = "Sessions"

        viewModel.startDate.observe(
            viewLifecycleOwner,
            Observer { date ->
                start_date_value_text.text = date.toString()
            }
        )
        viewModel.endDate.observe(
            viewLifecycleOwner,
            Observer {
                end_date_value_text.text = it.toString()
            }
        )
        viewModel.syncingIntradayMetrics.observe(
            viewLifecycleOwner,
            Observer { syncing ->
                intraday_sync_progress_bar.visibility = when (syncing) {
                    true -> View.VISIBLE
                    false -> View.INVISIBLE
                }
            }
        )

        viewModel.syncingSessions.observe(
            viewLifecycleOwner,
            Observer { syncing ->
                sessions_sync_progress_bar.visibility = when (syncing) {
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

        start_date_input_layout.setOnClickListener {
            // val date = model.startDate.value!!
            val date = viewModel.startDate.value ?: LocalDate.now()
            DatePickerDialog(
                requireContext(),
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    viewModel.setupDateRange(startDate = LocalDate.of(year, month + 1, dayOfMonth))
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }
        end_date_input_layout.setOnClickListener {
            // val date = model.startDate.value!!
            val date = viewModel.endDate.value ?: LocalDate.now()
            DatePickerDialog(
                requireContext(),
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    viewModel.setupDateRange(endDate = LocalDate.of(year, month + 1, dayOfMonth))
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }


        min_session_duration_text_edit.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus && min_session_duration_text_edit.text.isNullOrEmpty()) {
                min_session_duration_text_edit.setText("3")
            }
        }

        run_intraday_sync_button.setOnClickListener {
            val calories = calories_check_box.isChecked
            val steps = steps_check_box.isChecked
            val heartRate = heart_rate_check_box.isChecked
            viewModel.runIntradaySync(calories, heartRate, steps)
        }

        run_sessions_sync_button.setOnClickListener {
            val minutesDurationText = min_session_duration_text_edit.text.toString()
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
    }
}
