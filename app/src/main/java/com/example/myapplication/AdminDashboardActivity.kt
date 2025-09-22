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
import com.google.firebase.firestore.FirebaseFirestore
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.widget.Toast

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ADMIN_DASH", "AdminDashboardActivity started")

        val user = FirebaseAuth.getInstance().currentUser
        Log.d("ADMIN_DASH", "Current user: ${user?.email ?: "null"}")

        if (user == null) {
            Log.e("ADMIN_DASH", "No authenticated user found. Redirecting to start screen.")
            startActivity(Intent(this, StartScreen::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_admin_dashboard)

        val featureCard = findViewById<CardView>(R.id.admin_feature_card)
        val adminFragmentContainer = findViewById<FrameLayout>(R.id.admin_fragment_container)
        val bottomNav = findViewById<BottomNavigationView>(R.id.admin_bottom_nav)

        // Function to open fragment
        fun openFragment(fragment: androidx.fragment.app.Fragment,
                         featureCard: CardView,
                         fragmentContainer: FrameLayout) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.admin_fragment_container, fragment)
                .addToBackStack(null)
                .commit()
            featureCard.visibility = View.GONE
            fragmentContainer.visibility = View.VISIBLE
        }

        featureCard.visibility = View.VISIBLE
        adminFragmentContainer.visibility = View.GONE

        // Back button handling
        onBackPressedDispatcher.addCallback(this) {
            if (adminFragmentContainer.isVisible) {
                adminFragmentContainer.visibility = View.GONE
                featureCard.visibility = View.VISIBLE
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        // Bottom nav listener
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    true
                }
                R.id.class_list -> {
                    openFragment(ClassListFragment(), featureCard, adminFragmentContainer)
                    true
                }
                R.id.reg_Approval -> {
                    openFragment(RegistrationApprovalFragment(), featureCard, adminFragmentContainer)
                    true
                }
                R.id.add_admin -> {
                    openFragment(AddAdminFragment(), featureCard, adminFragmentContainer)
                    true
                }
                R.id.add_teacher -> {
                    openFragment(AddTeacherFragment(), featureCard, adminFragmentContainer)
                    true
                }
                else -> false
            }
        }

        // Assign button
        findViewById<LinearLayout>(R.id.admin_btn_assign)?.setOnClickListener {
            openFragment(AssigningFragment(), featureCard, adminFragmentContainer)
        }

        // Announcement button
        findViewById<LinearLayout>(R.id.admin_btn_announcement)?.setOnClickListener {
            openFragment(AdminAnnouncementFragment(), featureCard, adminFragmentContainer)
        }

        // Event button
        findViewById<LinearLayout>(R.id.admin_btn_events)?.setOnClickListener {
            openFragment(AdminEventFragment(), featureCard, adminFragmentContainer)
        }

        // Notification icon
        findViewById<ImageView>(R.id.notification_icon)?.setOnClickListener {
            openFragment(AdminNotificationFragment(), featureCard, adminFragmentContainer)
        }

        // Attendance button
        findViewById<LinearLayout>(R.id.admin_btn_attendance)?.setOnClickListener {
            openFragment(AdminAttendanceFragment(), featureCard, adminFragmentContainer)
        }

        //Payment button
// Admin button opens PaymentActivity instead of showing a popup
        findViewById<LinearLayout>(R.id.admin_btn_payment)?.setOnClickListener {
            val intent = Intent(this, PaymentActivity::class.java)
            startActivity(intent)
        }

    }
}
