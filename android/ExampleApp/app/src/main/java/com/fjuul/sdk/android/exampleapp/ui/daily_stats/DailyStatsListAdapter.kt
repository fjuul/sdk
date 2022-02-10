package com.fjuul.sdk.android.exampleapp.ui.daily_stats

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.fjuul.sdk.analytics.entities.DailyStats

class DailyStatsListAdapter(private val context: Context, var dataSource: Array<DailyStats>) :
    BaseAdapter() {
    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): DailyStats {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        var rowView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        val textView = rowView.findViewById<TextView>(android.R.id.text1)
        textView.text =
            """
|date: ${item.date};
|low: ${item.low.metMinutes} metMinutes;
|moderate: ${item.moderate.metMinutes} metMinutes;
|high: ${item.high.metMinutes} metMinutes;
|activeKcal: ${item.activeKcal} kcal;
|bmr: ${item.bmr};
|steps: ${item.steps}""".trimMargin()
        return rowView
    }
}
