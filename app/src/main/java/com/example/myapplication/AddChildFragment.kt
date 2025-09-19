package com.example.myapplication

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentAddChildBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar


class AddChildFragment : Fragment() {

    private lateinit var binding: FragmentAddChildBinding
    private val contactList = mutableListOf<String>()
    private lateinit var contactAdapter: EmergencyContactAdapter


    private val contactResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contacts = result.data?.getStringArrayListExtra("CONTACT_LIST")
            if (contacts != null) {
                contactList.clear()
                contactList.addAll(contacts)
                contactAdapter.notifyDataSetChanged()
            }
        }
    }


    //This calls the fragment into the FrameLayout//
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddChildBinding.inflate(inflater, container, false)
        return binding.root
    }

    //This acts like an OnCreate class(Main class) in an activity
    //Use this as the main class for calling functions when doing fragments
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        allergiesSelectionDetails()
        genderSelectionDetails()
        dobCalendar()

        contactAdapter = EmergencyContactAdapter(contactList)
        binding.rvEmergencyContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEmergencyContacts.adapter = contactAdapter

        binding.btnAddEmergencyContact.setOnClickListener {
            val intent = Intent(requireContext(), EmergencyContactActivity::class.java)
            intent.putStringArrayListExtra("CURRENT_CONTACTS", ArrayList(contactList))
            contactResultLauncher.launch(intent)
        }

        binding.btnConfirm.setOnClickListener {
            val childFirstName = binding.childFirstName.text.toString().trim()
            val childLastName = binding.childLastName.text.toString().trim()
            val childDOB = binding.childDOB.text.toString().trim()
            val address = binding.Address.text.toString().trim()
            val childGender = binding.spinnerGender.selectedItem.toString().trim()
            val spinnerAllergies = binding.spinnerAllergies.selectedItem.toString()
            val childAllergiesDetail = binding.childAllergyDetails.text.toString().trim()
            val addressPattern = Regex("^[0-9]+\\s+[A-Za-z ]+$")

            val childEmergencyContact = contactList.joinToString(", ")


            if (childFirstName.isEmpty() || childLastName.isEmpty() || childDOB.isEmpty() ||
                address.isEmpty() || childGender == "Select" || spinnerAllergies == "Select" || childEmergencyContact.isEmpty()
            ) {

                Toast.makeText(requireContext(), "Please complete all required fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if(!address.matches(addressPattern)){
                binding.Address.error = "Format: 12 Example Street"
                return@setOnClickListener
            }


            if (spinnerAllergies == "Yes" && childAllergiesDetail.isEmpty()) {
                Toast.makeText(requireContext(), "Please provide allergy details.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalAllergyDetail = if (spinnerAllergies == "No") "None" else childAllergiesDetail


            registerChild(
                childFirstName,
                childLastName,
                childDOB,
                address,
                childGender,
                spinnerAllergies,
                finalAllergyDetail,
                childEmergencyContact
            )

            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun allergiesSelectionDetails() {
        val spinnerAllergies = binding.spinnerAllergies
        val etAllergyDetails = binding.childAllergyDetails

        val allergyOptions = arrayOf("Select", "Yes", "No")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, allergyOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAllergies.adapter = adapter

        spinnerAllergies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = parent.getItemAtPosition(position).toString()
                etAllergyDetails.visibility = if (selected == "Yes") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                etAllergyDetails.visibility = View.GONE
            }
        }
    }


    private fun dobCalendar(){

        binding.childDOB.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val dateString = "%02d/%02d/%04d".format(
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )
                    binding.childDOB.setText(dateString)
                },
                year,
                month,
                day
            )

            // Prevent selecting future dates
            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }
    }

    private fun genderSelectionDetails(){
        val spinnerGender = binding.spinnerGender

        val genderOptions = arrayOf("Select", "Male", "Female")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = adapter

        spinnerGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Toast.makeText(requireContext(), "Please select a gender", Toast.LENGTH_SHORT).show()

            }
        }
    }



    private fun registerChild(
        firstName: String,
        lastName: String,
        dob: String,
        address: String,
        gender: String,
        allergies: String,
        allergiesDetail: String,
        emergencyContact: String
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val email = FirebaseAuth.getInstance().currentUser?.email ?: "N/A"

        val child = Child(
            firstName = firstName,
            lastName = lastName,
            dob = dob,
            address = address,
            gender = gender,
            allergies = allergies,
            allergiesDetail = allergiesDetail,
            emergencyContact = emergencyContact,
            parentID = uid,
            parentEmail = email,
            status = "pending",
            assignedTeacherId = null
        )

        val db = FirebaseFirestore.getInstance()
        val requestId = db.collection("Child").document().id

        db.collection("Child")
            .document(requestId)
            .set(child)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Request sent for admin approval", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

}