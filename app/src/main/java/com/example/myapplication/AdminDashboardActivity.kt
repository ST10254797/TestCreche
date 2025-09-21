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
        // Payment button (Admin can create a new fee for a child)
        findViewById<LinearLayout>(R.id.admin_btn_payment)?.setOnClickListener {
            val firestore = FirebaseFirestore.getInstance()

            firestore.collection("Child")
                .limit(1) // just get one child for testing
                .get()
                .addOnSuccessListener { childrenSnapshot ->
                    val childDoc = childrenSnapshot.documents.firstOrNull()
                    if (childDoc == null) {
                        Log.e("ADMIN_DASH", "No children found.")
                        return@addOnSuccessListener
                    }

                    val childId = childDoc.id
                    val email = childDoc.getString("parentEmail") ?: ""
                    if (email.isEmpty()) {
                        Log.e("ADMIN_DASH", "Child email missing.")
                        return@addOnSuccessListener
                    }

                    // Get the first fee for this child (just for description reference)
                    childDoc.reference.collection("Fees")
                        .limit(1)
                        .get()
                        .addOnSuccessListener { feeSnapshot ->
                            val feeDoc = feeSnapshot.documents.firstOrNull()
                            val description = feeDoc?.getString("description") ?: "School Fee"

                            // Show dialog to let admin enter the amount for new fee
                            val input = EditText(this)
                            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                            input.hint = "Enter amount"

                            AlertDialog.Builder(this)
                                .setTitle("Create New Fee")
                                .setMessage("Enter amount for $description")
                                .setView(input)
                                .setCancelable(true)
                                .setPositiveButton("Save") { _, _ ->
                                    val newAmount = input.text.toString().toDoubleOrNull()
                                    if (newAmount != null && newAmount > 0) {
                                        // Create a new fee instead of updating
                                        val newFee = hashMapOf(
                                            "amount" to newAmount,
                                            "description" to description,
                                            "createdAt" to com.google.firebase.Timestamp.now(),
                                            "amountPaid" to 0.0,
                                            "paymentStatus" to "PENDING",
                                            "paymentType" to "FULL" // admin default
                                        )

                                        childDoc.reference.collection("Fees")
                                            .add(newFee)
                                            .addOnSuccessListener { docRef ->
                                                Log.d("ADMIN_DASH", "New fee created successfully with ID ${docRef.id}")
                                                Toast.makeText(this, "New fee created successfully", Toast.LENGTH_SHORT).show()
                                                // Optionally: initiate payment flow here using docRef.id
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("ADMIN_DASH", "Failed to create new fee: ${e.message}")
                                                Toast.makeText(this, "Failed to create new fee", Toast.LENGTH_LONG).show()
                                            }

                                    } else {
                                        Log.e("ADMIN_DASH", "Invalid amount entered")
                                        Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ADMIN_DASH", "Failed to fetch fees: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("ADMIN_DASH", "Failed to fetch children: ${e.message}")
                }
        }


    }
}
