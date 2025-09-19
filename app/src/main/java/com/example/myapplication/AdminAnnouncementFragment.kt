package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class AdminAnnouncementFragment : Fragment() {


    private lateinit var titleEditText: EditText
    private lateinit var messageEditText: EditText
    private lateinit var importantCheckBox: CheckBox
    private lateinit var publishButton: Button
    private lateinit var cancelButton: Button // Made class-level
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnnouncementAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val announcements = mutableListOf<Announcement>()
    private var selectedAnnouncement: Announcement? = null // For updates



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_announcement, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleEditText = view.findViewById(R.id.etTitle)
        messageEditText = view.findViewById(R.id.etMessage)
        importantCheckBox = view.findViewById(R.id.cbImportant)
        publishButton = view.findViewById(R.id.btnPublish)
        cancelButton = view.findViewById(R.id.btnCancel)
        recyclerView = view.findViewById(R.id.recyclerAnnouncements)

        cancelButton.setOnClickListener {
            clearFields()
        }

        adapter = AnnouncementAdapter(announcements) { announcement ->
            val options = arrayOf("Edit", "Delete")

            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Choose Action")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> { // Edit
                            selectedAnnouncement = announcement
                            titleEditText.setText(announcement.title)
                            messageEditText.setText(announcement.message)
                            importantCheckBox.isChecked = announcement.important
                            publishButton.text = "Update Announcement"

                            cancelButton.visibility = View.VISIBLE
                        }
                        1 -> { // Delete with confirmation
                            android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Confirm Delete")
                                .setMessage("Are you sure you want to delete this announcement?")
                                .setPositiveButton("Yes") { _, _ ->
                                    deleteAnnouncement(announcement)
                                }
                                .setNegativeButton("No", null)
                                .show()
                        }
                    }
                }
                .show()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        publishButton.setOnClickListener {
            if (selectedAnnouncement == null) {
                createAnnouncement()
            } else {
                updateAnnouncement()
            }
        }

        loadAnnouncements()
    }


    private fun createAnnouncement() {
        val title = titleEditText.text.toString().trim()
        val message = messageEditText.text.toString().trim()
        val isImportant = importantCheckBox.isChecked

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val id = firestore.collection("Announcements").document().id
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        if (currentUserEmail == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("Users").document(currentUserEmail).get()
            .addOnSuccessListener { snapshot ->
                val firstName = snapshot.getString("firstName") ?: ""
                val lastName = snapshot.getString("lastName") ?: ""
                val adminName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                    "$firstName $lastName"
                } else {
                    currentUserEmail
                }

                val announcement = Announcement(
                    id = id,
                    title = title,
                    message = message,
                    createdBy = adminName,
                    timestamp = Timestamp.now(),
                    important = isImportant
                )

                firestore.collection("Announcements").document(id).set(announcement)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Announcement published", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch admin name", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteAnnouncement(announcement: Announcement) {
        firestore.collection("Announcements").document(announcement.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Announcement deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAnnouncement() {
        val announcement = selectedAnnouncement ?: return
        val title = titleEditText.text.toString().trim()
        val message = messageEditText.text.toString().trim()
        val isImportant = importantCheckBox.isChecked

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("Announcements").document(announcement.id)
            .update(
                mapOf(
                    "title" to title,
                    "message" to message,
                    "important" to isImportant
                )
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Announcement updated", Toast.LENGTH_SHORT).show()
                clearFields() // Cancel button will hide here
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        titleEditText.text.clear()
        messageEditText.text.clear()
        importantCheckBox.isChecked = false
        publishButton.text = "Publish"
        selectedAnnouncement = null
        cancelButton.visibility = View.GONE // <- hide Cancel button every time
    }

    private fun loadAnnouncements() {
        firestore.collection("Announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading announcements", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                announcements.clear()
                for (doc in snapshot!!.documents) {
                    val announcement = doc.toObject(Announcement::class.java)
                    if (announcement != null) {
                        announcements.add(announcement)
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

}