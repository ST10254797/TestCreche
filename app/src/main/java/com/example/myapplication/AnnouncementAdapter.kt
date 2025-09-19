package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class AnnouncementAdapter(
    private val announcements: List<Announcement>,
    private val onItemClick: (Announcement) -> Unit   // ✅ non-nullable
) : RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {

    inner class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val message: TextView = itemView.findViewById(R.id.tvMessage)
        val createdBy: TextView = itemView.findViewById(R.id.tvCreatedBy)
        val timestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_announcement, parent, false)
        return AnnouncementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val announcement = announcements[position]

        holder.title.text = announcement.title
        holder.message.text = announcement.message
        holder.createdBy.text = "By: ${announcement.createdBy}"
        holder.timestamp.text = announcement.timestamp?.toDate()?.let { date ->
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
        } ?: ""

        // Get references for dynamic views
        val importantBadge: TextView? = holder.itemView.findViewById(R.id.tvImportant)

        if (announcement.important) {
            // Important announcement
            holder.itemView.setBackgroundResource(R.drawable.bg_card_important)
            importantBadge?.visibility = View.VISIBLE
            holder.title.setTextColor(Color.parseColor("#B91C1C")) // Dark red title
        } else {
            // Normal announcement
            holder.itemView.setBackgroundResource(R.drawable.bg_card_normal)
            importantBadge?.visibility = View.GONE
            holder.title.setTextColor(Color.parseColor("#0F172A")) // Standard dark title
        }

        // Click listener remains unchanged
        holder.itemView.setOnClickListener {
            onItemClick(announcement)
        }
    }


    override fun getItemCount() = announcements.size
}
