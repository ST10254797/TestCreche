package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val featureCard = findViewById<CardView>(R.id.feature_card)
        val parentFragmentContainer = findViewById<FrameLayout>(R.id.parent_fragment_container)
        val parentBottomNav = findViewById<BottomNavigationView>(R.id.parent_bottom_nav)

        // Helper function to open a fragment
        fun openFragment(fragment: androidx.fragment.app.Fragment,
                         featureCard: CardView,
                         fragmentContainer: FrameLayout) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.parent_fragment_container, fragment)
                .addToBackStack(null)
                .commit()
            featureCard.visibility = View.GONE
            fragmentContainer.visibility = View.VISIBLE
        }

        featureCard.visibility = View.VISIBLE
        parentFragmentContainer.visibility = View.GONE

        // Back button behavior for fragments
        onBackPressedDispatcher.addCallback(this) {
            if (parentFragmentContainer.isVisible) {
                parentFragmentContainer.visibility = View.GONE
                featureCard.visibility = View.VISIBLE
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        // Bottom navigation
        parentBottomNav.setOnItemSelectedListener { item ->
            when(item.itemId){
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.add_child -> {
                    openFragment(AddChildFragment(), featureCard, parentFragmentContainer)
                    true
                }
                else -> false
            }
        }

        // Assign buttons in grid
        findViewById<LinearLayout>(R.id.btn_events)?.setOnClickListener {
            openFragment(ParentEventFragment(), featureCard, parentFragmentContainer)
        }

        findViewById<ImageView>(R.id.notification_icon)?.setOnClickListener {
            openFragment(ParentNotificationFragment(), featureCard, parentFragmentContainer)
        }

        findViewById<LinearLayout>(R.id.btn_announcement)?.setOnClickListener {
            openFragment(ParentAnnouncementFragment(), featureCard, parentFragmentContainer)
        }

        findViewById<LinearLayout>(R.id.btn_attendance)?.setOnClickListener {
            openFragment(ParentAttendanceFragment(), featureCard, parentFragmentContainer)
        }

        // ---- Payment button ----
        findViewById<LinearLayout>(R.id.btn_payment)?.setOnClickListener {
            val parentEmail = FirebaseAuth.getInstance().currentUser?.email
            if (parentEmail.isNullOrEmpty()) {
                Toast.makeText(this, "Parent not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()

            // Step 1: Get the first child for this parent
            db.collection("Child")
                .whereEqualTo("parentEmail", parentEmail)
                .get()
                .addOnSuccessListener { childSnapshot ->
                    if (childSnapshot.isEmpty) {
                        Toast.makeText(this, "No child found for this parent", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val childDoc = childSnapshot.documents[0]
                    val childId = childDoc.id

                    // Step 2: Get first pending fee for this child
                    db.collection("Child").document(childId)
                        .collection("Fees")
                        .whereEqualTo("paymentStatus", "PENDING")
                        .get()
                        .addOnSuccessListener { feeSnapshot ->
                            if (feeSnapshot.isEmpty) {
                                // No pending fees — create one automatically
                                val newFee = hashMapOf(
                                    "type" to "MONTHLY",           // or "ONE_TIME"
                                    "description" to "School Fees",
                                    "amount" to 2000,              // example amount
                                    "dueDate" to "2025-09-30",     // example due date
                                    "paymentStatus" to "PENDING",
                                    "createdAt" to com.google.firebase.Timestamp.now()
                                )
                                db.collection("Child").document(childId)
                                    .collection("Fees")
                                    .add(newFee)
                                    .addOnSuccessListener { feeDocRef ->
                                        openPaymentScreen(childId, feeDocRef.id, parentEmail)
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Failed to create fee: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                val feeDoc = feeSnapshot.documents[0]
                                openPaymentScreen(childId, feeDoc.id, parentEmail)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to fetch fees: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to fetch child: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    } // end of onCreate

    // Private member function to launch ParentPaymentActivity
    private fun openPaymentScreen(childId: String, feeId: String, parentEmail: String) {
        val intent = Intent(this, ParentPaymentActivity::class.java)
        intent.putExtra("childId", childId)
        intent.putExtra("feeId", feeId)
        intent.putExtra("parentEmail", parentEmail)
        startActivity(intent)
    }
}
