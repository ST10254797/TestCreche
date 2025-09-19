package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminNotificationFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationsList = mutableListOf<NotificationItem>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.rvNotifications)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        notificationAdapter = NotificationAdapter(notificationsList)
        recyclerView.adapter = notificationAdapter

        // Fetch notifications
        fetchNotifications()
    }

    private fun fetchNotifications() {
        notificationsList.clear()
        val now = System.currentTimeMillis()
        val maxAgeMillis = 14 * 24 * 60 * 60 * 1000L // 2 weeks

        // 1️⃣ Fetch Events
        firestore.collection("Events")
            .get()
            .addOnSuccessListener { eventResult ->
                for (doc in eventResult) {
                    val eventText = doc.getString("event") ?: ""
                    val eventDate = doc.id // This is the date of the event (from document ID)

                    val ts = when (val field = doc.get("createdAt")) {
                        is com.google.firebase.Timestamp -> field.toDate().time
                        is Long -> field
                        is String -> {
                            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                            sdf.parse(field)?.time ?: 0L
                        }
                        else -> 0L
                    }

                    val now = System.currentTimeMillis()
                    val maxAgeMillis = 14L * 24 * 60 * 60 * 1000 // 2 weeks
                    if (now - ts <= maxAgeMillis) { // Only recent events
                        val createdAt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                            .format(Date(ts))

                        notificationsList.add(
                            NotificationItem(
                                title = "Reminder!",
                                subtitle = "$eventText\nEvent Date: $eventDate", // Show event date here
                                date = createdAt,
                                type = NotificationType.EVENT,
                                timestamp = ts
                            )
                        )
                    }
                }

        // 2️⃣ Fetch Announcements
                firestore.collection("Announcements")
                    .get()
                    .addOnSuccessListener { announcementResult ->
                        for (doc in announcementResult) {
                            val title = doc.getString("title") ?: ""
                            val message = doc.getString("message") ?: ""
                            val important = doc.getBoolean("important") ?: false

                            val ts = when (val field = doc.get("timestamp")) {
                                is com.google.firebase.Timestamp -> field.toDate().time
                                is Long -> field
                                else -> 0L
                            }

                            if (now - ts <= maxAgeMillis) { // Only recent announcements
                                val createdAt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                    .format(Date(ts))

                                notificationsList.add(
                                    NotificationItem(
                                        title = title,
                                        subtitle = message,
                                        date = createdAt,
                                        type = NotificationType.ANNOUNCEMENT,
                                        timestamp = ts,
                                        isImportant = important
                                    )
                                )
                            }
                        }

                        // Sort by timestamp descending
                        notificationsList.sortByDescending { it.timestamp }

                        // Optional: group by week-year
                        // val grouped = notificationsList.groupBy { it.weekYear() }

                        notificationAdapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Failed to load announcements: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to load events: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}



