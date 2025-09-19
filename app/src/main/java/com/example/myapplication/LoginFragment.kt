package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import com.example.myapplication.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore


class LoginFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: FragmentLoginBinding


    //This calls the fragment into the FrameLayout//
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }


    //This acts like an OnCreate class(Main class) in an activity
    //Use this as the main class for calling functions when doing fragments
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        // Password reset
        binding.forgotPassword.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            val view = layoutInflater.inflate(R.layout.forgot_password, null)
            val userEmail = view.findViewById<EditText>(R.id.resetEmail)

            builder.setView(view)
            val forgetPass = builder.create()

            view.findViewById<Button>(R.id.btnResetPassword).setOnClickListener {
                compareEmail(userEmail)
                forgetPass.dismiss()
            }
            view.findViewById<TextView>(R.id.backToLogin).setOnClickListener {
                forgetPass.dismiss()
            }

            if (forgetPass.window != null) {
                forgetPass.window!!.setBackgroundDrawable(0.toDrawable())
            }
            forgetPass.show()
        }

        // Redirect to register
        binding.RegisterRedirect.setOnClickListener {
            val registerFragment = RegisterFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, registerFragment) // your host container id
                .addToBackStack(null) // so user can go back
                .commit()
        }

        // Login click
        binding.btnLogin.setOnClickListener {
            val email = binding.LoginEmail.text.toString()
            val pass = binding.password.text.toString()

            Log.d("LOGIN_FLOW", "Login button clicked with email: $email")

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Log.d("LOGIN_FLOW", "Firebase login SUCCESS for $email")
                            Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
                            validatingRoleOfUser(email, pass)
                        } else {
                            Log.e("LOGIN_FLOW", "Firebase login FAILED", it.exception)

                            val errorMessage = when ((it.exception as? FirebaseAuthException)?.errorCode) {
                                "ERROR_INVALID_EMAIL" -> "Invalid email or password. Please try again."
                                "ERROR_WRONG_PASSWORD" -> "Incorrect email or password. Please try again."
                                "ERROR_USER_NOT_FOUND" -> "No account found with this email."
                                "ERROR_USER_DISABLED" -> "This account has been disabled."
                                "ERROR_TOO_MANY_REQUESTS" -> "Too many login attempts. Please try again later."
                                else -> "Login failed. Please check your details and try again."
                            }

                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Log.w("LOGIN_FLOW", "Empty fields - email or password missing")
                Toast.makeText(requireContext(), "Empty fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun compareEmail(email: EditText) {
        if (email.text.toString().isEmpty()) {
            Log.w("PASSWORD_RESET", "Empty email field")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            Log.w("PASSWORD_RESET", "Invalid email format: ${email.text}")
            return
        }
        firebaseAuth.sendPasswordResetEmail(email.text.toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("PASSWORD_RESET", "Password reset email sent to ${email.text}")
                    Toast.makeText(requireContext(), "Check your spam in email", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("PASSWORD_RESET", "Failed to send reset email", task.exception)
                }
            }
    }

    private fun validatingRoleOfUser(email: String, pass: String) {
        Log.d("ROLE_CHECK", "Fetching role for user: $email")

        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userType = document.getString("role").toString()
                    Log.d("ROLE_CHECK", "Role from DB: $userType")

                    when (userType.lowercase()) {
                        "parent" -> {
                            Log.d("ROLE_CHECK", "Navigating to Parent dashboard")
                            startActivity(Intent(requireContext(), MainActivity::class.java))
                        }
                        "admin" -> {
                            Log.d("ROLE_CHECK", "Navigating to Admin dashboard")
                            startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
                        }
                        "teacher" -> {
                            Log.d("ROLE_CHECK", "Navigating to Teacher dashboard")
                            startActivity(Intent(requireContext(), TeacherDashboardActivity::class.java))
                        }
                        else -> {
                            Log.e("ROLE_CHECK", "Unknown role type: $userType")
                            Toast.makeText(requireContext(), "User role not recognized", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    Log.e("ROLE_CHECK", "User document for $email does not exist")
                    Toast.makeText(requireContext(), "User document not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e("ROLE_CHECK", "Error fetching role for $email", it)
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

}