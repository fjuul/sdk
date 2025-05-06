package com.fjuul.sdk.android.exampleapp.ui.activity_sources

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

enum class ActivitySourcesItem(val label: String) {
    FITBIT("Fitbit"),
    GARMIN("Garmin"),
    OURA("Oura"),
    POLAR("Polar"),
    GOOGLE_FIT("Google Fit"),
    HEALTH_CONNECT("Health Connect"),
    SUUNTO("Suunto"),
    WITHINGS("Withings"),
    DISCONNECT("Disconnect all"),
}

class ActivitySourcesListAdapter(
    context: Context,
    private val dataSource: ArrayList<ActivitySourcesItem>
) : BaseAdapter() {
    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): ActivitySourcesItem {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get view for row item
        val item = getItem(position)
        val rowView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        val textView = rowView.findViewById<TextView>(android.R.id.text1)
        textView.text = item.label
        return rowView
    }
}
