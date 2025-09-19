package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.CalendarDay
import java.util.*

class AdminAttendanceFragment : Fragment() {

    private lateinit var spinnerTeachers: Spinner
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var attendanceListView: ListView
    private lateinit var submitButton: Button
    private lateinit var calendarContainer: LinearLayout
    private lateinit var toggleButton: ImageButton
    private var isCalendarVisible = true

    private val db = FirebaseFirestore.getInstance()
    private var selectedTeacherEmail: String? = null
    private var selectedDate = ""

    private val teacherNames = mutableListOf<String>()
    private val teacherEmails = mutableListOf<String>()

    private val studentList = mutableListOf<Child>()
    private val displayNames = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerTeachers = view.findViewById(R.id.spinnerTeachers)
        calendarView = view.findViewById(R.id.calendarView)
        attendanceListView = view.findViewById(R.id.attendanceListView)
        submitButton = view.findViewById(R.id.btnSubmitAttendance)
        calendarContainer = view.findViewById(R.id.calendarContainer)
        toggleButton = view.findViewById(R.id.btnToggleCalendar)

        adapter = ArrayAdapter(requireContext(), R.layout.list_item_student_modern, R.id.checkedTextView, displayNames)
        attendanceListView.adapter = adapter
        attendanceListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE


        // Add weekend decorator
        calendarView.addDecorators(WeekendDecorator())

        loadTeachers()

        spinnerTeachers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTeacherEmail = teacherEmails[position]
                loadStudents()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = "${date.year}-${(date.month + 1).toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"
            collapseCalendar()
            loadStudents()
        }

        toggleButton.setOnClickListener {
            if (isCalendarVisible) collapseCalendar() else expandCalendar()
        }

        submitButton.setOnClickListener { saveAttendance() }
    }

    private fun loadTeachers() {
        db.collection("Users")
            .whereEqualTo("role", "teacher")
            .get()
            .addOnSuccessListener { result ->
                teacherNames.clear()
                teacherEmails.clear()
                for (doc in result) {
                    val email = doc.getString("email") ?: "Unnamed"
                    teacherNames.add(email)
                    teacherEmails.add(email)
                }

                val spinnerAdapter = object : ArrayAdapter<String>(
                    requireContext(),
                    R.layout.spinner_item_teacher_modern,
                    teacherNames
                ) {
                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_dropdown_item_teacher_modern, parent, false)
                        val tv = view.findViewById<TextView>(R.id.tvTeacherDropdownItem)
                        tv.text = teacherNames[position]
                        return view
                    }

                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item_teacher_modern, parent, false)
                        val tv = view.findViewById<TextView>(R.id.tvTeacherItem)
                        tv.text = teacherNames[position]
                        return view
                    }
                }
                spinnerTeachers.adapter = spinnerAdapter

            }
    }

    private fun loadStudents() {
        if (selectedTeacherEmail == null || selectedDate.isEmpty()) {
            studentList.clear()
            displayNames.clear()
            adapter.notifyDataSetChanged()
            return
        }

        studentList.clear()
        displayNames.clear()
        adapter.notifyDataSetChanged()

        db.collection("Child")
            .whereEqualTo("assignedTeacherId", selectedTeacherEmail)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val child = doc.toObject(Child::class.java)
                    studentList.add(child)
                    displayNames.add("${child.firstName} ${child.lastName}")
                }
                adapter.notifyDataSetChanged()

                db.collection("Attendance")
                    .document(selectedDate)
                    .get()
                    .addOnSuccessListener { attDoc ->
                        val records = attDoc.get("Records") as? Map<String, String>
                        for (i in studentList.indices) {
                            val studentId = "${studentList[i].firstName}_${studentList[i].lastName}"
                            attendanceListView.setItemChecked(i, records?.get(studentId) == "Present")
                        }
                        adapter.notifyDataSetChanged()
                    }
            }
    }

    private fun saveAttendance() {
        if (selectedTeacherEmail == null || selectedDate.isEmpty()) {
            Toast.makeText(requireContext(), "Select a teacher and date first", Toast.LENGTH_SHORT).show()
            return
        }

        val records = mutableMapOf<String, String>()
        for (i in studentList.indices) {
            val studentId = "${studentList[i].firstName}_${studentList[i].lastName}"
            records[studentId] = if (attendanceListView.isItemChecked(i)) "Present" else "Absent"
        }

        db.collection("Attendance")
            .document(selectedDate)
            .set(mapOf("Records" to records))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Attendance saved", Toast.LENGTH_SHORT).show()
            }
    }

    private fun collapseCalendar() {
        calendarContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                calendarContainer.visibility = View.GONE
                toggleButton.setImageResource(android.R.drawable.arrow_down_float)
                isCalendarVisible = false
            }
    }

    private fun expandCalendar() {
        calendarContainer.visibility = View.VISIBLE
        calendarContainer.alpha = 0f
        calendarContainer.animate()
            .alpha(1f)
            .setDuration(300)
            .withEndAction {
                toggleButton.setImageResource(android.R.drawable.arrow_up_float)
                isCalendarVisible = true
            }
    }

    // WEEKEND DECORATOR: greyed out and unselectable
    inner class WeekendDecorator : DayViewDecorator {
        private val calendar = Calendar.getInstance()
        private val greyColor by lazy { ContextCompat.getColor(requireContext(), android.R.color.darker_gray) }

        override fun shouldDecorate(day: CalendarDay): Boolean {
            calendar.set(day.year, day.month, day.day)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        }

        override fun decorate(view: DayViewFacade) {
            view.setDaysDisabled(true) // unselectable
            view.addSpan(android.text.style.ForegroundColorSpan(greyColor)) // grey text
        }
    }
}
