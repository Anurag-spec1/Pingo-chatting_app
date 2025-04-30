package com.anurag.chatese

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView




class RecyclerViewAdapter(
    private val userList: ArrayList<UserModel>,
    private val currentUid: String,
    private val onUserClick: (UserModel) -> Unit
) : RecyclerView.Adapter<RecyclerViewAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val statusIndicator: ImageView = itemView.findViewById(R.id.statusIndicator)
        val statusText: TextView = itemView.findViewById(R.id.statusText)
        val newMessageDot: ImageView = itemView.findViewById(R.id.newMessageDot)
        val requestButton: Button = itemView.findViewById(R.id.requestButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.users_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        val context = holder.itemView.context

        // Online/offline status
        when (user.status?.lowercase()) {
            "online" -> {
                holder.statusIndicator.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                holder.statusText.text = "Online"
            }
            "offline" -> {
                holder.statusIndicator.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                holder.statusText.text = "Offline"
            }
            else -> {
                holder.statusIndicator.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_light))
                holder.statusText.text = "Away"
            }
        }

        // New message highlight
        if (user.hasNewMessage) {
            holder.usernameText.text = "${user.name} (New message)"
            holder.usernameText.setTypeface(null, Typeface.BOLD)
            holder.newMessageDot.visibility = View.VISIBLE
        } else {
            holder.usernameText.text = user.name
            holder.usernameText.setTypeface(null, Typeface.NORMAL)
            holder.newMessageDot.visibility = View.GONE
        }

        // Friend request button setup
        when (user.requestType) {
            "" -> {
                holder.requestButton.text = "Send Request"
                holder.requestButton.setBackgroundColor(Color.parseColor("#2196F3")) // Blue
                holder.requestButton.isEnabled = true
            }
            "received" -> {
                holder.requestButton.text = "Accept Request"
                holder.requestButton.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
                holder.requestButton.isEnabled = true
            }
            "sent" -> {
                holder.requestButton.text = "Request Sent"
                holder.requestButton.setBackgroundColor(Color.LTGRAY)
                holder.requestButton.isEnabled = false
            }
            "accepted" -> {
                holder.requestButton.text = "Friends"
                holder.requestButton.setBackgroundColor(Color.DKGRAY)
                holder.requestButton.isEnabled = false
            }
        }

        // Handle button click
        holder.requestButton.setOnClickListener {
            onUserClick(user)
        }

        // Optional: handle clicking the whole item too
        holder.itemView.setOnClickListener {
            if (user.requestType == "accepted") {
                onUserClick(user)
            }
        }
    }

    override fun getItemCount(): Int = userList.size
}
