package com.example.myapplication

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import java.text.SimpleDateFormat
import java.util.*

class ParentAttendanceFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private val studentList = mutableListOf<Child>()
    private val attendanceMap = mutableMapOf<String, Map<String, String>>() // date -> records

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_parent_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarView = view.findViewById(R.id.calendarView)
        calendarView.setSelectionColor(ContextCompat.getColor(requireContext(), R.color.purple_500))

        // Grey out weekends
        calendarView.addDecorators(WeekendDecorator())

        // Load data and decorate calendar immediately
        loadChildData()
    }

    private fun loadChildData() {
        if (currentUser == null) return

        db.collection("Child")
            .whereEqualTo("parentEmail", currentUser.email)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) return@addOnSuccessListener

                studentList.clear()
                for (doc in result) {
                    val child = doc.toObject(Child::class.java)
                    studentList.add(child)
                }

                loadAllAttendance()
            }
    }

    private fun loadAllAttendance() {
        db.collection("Attendance")
            .get()
            .addOnSuccessListener { result ->
                attendanceMap.clear()
                for (doc in result) {
                    val date = doc.id
                    val records = doc.get("Records") as? Map<String, String> ?: mapOf()
                    attendanceMap[date] = records
                }

                decorateCalendar()
            }
    }

    private fun decorateCalendar() {
        val decorators = mutableListOf<DayViewDecorator>()
        for ((dateStr, records) in attendanceMap) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = Calendar.getInstance().apply { time = sdf.parse(dateStr)!! }
            val calendarDay = CalendarDay.from(date)

            var color = android.R.color.transparent
            for (child in studentList) {
                val childId = "${child.firstName}_${child.lastName}"
                when (records[childId]) {
                    "Present" -> color = android.R.color.holo_green_dark
                    "Absent" -> color = android.R.color.holo_red_dark
                    else -> continue
                }
                break
            }

            decorators.add(object : DayViewDecorator {
                override fun shouldDecorate(day: CalendarDay) = day == calendarDay
                override fun decorate(view: DayViewFacade) {
                    val drawable = GradientDrawable()
                    drawable.shape = GradientDrawable.OVAL
                    drawable.setColor(ContextCompat.getColor(requireContext(), color))
                    drawable.setSize(20, 20)
                    view.setBackgroundDrawable(drawable)
                }
            })
        }

        calendarView.removeDecorators()
        for (dec in decorators) calendarView.addDecorator(dec)

        // Keep weekends decorator applied
        calendarView.addDecorators(WeekendDecorator())
    }

    // NEW: Decorator to blank out weekends
    inner class WeekendDecorator : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay?): Boolean {
            val cal = day?.calendar ?: return false
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        }

        override fun decorate(view: DayViewFacade?) {
            view?.setDaysDisabled(true)  // Disable weekends
        }
    }
}
