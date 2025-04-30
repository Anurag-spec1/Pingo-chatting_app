package com.anurag.chatese

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messageList: List<MessageModel>,
    private val senderId: String,
    private val onMessageLongClick: (MessageModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_SEND = 1
    private val ITEM_RECEIVE = 2

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == senderId) ITEM_SEND else ITEM_RECEIVE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SEND) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_send, parent, false)
            SendViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_receive, parent, false)
            ReceiveViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messageList[position]
        val timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

        if (holder is SendViewHolder) {
            holder.sendText.text = msg.message
            holder.timeText.text = timestamp
            holder.itemView.setOnLongClickListener {
                onMessageLongClick(msg)
                true
            }

        } else if (holder is ReceiveViewHolder) {
            holder.receiveText.text = msg.message
            holder.timeText.text = timestamp

            holder.itemView.setOnLongClickListener {
                onMessageLongClick(msg)
                true
            }
        }
    }

    override fun getItemCount(): Int = messageList.size

    inner class SendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sendText: TextView = view.findViewById(R.id.text_send)
        val timeText: TextView = view.findViewById(R.id.text_send_time)
//        val statusText: TextView = view.findViewById(R.id.text_status)
    }

    inner class ReceiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val receiveText: TextView = view.findViewById(R.id.text_receive)
        val timeText: TextView = view.findViewById(R.id.text_receive_time)
    }
}





