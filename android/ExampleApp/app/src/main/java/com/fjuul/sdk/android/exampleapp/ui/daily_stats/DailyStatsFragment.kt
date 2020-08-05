package com.fjuul.sdk.android.exampleapp.ui.daily_stats

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fjuul.sdk.android.exampleapp.R

class DailyStatsFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_daily_stats, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DailyStatsFragment().apply {
                arguments = Bundle()
            }
    }
}
