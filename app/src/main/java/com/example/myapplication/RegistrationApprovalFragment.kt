package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore


class RegistrationApprovalFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val childNames = mutableListOf<String>()
    private val childDocIds = mutableListOf<String>()


    //This calls the fragment into the FrameLayout//
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registration_approval, container, false)
    }


    //This acts like an OnCreate class(Main class) in an activity
    //Use this as the main class for calling functions when doing fragments
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.listViewApprovals)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, childNames)
        listView.adapter = adapter

        loadPendingRequests()

        listView.setOnItemClickListener { _, _, position, _ ->
            val docId = childDocIds[position]
            showApprovalDialog(docId)
        }
    }

    private fun loadPendingRequests() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Child")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                childNames.clear()
                childDocIds.clear()

                for (doc in snapshots!!) {
                    val child = doc.toObject(Child::class.java)
                    val fullName = "${child.firstName} ${child.lastName}"
                    childNames.add(fullName)
                    childDocIds.add(doc.id)
                }

                adapter.notifyDataSetChanged()
            }
    }

    private fun showApprovalDialog(docId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Approve or Reject")
            .setMessage("Do you want to approve or reject this registration?")
            .setPositiveButton("Approve") { _, _ -> approve(docId) }
            .setNegativeButton("Reject") { _, _ -> reject(docId) }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun approve(docId: String) {
        val db = FirebaseFirestore.getInstance()
        val requestRef = db.collection("Child").document(docId)

        requestRef.get().addOnSuccessListener { docSnapshot ->
            val child = docSnapshot.toObject(Child::class.java)

            if (child != null) {
                // Save to /children collection
                db.collection("Child")
                    .document(docId)
                    .set(child)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "child approved", Toast.LENGTH_SHORT).show()

                        // Update request status to "approved"
                        requestRef.update("status", "approved")
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to approve: ${it.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
            } else {
                Toast.makeText(requireContext(), "Child data missing", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error retrieving request", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reject(docId: String) {
        val db = FirebaseFirestore.getInstance()
        val requestRef = db.collection("Child").document(docId)

        requestRef.get().addOnSuccessListener { docSnapshot ->
            val child = docSnapshot.toObject(Child::class.java)

            if (child != null) {
                // Save to /children collection
                db.collection("Child")
                    .document(docId)
                    .set(child)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "child rejected", Toast.LENGTH_SHORT).show()

                        // Update request status to "approved"
                        requestRef.update("status", "rejected")
                            .addOnSuccessListener {
                                db.collection("Child").document(docId).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "child removed from database", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(requireContext(), "Failed to removed from database:", Toast.LENGTH_SHORT).show()
                                    }
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to rejected: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Child data missing", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error retrieving request", Toast.LENGTH_SHORT).show()
        }
    }
}