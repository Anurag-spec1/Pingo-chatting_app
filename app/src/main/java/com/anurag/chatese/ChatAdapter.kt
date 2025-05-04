package com.anurag.chatese

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

//class ChatAdapter(
//    private val messageList: List<MessageModel>,
//    private val senderId: String,
//    private val onMessageLongClick: (MessageModel) -> Unit
//) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//
//    private val ITEM_SEND = 1
//    private val ITEM_RECEIVE = 2
//
//    override fun getItemViewType(position: Int): Int {
//        return if (messageList[position].senderId == senderId) ITEM_SEND else ITEM_RECEIVE
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        return if (viewType == ITEM_SEND) {
//            val view = LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_send, parent, false)
//            SendViewHolder(view)
//        } else {
//            val view = LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_receive, parent, false)
//            ReceiveViewHolder(view)
//        }
//    }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val msg = messageList[position]
//        val timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
//
//        when (holder) {
//            is SendViewHolder -> {
//                // Handle sent messages
//                holder.sendText.text = msg.message
//                holder.timeText.text = timestamp
//
//                // Message status handling (ticks)
//                when(msg.status) {
//                    MessageStatus.SENT -> {
//                        holder.singleCheck.visibility = View.VISIBLE
//                        holder.doubleCheck.visibility = View.GONE
//                        holder.doubleCheck.clearColorFilter()
//                    }
//                    MessageStatus.DELIVERED -> {
//                        holder.singleCheck.visibility = View.GONE
//                        holder.doubleCheck.visibility = View.VISIBLE
//                        holder.doubleCheck.setColorFilter(
//                            ContextCompat.getColor(holder.itemView.context, R.color.gray),
//                            PorterDuff.Mode.SRC_IN
//                        )
//                    }
//                    MessageStatus.SEEN -> {
//                        holder.singleCheck.visibility = View.GONE
//                        holder.doubleCheck.visibility = View.VISIBLE
//                        holder.doubleCheck.setColorFilter(
//                            ContextCompat.getColor(holder.itemView.context, R.color.blue),
//                            PorterDuff.Mode.SRC_IN
//                        )
//                    }
//                }
//
//                holder.itemView.setOnLongClickListener {
//                    onMessageLongClick(msg)
//                    true
//                }
//            }
//
//            is ReceiveViewHolder -> {
//                // Handle received messages
//                holder.receiveText.text = msg.message
//                holder.timeText.text = timestamp
//
//                // Typically received messages don't show read receipts
//                // But you can implement if needed
//                holder.singleCheck.visibility = View.GONE
//                holder.doubleCheck.visibility = View.GONE
//
//                holder.itemView.setOnLongClickListener {
//                    onMessageLongClick(msg)
//                    true
//                }
//            }
//        }
//    }
//
//    override fun getItemCount(): Int = messageList.size
//
//    inner class SendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val sendText: TextView = view.findViewById(R.id.text_send)
//        val timeText: TextView = view.findViewById(R.id.text_send_time)
//
////        val statusText: TextView = view.findViewById(R.id.text_status)
//
//
//
//    }
//
//
//    inner class ReceiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val receiveText: TextView = view.findViewById(R.id.text_receive)
//        val timeText: TextView = view.findViewById(R.id.text_receive_time)
//    }
//}





class ChatAdapter(
    private val messageList: List<MessageModel>,
    private val senderId: String,
    private val onMessageLongClick: (MessageModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val ITEM_SEND = 1
        private const val ITEM_RECEIVE = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == senderId) ITEM_SEND else ITEM_RECEIVE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SEND) {
            SendViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_send, parent, false)
            )
        } else {
            ReceiveViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_receive, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messageList[position]
        val timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

        when (holder) {
            is SendViewHolder -> {
                holder.bind(msg, timestamp)
                holder.itemView.setOnLongClickListener {
                    onMessageLongClick(msg)
                    true
                }
            }
            is ReceiveViewHolder -> {
                holder.bind(msg, timestamp)
                holder.itemView.setOnLongClickListener {
                    onMessageLongClick(msg)
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int = messageList.size

    inner class SendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val sendText: TextView = view.findViewById(R.id.text_send)
        private val timeText: TextView = view.findViewById(R.id.text_send_time)
        private val singleCheck: ImageView = view.findViewById(R.id.singleCheck)
        private val doubleCheck: ImageView = view.findViewById(R.id.doubleCheck)

        fun bind(message: MessageModel, timestamp: String) {
            sendText.text = message.message
            timeText.text = timestamp

            when (message.status) {
                MessageStatus.SENT -> {
                    singleCheck.visibility = View.VISIBLE
                    doubleCheck.visibility = View.GONE
                    doubleCheck.clearColorFilter()
                }
                MessageStatus.DELIVERED -> {
                    singleCheck.visibility = View.GONE
                    doubleCheck.visibility = View.VISIBLE
                    doubleCheck.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.gray_600),
                        PorterDuff.Mode.SRC_IN
                    )
                }
                MessageStatus.SEEN -> {
                    singleCheck.visibility = View.GONE
                    doubleCheck.visibility = View.VISIBLE
                    doubleCheck.setColorFilter(
                        ContextCompat.getColor(itemView.context, carbon.R.color.carbon_blue_a200),
                        PorterDuff.Mode.SRC_IN
                    )
                }
            }
        }
    }

    inner class ReceiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val receiveText: TextView = view.findViewById(R.id.text_receive)
        private val timeText: TextView = view.findViewById(R.id.text_receive_time)

        fun bind(message: MessageModel, timestamp: String) {
            receiveText.text = message.message
            timeText.text = timestamp
        }
    }
}