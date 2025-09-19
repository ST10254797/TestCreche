package com.example.myapplication

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class AdminEventFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvEventText: TextView
    private lateinit var etAddEvent: EditText
    private lateinit var btnSubmitEvent: ImageView
    private lateinit var firestore: FirebaseFirestore
    private var selectedDate: CalendarDay = CalendarDay.today()
    private val eventDates = mutableSetOf<CalendarDay>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI references
        calendarView = view.findViewById(R.id.calendarView)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        tvEventText = view.findViewById(R.id.tvEventText)
        etAddEvent = view.findViewById(R.id.etAddEvent)
        btnSubmitEvent = view.findViewById(R.id.btnSubmitEvent)

        firestore = FirebaseFirestore.getInstance()

        // Default selected date
        selectedDate = CalendarDay.today()
        calendarView.selectedDate = selectedDate
        updateSelectedDateText(selectedDate)

        // Load all events from Firestore
        fetchAllEventDates()
        fetchEventForDate(selectedDate)

        // Date selection listener
        calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = date
            updateSelectedDateText(date)

            // Refresh decorators
            calendarView.removeDecorators()
            calendarView.addDecorator(SelectedDateDecorator(selectedDate))
            calendarView.addDecorator(EventDateDecorator(eventDates))

            fetchEventForDate(date)
        }

        // Add new event
        btnSubmitEvent.setOnClickListener {
            val eventText = etAddEvent.text.toString().trim()
            if (eventText.isNotEmpty()) {
                saveEventToFirebase(selectedDate, eventText)
            } else {
                Toast.makeText(requireContext(), "Please enter event details", Toast.LENGTH_SHORT).show()
            }
        }

        // Edit or delete existing event
        tvEventText.setOnClickListener {
            val currentText = tvEventText.text.toString()
            if (currentText != "//No Event//" && currentText != "//Error Loading Event//") {
                showEditDeleteDialog(selectedDate, currentText)
            }
        }
    }

    // ----------------- DATE DISPLAY -----------------
    private fun updateSelectedDateText(date: CalendarDay) {
        // Use CalendarDay's own date conversion
        val selectedDate: Date = date.date
        val sdf = SimpleDateFormat("d MMM EEE", Locale.getDefault())
        tvSelectedDate.text = sdf.format(selectedDate)
    }

    // ----------------- FIREBASE EVENTS -----------------
    private fun fetchEventForDate(date: CalendarDay) {
        val dateKey = formatDateKey(date)
        firestore.collection("Events")
            .document(dateKey)
            .get()
            .addOnSuccessListener { document ->
                tvEventText.text = if (document != null && document.exists()) {
                    document.getString("event") ?: "//No Event//"
                } else {
                    "//No Event//"
                }
            }
            .addOnFailureListener {
                tvEventText.text = "//Error Loading Event//"
            }
    }

    private fun fetchAllEventDates() {
        firestore.collection("Events")
            .get()
            .addOnSuccessListener { querySnapshot ->
                eventDates.clear()
                for (doc in querySnapshot.documents) {
                    val parts = doc.id.split("-")
                    if (parts.size == 3) {
                        val year = parts[0].toInt()
                        val month = parts[1].toInt()
                        val day = parts[2].toInt()
                        eventDates.add(CalendarDay.from(year, month - 1, day))
                    }
                }
                // Refresh decorators
                calendarView.removeDecorators()
                calendarView.addDecorator(SelectedDateDecorator(selectedDate))
                calendarView.addDecorator(EventDateDecorator(eventDates))
            }
    }

    private fun saveEventToFirebase(date: CalendarDay, eventText: String) {
        val dateKey = formatDateKey(date)
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserEmail == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("Users").document(currentUserEmail).get()
            .addOnSuccessListener { snapshot ->
                val firstName = snapshot.getString("firstName") ?: ""
                val lastName = snapshot.getString("lastName") ?: ""
                val adminName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) "$firstName $lastName" else "Unknown Admin"

                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                val currentDateTime = sdf.format(Date())

                val eventMap = hashMapOf(
                    "event" to eventText,
                    "createdAt" to currentDateTime,
                    "adminName" to adminName
                )

                firestore.collection("Events")
                    .document(dateKey)
                    .set(eventMap)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Event saved", Toast.LENGTH_SHORT).show()
                        etAddEvent.text.clear()
                        fetchEventForDate(date)
                        fetchAllEventDates()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch admin name", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteEvent(date: CalendarDay) {
        val dateKey = formatDateKey(date)
        firestore.collection("Events")
            .document(dateKey)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Event deleted", Toast.LENGTH_SHORT).show()
                fetchEventForDate(date)
                fetchAllEventDates()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDeleteDialog(date: CalendarDay, currentText: String) {
        val options = arrayOf("Edit Event", "Delete Event")
        AlertDialog.Builder(requireContext())
            .setTitle("Event Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> etAddEvent.setText(currentText)
                    1 -> deleteEvent(date)
                }
            }
            .show()
    }

    private fun formatDateKey(date: CalendarDay): String {
        return "${date.year}-${(date.month + 1).toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"
    }
}
