package com.fjuul.sdk.android.exampleapp.ui.create_user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.fjuul.sdk.android.exampleapp.R

class CreateUserFragment : Fragment() {
    val args: CreateUserFragmentArgs by navArgs()
    private lateinit var genderDropdown: AutoCompleteTextView
    private lateinit var birthdateInput: View
    private lateinit var birthdateText: TextView
    private lateinit var heightInput: EditText
    private lateinit var weightInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_menu_popup_item,
            GENDERS
        )
        genderDropdown = view.findViewById(R.id.gender_filled_exposed_dropdown)
        genderDropdown.keyListener = null
        genderDropdown.setAdapter(adapter)

        birthdateInput = view.findViewById(R.id.birthdate_block)
        birthdateText = view.findViewById(R.id.birthdate_input)
        heightInput = view.findViewById(R.id.height_input)
        weightInput = view.findViewById(R.id.weight_input)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            CreateUserFragment().apply {
                arguments = Bundle()
            }

        val GENDERS = arrayOf("MALE", "FEMALE", "OTHER")
    }
}
