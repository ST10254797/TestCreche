package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible

class StartScreen : AppCompatActivity() {

    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("START_SCREEN", "StartScreen onCreate started")
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_screen)

        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)

        fun openFragment(fragment: androidx.fragment.app.Fragment,
                         fragmentContainer: FrameLayout) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
            fragmentContainer.visibility = View.VISIBLE
        }

        onBackPressedDispatcher.addCallback(this) {
            if (fragmentContainer.isVisible) {
                fragmentContainer.visibility = View.GONE
                supportFragmentManager.popBackStack()
            } else {
                // Default back behavior
                finish()
            }
        }

        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        btnLogin.setOnClickListener {
            Log.d("START_SCREEN", "Login button clicked - navigating to LoginActivity")
            openFragment(LoginFragment(), fragmentContainer)
        }

        btnRegister.setOnClickListener {
            Log.d("START_SCREEN", "Register button clicked - navigating to RegisterActivity")
            openFragment(RegisterFragment(), fragmentContainer)
        }
    }
}
