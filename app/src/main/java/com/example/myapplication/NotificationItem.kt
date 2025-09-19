package com.example.myapplication

enum class NotificationType {
    EVENT,
    ANNOUNCEMENT
}

data class NotificationItem(
    val title: String,          // Event title or Announcement title
    val subtitle: String? = "", // Event description or Announcement message
    val date: String,           // Formatted date string
    val timestamp: Long,
    val type: NotificationType, // Event or Announcement
    val isImportant: Boolean = false
)

