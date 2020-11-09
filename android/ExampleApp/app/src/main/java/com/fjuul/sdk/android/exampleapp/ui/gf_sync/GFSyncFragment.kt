package com.fjuul.sdk.android.exampleapp.ui.gf_sync

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fjuul.sdk.android.exampleapp.R

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
        // TODO: Use the ViewModel
    }
}
