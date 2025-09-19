package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.myapplication.databinding.FragmentAddAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class AddAdminFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: FragmentAddAdminBinding


    //This calls the fragment into the FrameLayout//
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    //This acts like an OnCreate class(Main class) in an activity
    //Use this as the main class for calling functions when doing fragments
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()


        binding.btnAddAdmin.setOnClickListener {
            val firstName = binding.AdminFirstName.text.toString()
            val lastName = binding.AdminLastName.text.toString()
            val email = binding.AdminEmail.text.toString()
            val number = binding.AdminContactNumber.text.toString()
            val pass = binding.AdminPassword.text.toString()
            val confirmPass = binding.AdminConfirmPassword.text.toString()

            //Logic for checking if the field is empty//
            if(email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty() &&
                firstName.isNotEmpty() && lastName.isNotEmpty() && number.isNotEmpty()){
                //Logic for checking if the passwords match//
                if(pass == confirmPass){
                    firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener{
                        if(it.isSuccessful){
                            //Calling the function which stores the parent info in firestore/
                            registerAdmin(firstName, lastName, email, number)
                            Toast.makeText(requireContext(), " Admin Registration successful", Toast.LENGTH_SHORT).show()

                            //Sends user to start page//
                            val intent = Intent(requireContext(), StartScreen::class.java)
                            startActivity(intent)
                        }else{
                            Toast.makeText(requireContext(), it.exception.toString() , Toast.LENGTH_SHORT).show()
                        }
                    }
                }else{
                    Toast.makeText(requireContext(), "Password does not match" , Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(requireContext(), "Empty fields" , Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun registerAdmin(firstName: String,lastName: String,email: String, number: String){
        val userType = "admin"
        val users = Users(
            firstName = firstName,
            lastName = lastName,
            email = email,
            number = number,
            role = userType
        )

        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(email)
            .set(users)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Admin Registration Created", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

}