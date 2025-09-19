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
import com.google.firebase.firestore.FirebaseFirestore

class ClassListFragment : Fragment() {

    private lateinit var teacherSpinner: Spinner
    private lateinit var classListView: ListView
    private val teacherNames = mutableListOf<String>()
    private val teacherIds = mutableListOf<String>()
    private val displayChildren = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>


    //This calls the fragment into the FrameLayout//
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_class_list, container, false)
    }


    //This acts like an OnCreate class(Main class) in an activity
    //Use this as the main class for calling functions when doing fragments
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        teacherSpinner = view.findViewById(R.id.spinnerClassTeachers)
        classListView = view.findViewById(R.id.classListView)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayChildren)
        classListView.adapter = adapter

        loadTeachers()
    }


    private fun loadTeachers() {
        FirebaseFirestore.getInstance()
            .collection("Users")
            .whereEqualTo("role", "teacher")
            .get()
            .addOnSuccessListener { result ->
                teacherNames.clear()
                teacherIds.clear()

                for (doc in result) {
                    val name = doc.getString("email") ?: "Unknown"
                    teacherNames.add(name)
                    teacherIds.add(doc.id)
                }

                val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teacherNames)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                teacherSpinner.adapter = spinnerAdapter

                teacherSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedTeacherId = teacherIds[position]
                        loadClassList(selectedTeacherId)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load teachers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadClassList(teacherId: String) {
        FirebaseFirestore.getInstance()
            .collection("Child")
            .whereEqualTo("assignedTeacherId", teacherId)
            .get()
            .addOnSuccessListener { result ->
                displayChildren.clear()

                for (doc in result) {
                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    displayChildren.add("$firstName $lastName")
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load class list", Toast.LENGTH_SHORT).show()
            }
    }
}