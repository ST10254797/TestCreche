package com.example.myapplication

import com.google.firebase.Timestamp

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val createdBy: String = "",
    val timestamp: Timestamp? = null,
    val important: Boolean = false
)
