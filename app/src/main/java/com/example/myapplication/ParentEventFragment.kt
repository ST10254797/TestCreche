package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ParentEventFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvEventText: TextView
    private val firestore = FirebaseFirestore.getInstance()
    private val eventDates = mutableSetOf<CalendarDay>()
    private var selectedDate: CalendarDay = CalendarDay.today()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_parent_event, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarView = view.findViewById(R.id.calendarView)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        tvEventText = view.findViewById(R.id.tvEventText)

        // Default selected date
        selectedDate = CalendarDay.today()
        calendarView.selectedDate = selectedDate
        updateSelectedDateText(selectedDate)

        fetchAllEventDates()
        fetchEvent(selectedDate)

        // Add decorators
        calendarView.addDecorator(SelectedDateDecorator(selectedDate))
        calendarView.addDecorator(EventDateDecorator(eventDates))

        // Handle date selection
        calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = date
            updateSelectedDateText(selectedDate)

            // Refresh decorators
            calendarView.removeDecorators()
            calendarView.addDecorator(SelectedDateDecorator(selectedDate))
            calendarView.addDecorator(EventDateDecorator(eventDates))

            fetchEvent(date)
        }
    }

    // ----------------- SELECTED DATE DISPLAY -----------------
    private fun updateSelectedDateText(date: CalendarDay) {
        val calendar = Calendar.getInstance()
        calendar.set(date.year, date.month, date.day) // month is 0-based
        val formatted = SimpleDateFormat("d EEE", Locale.getDefault()).format(calendar.time)
        tvSelectedDate.text = formatted
    }

    // ----------------- FIREBASE EVENTS -----------------
    private fun fetchEvent(date: CalendarDay) {
        val dateKey = String.format("%04d-%02d-%02d", date.year, date.month + 1, date.day)
        firestore.collection("Events").document(dateKey).get()
            .addOnSuccessListener { doc ->
                tvEventText.text = doc.getString("event") ?: "//No Event//"
            }
            .addOnFailureListener {
                tvEventText.text = "//Error Loading Event//"
            }
    }

    private fun fetchAllEventDates() {
        firestore.collection("Events").get()
            .addOnSuccessListener { snapshot ->
                eventDates.clear()
                for (doc in snapshot.documents) {
                    val parts = doc.id.split("-")
                    if (parts.size == 3) {
                        try {
                            val year = parts[0].toInt()
                            val month = parts[1].toInt()
                            val day = parts[2].toInt()
                            eventDates.add(CalendarDay.from(year, month - 1, day))
                        } catch (_: Exception) { }
                    }
                }
                calendarView.invalidateDecorators()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load events", Toast.LENGTH_SHORT).show()
            }
    }
}
