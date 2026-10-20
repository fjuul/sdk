package com.fjuul.sdk.android.exampleapp.ui.modules

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.fjuul.sdk.android.exampleapp.R
import com.fjuul.sdk.android.exampleapp.data.AuthorizedUserDataViewModel
import com.fjuul.sdk.android.exampleapp.data.model.ApiClientHolder
import com.fjuul.sdk.android.exampleapp.ui.user_profile.UserProfileNavigationFlow

// TODO: move these to own file
enum class ModuleItemName(val value: String) {
    PROFILE("Profile"),
    ACTIVITY_SOURCES("Activity Sources"),
    GF_SYNC("Google Fit synchronization"),
    HEALTH_CONNECT_SYNC("Health Connect synchronization"),
    DAILY_STATS("Daily Stats"),
    AGGREGATED_DAILY_STATS("Aggregated Daily Stats"),
    LOGOUT("Logout")
}
sealed class ModulesListItem
data class ModuleItem(val name: ModuleItemName) : ModulesListItem()
data class ModulesSection(val name: String) : ModulesListItem()

class ModulesFragment : Fragment() {
    private lateinit var modulesListView: ListView
    private var logoutBackCallback: OnBackPressedCallback? = null
    private val authorizedUserDataViewModel: AuthorizedUserDataViewModel by activityViewModels()

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
        modulesListView = view.findViewById(R.id.modules_list)
        logoutBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = Unit
        }.also { requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it) }
        authorizedUserDataViewModel.logoutResult.observe(viewLifecycleOwner) { result ->
            updateModuleNavigationEnabled()
            if (result != null && !isModulesDestinationActive(result.destinationId)) {
                authorizedUserDataViewModel.consumeLogoutResult()
            } else if (result != null) {
                if (result.backgroundWorkError != null) {
                    val retryDialog = AlertDialog.Builder(requireContext())
                        .setTitle("Can't stop background synchronization")
                        .setMessage(result.backgroundWorkError.message ?: "Logout is still pending.")
                        .setPositiveButton("Retry") { _, _ ->
                            authorizedUserDataViewModel.retryLogoutCleanup()
                        }
                        .setCancelable(false)
                        .create()
                    retryDialog.setCanceledOnTouchOutside(false)
                    retryDialog.show()
                } else if (!result.storageRemoved) {
                    val retryDialog = AlertDialog.Builder(requireContext())
                        .setTitle("Can't remove the persistent storage")
                        .setMessage("Retry storage cleanup to finish logout.")
                        .setPositiveButton("Retry") { _, _ ->
                            authorizedUserDataViewModel.retryLogoutCleanup()
                        }
                        .setCancelable(false)
                        .create()
                    retryDialog.setCanceledOnTouchOutside(false)
                    retryDialog.show()
                } else {
                    authorizedUserDataViewModel.consumeLogoutResult()
                    findNavController().popBackStack()
                }
            }
        }
        val listItems = arrayListOf(
            ModulesSection("User"),
            ModuleItem(ModuleItemName.PROFILE),
            ModuleItem(ModuleItemName.ACTIVITY_SOURCES),
            ModuleItem(ModuleItemName.GF_SYNC),
            ModuleItem(ModuleItemName.HEALTH_CONNECT_SYNC),
            ModulesSection("Analytics"),
            ModuleItem(ModuleItemName.DAILY_STATS),
            ModuleItem(ModuleItemName.AGGREGATED_DAILY_STATS),
            ModulesSection("Exit"),
            ModuleItem(ModuleItemName.LOGOUT)
        )
        val adapter = ModulesListAdapter(requireContext(), listItems)
        modulesListView.adapter = adapter
        authorizedUserDataViewModel.logoutInProgress.observe(viewLifecycleOwner) {
            updateModuleNavigationEnabled()
        }
        modulesListView.setOnItemClickListener { _, _, position, _ ->
            if (!updateModuleNavigationEnabled()) {
                return@setOnItemClickListener
            }
            val pressedItem = adapter.getItem(position)
            if (pressedItem is ModuleItem) {
                when (pressedItem.name) {
                    ModuleItemName.DAILY_STATS -> {
                        val action = ModulesFragmentDirections.actionModulesFragmentToDailyStatsFragment()
                        findNavController().navigate(action)
                    }
                    ModuleItemName.AGGREGATED_DAILY_STATS -> {
                        val action = ModulesFragmentDirections.actionModulesFragmentToAggregatedDailyStatsFragment()
                        findNavController().navigate(action)
                    }
                    ModuleItemName.PROFILE -> {
                        val action = ModulesFragmentDirections.actionModulesFragmentToUserProfileFragment(UserProfileNavigationFlow.UPDATE)
                        findNavController().navigate(action)
                    }
                    ModuleItemName.ACTIVITY_SOURCES -> {
                        val action = ModulesFragmentDirections.actionModulesFragmentToActivitySourcesFragment()
                        findNavController().navigate(action)
                    }
                    ModuleItemName.GF_SYNC -> {
                        val action = ModulesFragmentDirections.actionModulesFragmentToGFSyncFragment()
                        findNavController().navigate(action)
                    }
                    ModuleItemName.HEALTH_CONNECT_SYNC -> {
                        val action = ModulesFragmentDirections.actionModulesFragmentToHealthConnectSyncFragment()
                        findNavController().navigate(action)
                    }
                    ModuleItemName.LOGOUT -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Are you sure?")
                            .setNegativeButton("No") { dialog, _ ->
                                dialog.dismiss()
                            }.setPositiveButton("Logout") { dialog, _ ->
                                val clientAtLogout = ApiClientHolder.sdkClient
                                val destinationId = findNavController().currentBackStackEntry?.id
                                    ?: return@setPositiveButton
                                authorizedUserDataViewModel.logout(
                                    clientAtLogout,
                                    requireContext().applicationContext,
                                    destinationId
                                )
                                dialog.dismiss()
                            }.show()
                    }
                }
            }
        }
    }

    private fun isModulesDestinationActive(destinationId: String): Boolean =
        isAdded
            && viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            && findNavController().currentBackStackEntry?.id == destinationId
            && findNavController().currentDestination?.id == R.id.modulesFragment

    private fun updateModuleNavigationEnabled(): Boolean {
        val logoutPending = authorizedUserDataViewModel.logoutInProgress.value == true
            || authorizedUserDataViewModel.logoutResult.value != null
        val enabled = !logoutPending
        logoutBackCallback?.isEnabled = logoutPending
        if (::modulesListView.isInitialized) {
            modulesListView.isEnabled = enabled
        }
        return enabled
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
    context: Context,
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
        val rowView: View
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
