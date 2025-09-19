package com.example.myapplication

data class Child(
    val firstName: String = "",
    val lastName: String = "",
    val dob: String = "",
    val address: String = "",
    val gender: String = "",
    val allergies: String = "",
    val allergiesDetail: String = "",
    val emergencyContact: String = "",
    val parentID: String = "",
    val parentEmail: String = "",
    val status: String = "pending", // NEW: for admin approval logic
    val assignedTeacherId: String? = null // NEW: for admin classroom assignment
)