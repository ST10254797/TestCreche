package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore


class AssigningFragment : Fragment() {

    private lateinit var teacherSpinner: Spinner
    private lateinit var childListView: ListView
    private lateinit var selectedTeacherId: String

    private val teacherNames = mutableListOf <String>()
    private val teacherIds = mutableListOf <String>()
    private val childList = mutableListOf <Child>()
    private val displayNames = mutableListOf <String>()
    private lateinit var adapter: ArrayAdapter<String>


    //This calls the fragment into the FrameLayout//
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_assigning, container, false)
    }


    //This acts like an OnCreate class(Main class) in an activity
    //Use this as the main class for calling functions when doing fragments
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        teacherSpinner = view.findViewById(R.id.teacherSpinner)
        childListView = view.findViewById(R.id.childListView)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayNames)
        childListView.adapter = adapter

        loadTeachers()
        setupChildAssignment()
    }



    private fun loadTeachers () {
        FirebaseFirestore.getInstance()
            .collection("Users")
            .whereEqualTo("role", "teacher")
            .get()
            .addOnSuccessListener {
                    result ->
                teacherNames.clear()
                teacherIds.clear()
                for (document in result) {
                    val name = document.getString("email") ?:"Unnamed"
                    teacherNames.add(name)
                    teacherIds.add(document.id)
                }

                val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teacherNames)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                teacherSpinner.adapter = spinnerAdapter

                teacherSpinner.onItemSelectedListener = object :AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent:AdapterView < * > ?, view:View ?, position:
                    Int, id:Long){
                        selectedTeacherId = teacherIds[position]
                        loadChildren()
                    }

                    override fun onNothingSelected(parent:AdapterView < * > ?){
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load teachers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadChildren () {
        FirebaseFirestore.getInstance()
            .collection("Child")
            .get()
            .addOnSuccessListener {
                    result ->
                childList.clear()
                displayNames.clear()

                for (document in result) {
                    val child = document.toObject(Child:: class.java)
                    val statusCheck = document.getString("status")

                    if(statusCheck == "approved"){
                        childList.add(child)

                        val isAssigned = child.assignedTeacherId == selectedTeacherId
                        val status = when {
                            child.assignedTeacherId == null ->"Unassigned"
                            isAssigned -> "✔ Assigned"
                            else ->"Assigned elsewhere"
                        }

                        displayNames.add("${child.firstName} ${child.lastName} ($status)")
                    }
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load children", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupChildAssignment () {
        childListView.setOnItemClickListener {
                _, _, position, _ ->
            val selectedChild = childList[position]
            val childId = "${selectedChild.firstName}_${selectedChild.lastName}"

            AlertDialog.Builder(requireContext())
                .setTitle("Assign Child")
                .setMessage("Assign ${selectedChild.firstName} to ${teacherNames[teacherIds.indexOf(selectedTeacherId)]}?")
                .setPositiveButton("Assign") {
                        _, _ ->
                    assignChildToTeacher(selectedChild, childId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun assignChildToTeacher (child:Child, documentId:String){
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("Child")
            .whereEqualTo("firstName", child.firstName)
            .whereEqualTo("lastName", child.lastName)
            .limit(1)

        query.get().addOnSuccessListener {
                documents ->
            if (!documents.isEmpty) {
                val docRef = documents.documents[0].reference
                docRef.update("assignedTeacherId", selectedTeacherId)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Assigned!", Toast.LENGTH_SHORT).show()
                        loadChildren()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to assign", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Child record not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}