package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val featureCard = findViewById<CardView>(R.id.feature_card)
        val parentFragmentContainer = findViewById<FrameLayout>(R.id.parent_fragment_container)
        val parentBottomNav = findViewById<BottomNavigationView>(R.id.parent_bottom_nav)


        //This hides the un-hides the FrameLayout which hold the fragment//
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


        //This allows the user to use the back button if they are in a fragment//
        onBackPressedDispatcher.addCallback(this) {
            if (parentFragmentContainer.isVisible) {
                parentFragmentContainer.visibility = View.GONE
                featureCard.visibility = View.VISIBLE
                supportFragmentManager.popBackStack()
            } else {
                // Default back behavior
                finish()
            }
        }


        //New bottom nav//
        parentBottomNav.setOnItemSelectedListener {item ->
            when(item.itemId){
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
//                R.id.messaging -> {
//
//                }
                R.id.add_child -> {
                    openFragment(AddChildFragment(), featureCard, parentFragmentContainer)
                    true
                }
//                R.id.lunch -> {
//
//                }
//                R.id.child_info -> {
//
//                }
                else -> false
            }
        }

        // Assign button in grid
        // Events button for viewing events
        findViewById<LinearLayout>(R.id.btn_events)?.setOnClickListener {
            openFragment(ParentEventFragment(), featureCard, parentFragmentContainer)
        }

        //Notification for the events
        findViewById<ImageView>(R.id.notification_icon)?.setOnClickListener {
            openFragment(ParentNotificationFragment(), featureCard, parentFragmentContainer)
        }


        findViewById<LinearLayout>(R.id.btn_announcement)?.setOnClickListener {
            openFragment(ParentAnnouncementFragment(), featureCard, parentFragmentContainer)
        }

        findViewById<LinearLayout>(R.id.btn_attendance)?.setOnClickListener {
            openFragment(ParentAttendanceFragment(), featureCard, parentFragmentContainer)
        }
    }
}
