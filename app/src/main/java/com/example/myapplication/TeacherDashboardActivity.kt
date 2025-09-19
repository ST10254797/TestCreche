package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class TeacherDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("TEACHER_DASH", "TeacherDashboardActivity started")

        val user = FirebaseAuth.getInstance().currentUser
        Log.d("TEACHER_DASH", "Current user: ${user?.email ?: "null"}")

        if (user == null) {
            Log.e("TEACHER_DASH", "No authenticated user found. Redirecting to login.")
            startActivity(Intent(this, StartScreen::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_teacher_dashboard)

        val featureCard = findViewById<CardView>(R.id.teacher_feature_card)
        val teacherFragmentContainer = findViewById<FrameLayout>(R.id.teacher_fragment_container)
        val teacherBottomNav = findViewById<BottomNavigationView>(R.id.teacher_bottom_nav)


        //This hides the un-hides the FrameLayout which hold the fragment//
        fun openFragment(fragment: androidx.fragment.app.Fragment,
                         featureCard: CardView,
                         fragmentContainer: FrameLayout) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.teacher_fragment_container, fragment)
                .addToBackStack(null)
                .commit()
            featureCard.visibility = View.GONE
            fragmentContainer.visibility = View.VISIBLE
        }

        featureCard.visibility = View.VISIBLE
        teacherFragmentContainer.visibility = View.GONE


        //This allows the user to use the back button if they are in a fragment//
        onBackPressedDispatcher.addCallback(this) {
            if (teacherFragmentContainer.isVisible) {
                teacherFragmentContainer.visibility = View.GONE
                featureCard.visibility = View.VISIBLE
                supportFragmentManager.popBackStack()
            } else {
                // Default back behavior
                finish()
            }
        }




        // Bottom navigation
        teacherBottomNav.setOnItemSelectedListener {item ->
            when(item.itemId){
                R.id.home -> {
                    startActivity(Intent(this, TeacherDashboardActivity::class.java))
                    true
                }
//                R.id.messaging -> {
//
//                }
//                R.id.attendance -> {
//
//                }
//                R.id.media_sharing -> {
//
//                }
//                R.id.teacher_profile -> {
//
//                }
                else -> false
            }
        }

        //Notification for the events
        findViewById<ImageView>(R.id.notification_icon)?.setOnClickListener {
            openFragment(TeacherNotificationFragment(), featureCard, teacherFragmentContainer)
        }


        // Main feature buttons

        findViewById<LinearLayout>(R.id.btn_announcement)?.setOnClickListener {
            Log.d("TEACHER_DASH", "Messages button clicked")
            openFragment(TeacherAnnouncementsFragment(), featureCard, teacherFragmentContainer)
        }

        findViewById<LinearLayout>(R.id.btn_attendance)?.setOnClickListener {
            Log.d("TEACHER_DASH", "Attendance button clicked")
            openFragment(TeacherAttendanceFragment(), featureCard, teacherFragmentContainer)
        }
//
//        findViewById<LinearLayout>(R.id.btn_media)?.setOnClickListener {
//            Log.d("TEACHER_DASH", "Media button clicked")
//            startActivity(Intent(this, EventCalendarActivity::class.java))
//        }
//
//        findViewById<LinearLayout>(R.id.btn_messages)?.setOnClickListener {
//            Log.d("TEACHER_DASH", "Messages button clicked")
//            startActivity(Intent(this, TeacherAnnouncementActivity::class.java))
//        }


    }
}