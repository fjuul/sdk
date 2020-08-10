package com.fjuul.sdk.android.exampleapp.ui.modules

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fjuul.sdk.android.exampleapp.R

// TODO: move these to own file
enum class ModuleItemName(val value: String) {
    PROFILE("Profile"),
    DAILY_STATS("Daily Stats")
}
sealed class ModulesListItem()
data class ModuleItem(val name: ModuleItemName) : ModulesListItem()
data class ModulesSection(val name: String) : ModulesListItem()

class ModulesFragment : Fragment() {
    private lateinit var modulesListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_modules, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        modulesListView = view.findViewById<ListView>(R.id.modules_list)
        val listItems = arrayListOf<ModulesListItem>(
            ModulesSection("User"),
            ModuleItem(ModuleItemName.PROFILE),
            ModulesSection("Analytics"),
            ModuleItem(ModuleItemName.DAILY_STATS)
        )
        val adapter = ModulesListAdapter(requireContext(), listItems)
        modulesListView.adapter = adapter
        modulesListView.setOnItemClickListener { parent, view, position, id ->
            val pressedItem = adapter.getItem(position)
            if (pressedItem is ModuleItem && pressedItem.name == ModuleItemName.DAILY_STATS) {
                val action = ModulesFragmentDirections.actionModulesFragmentToDailyStatsFragment()
                findNavController().navigate(action)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ModulesFragment().apply {
                arguments = Bundle()
            }
        private const val TAG = "MODULES_FRAGMENT"
    }
}

class ModulesListAdapter(
    private val context: Context,
    private val dataSource: ArrayList<ModulesListItem>
) : BaseAdapter() {
    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): ModulesListItem {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get view for row item
        val item = getItem(position)
        var rowView: View
        if (item is ModuleItem) {
            rowView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            val textView = rowView.findViewById<TextView>(android.R.id.text1)
            textView.text = item.name.value
        } else {
            rowView = inflater.inflate(R.layout.section_list_item, parent, false)
            val textView = rowView.findViewById<TextView>(R.id.section_text)
            textView.text = (item as ModulesSection).name
        }
        return rowView
    }
}
