package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.myapplication.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class RegisterFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: FragmentRegisterBinding


    //This calls the fragment into the FrameLayout//
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }


    //This acts like an OnCreate class(Main class) in an activity
    //Use this as the main class for calling functions when doing fragments
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loginFragment = LoginFragment()

        firebaseAuth = FirebaseAuth.getInstance()

        binding.LoginRedirect.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, loginFragment) // your host container id
                .addToBackStack(null) // so user can go back
                .commit()
        }


        binding.btnRegister.setOnClickListener {
            val firstName = binding.firstName.text.toString()
            val lastName = binding.lastName.text.toString()
            val email = binding.email.text.toString()
            val number = binding.contactNumber.text.toString()
            val pass = binding.password.text.toString()
            val confirmPass = binding.confirmPassword.text.toString()

            //Logic for checking if the field is empty//
            if(email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty() &&
                firstName.isNotEmpty() && lastName.isNotEmpty() && number.isNotEmpty()){
                if(number.length == 10){
                    //Logic for checking if the passwords match//
                    if(pass == confirmPass){
                        firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener{
                            if(it.isSuccessful){
                                //Calling the function which stores the parent info in firestore/
                                registerParent(firstName, lastName, email, number)
                                Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()

                                //Redirects user to login page//
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, loginFragment) // your host container id
                                    .addToBackStack(null) // so user can go back
                                    .commit()
                            }
                            else{
                                Toast.makeText(requireContext(), it.exception.toString() , Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else{
                        Toast.makeText(requireContext(), "Password does not match" , Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    Toast.makeText(requireContext(),"Number format incorrect!!", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(requireContext(), "Empty fields" , Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun registerParent(firstName: String,lastName: String,email: String, number: String){
        val userType = "parent"
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
                Toast.makeText(requireContext(), "Parent Registration Created", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

}