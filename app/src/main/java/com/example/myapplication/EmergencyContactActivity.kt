package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityEmergencyContactBinding

class EmergencyContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactBinding
    private lateinit var contactAdapter: EmergencyContactAdapter
    private val contactList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEmergencyContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val existingContacts = intent.getStringArrayListExtra("CURRENT_CONTACTS")
        if (existingContacts != null) {
            contactList.addAll(existingContacts)
        }

        contactAdapter = EmergencyContactAdapter(contactList)
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = contactAdapter

        binding.btnAddEmergencyContact.setOnClickListener {
            val ecName = binding.ECfirstName.text.toString().trim()
            val ecNumber = binding.EClastName.text.toString().trim()

            if (ecName.isEmpty() || ecNumber.isEmpty()) {
                Toast.makeText(this, "Please enter both name and phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(ecNumber.length != 10){
                Toast.makeText(this, "Please enter the correct number format", Toast.LENGTH_SHORT).show()
                Toast.makeText(this, "Number must contain 10 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val combined = "$ecName - $ecNumber"
            contactList.add(combined)
            contactAdapter.notifyItemInserted(contactList.size - 1)

            // Clear inputs
            binding.ECfirstName.text.clear()
            binding.EClastName.text.clear()
        }

        binding.btnSaveAndReturn.setOnClickListener {
            val returnIntent = Intent()
            returnIntent.putStringArrayListExtra("CONTACT_LIST", ArrayList(contactList))
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }
}