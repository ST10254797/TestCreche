package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(private val items: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        val tvEvent: TextView = itemView.findViewById(R.id.tvEvent)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = items[position]

        // Show "Reminder!" for events, otherwise title
        holder.tvEvent.text = if (item.type == NotificationType.EVENT) "Reminder!" else item.title
        holder.tvSubtitle.text = item.subtitle
        holder.tvCreatedAt.text = "Created at: ${item.date}"

        // Highlight important announcements
        if (item.type == NotificationType.ANNOUNCEMENT && item.isImportant) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFCDD2")) // Light red
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
        }

        // Dynamically set icon
        holder.ivIcon.setImageResource(
            if (item.type == NotificationType.ANNOUNCEMENT) R.drawable.ic_announcement
            else R.drawable.ic_notification_bell
        )
    }

    override fun getItemCount(): Int = items.size
}
