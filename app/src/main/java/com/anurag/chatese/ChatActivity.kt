package com.anurag.chatese

import android.content.SharedPreferences
import okhttp3.Request
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anurag.chatese.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AlertDialog
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var database: DatabaseReference
    private lateinit var messageList: ArrayList<MessageModel>
    private lateinit var adapter: ChatAdapter
    private lateinit var receiverId: String
    private lateinit var senderId: String
    private lateinit var chatId: String
    private lateinit var sharedPreferences: SharedPreferences
    private var isLoading = false
    private var lastKey: String? = null
    private val loadedMessageKeys = HashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        database = FirebaseDatabase.getInstance().reference
        senderId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        receiverId = intent.getStringExtra("uid") ?: ""
        val receiverName = intent.getStringExtra("name") ?: ""

        supportActionBar?.title = receiverName

        chatId = if (senderId < receiverId) "$senderId$receiverId" else "$receiverId$senderId"

        messageList = ArrayList()
        adapter = ChatAdapter(messageList, senderId) { message ->
            showMessageOptionsDialog(message)
        }

        binding.messageRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.messageRecyclerView.adapter = adapter

        listenForMessages()
        clearNewMessageIndicator()

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }

        binding.messageRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (layoutManager.findFirstVisibleItemPosition() == 0 && !isLoading) {
                    loadMoreMessages()
                }
            }
        })
    }

    private fun sendMessage(text: String) {
        val messageId = database.child("chats").child(chatId).push().key ?: return
        val message = MessageModel(
            senderId = senderId,
            receiverId = receiverId,
            message = encryptMessage(text),
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT
        )

        // Optimistic UI update
        val decryptedMessage = message.copy(message = text)
        messageList.add(decryptedMessage)
        val insertPosition = messageList.size - 1
        adapter.notifyItemInserted(insertPosition)
        binding.messageRecyclerView.scrollToPosition(insertPosition)

        val messageRef = database.child("chats").child(chatId).child(messageId)
        messageRef.setValue(message)
            .addOnSuccessListener {
                // Update status to DELIVERED
                messageRef.child("status").setValue(MessageStatus.DELIVERED.toString())

                // Update local message status
                val index = messageList.indexOfFirst { it.timestamp == message.timestamp }
                if (index != -1) {
                    messageList[index] = messageList[index].copy(status = MessageStatus.DELIVERED)
                    adapter.notifyItemChanged(index)
                }

                // Notify receiver
                database.child("Users")
                    .child(receiverId)
                    .child("newMessages")
                    .child(senderId)
                    .setValue(true)
            }
            .addOnFailureListener {
                // Remove failed message
                val index = messageList.indexOfFirst { it.timestamp == message.timestamp }
                if (index != -1) {
                    messageList.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }

        binding.messageEditText.setText("")
    }

    private fun listenForMessages() {
        database.child("chats").child(chatId)
            .limitToLast(100)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newMessages = mutableListOf<MessageModel>()
                    val updatedMessages = mutableListOf<Pair<Int, MessageModel>>()
                    val messagesToMarkAsSeen = mutableMapOf<String, String>()
                    var isUserActive = true // Track if user is actively viewing chat

                    // Check if activity is in foreground
                    if (isFinishing || isDestroyed) {
                        isUserActive = false
                    }

                    for (snap in snapshot.children) {
                        val msg = snap.getValue(MessageModel::class.java) ?: continue
                        val messageKey = snap.key ?: continue

                        val decrypted = msg.copy(message = decryptMessage(msg.message))

                        val existingIndex = messageList.indexOfFirst { it.timestamp == msg.timestamp }

                        if (existingIndex == -1 && !loadedMessageKeys.contains(messageKey)) {
                            loadedMessageKeys.add(messageKey)
                            newMessages.add(decrypted)
                            lastKey = messageKey
                        } else if (existingIndex != -1) {
                            val existingMessage = messageList[existingIndex]
                            if (existingMessage.status != msg.status) {
                                updatedMessages.add(existingIndex to decrypted)
                            }
                        }

                        // Only mark as seen if user is actively viewing chat
                        if (msg.receiverId == senderId && msg.status != MessageStatus.SEEN && isUserActive) {
                            messagesToMarkAsSeen[messageKey] = msg.senderId
                        }
                    }

                    if (newMessages.isNotEmpty()) {
                        messageList.addAll(newMessages)
                        messageList.sortBy { it.timestamp }
                        adapter.notifyItemRangeInserted(messageList.size - newMessages.size, newMessages.size)
                        binding.messageRecyclerView.scrollToPosition(messageList.size - 1)
                    }

                    for ((index, updatedMessage) in updatedMessages) {
                        messageList[index] = updatedMessage
                        adapter.notifyItemChanged(index)
                    }

                    for (entry in messagesToMarkAsSeen.entries) {
                        database.child("chats").child(chatId).child(entry.key)
                            .child("status").setValue(MessageStatus.SEEN.toString())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
                }
            })
    }
    override fun onResume() {
        super.onResume()
        // When activity comes to foreground, mark messages as seen
        markUnseenMessagesAsSeen()
    }

    override fun onPause() {
        super.onPause()
        // When activity goes to background, don't mark messages as seen
    }

    private fun markUnseenMessagesAsSeen() {
        database.child("chats").child(chatId)
            .orderByChild("receiverId").equalTo(senderId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snap in snapshot.children) {
                        val msg = snap.getValue(MessageModel::class.java)
                        if (msg != null && msg.status != MessageStatus.SEEN) {
                            snap.ref.child("status").setValue(MessageStatus.SEEN.toString())
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun clearNewMessageIndicator() {
        database.child("Users")
            .child(senderId)
            .child("newMessages")
            .child(receiverId)
            .removeValue()
    }

    private fun showMessageOptionsDialog(message: MessageModel) {
        val options = arrayOf("Delete Message", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Message Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> deleteMessage(message)
                }
            }
            .show()
    }

    private fun deleteMessage(message: MessageModel) {
        database.child("chats").child(chatId)
            .orderByChild("timestamp").equalTo(message.timestamp.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snap in snapshot.children) {
                        snap.ref.removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to delete message", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun encryptMessage(message: String): String {
        return Base64.encodeToString(message.toByteArray(), Base64.DEFAULT)
    }

    private fun decryptMessage(encrypted: String): String {
        return String(Base64.decode(encrypted, Base64.DEFAULT))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any listeners if needed
    }
    private fun loadMoreMessages() {
        if (isLoading || lastKey == null) return
        isLoading = true

        database.child("chats").child(chatId)
            .orderByKey()
            .endBefore(lastKey)
            .limitToLast(20)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newMessages = ArrayList<MessageModel>()
                    for (snap in snapshot.children) {
                        if (snap.key != null && !loadedMessageKeys.contains(snap.key)) {
                            val msg = snap.getValue(MessageModel::class.java)
                            if (msg != null) {
                                loadedMessageKeys.add(snap.key!!)
                                newMessages.add(0, msg.copy(message = decryptMessage(msg.message)))
                                lastKey = snap.key
                            }
                        }
                    }

                    if (newMessages.isNotEmpty()) {
                        messageList.addAll(0, newMessages)
                        adapter.notifyItemRangeInserted(0, newMessages.size)
                        binding.messageRecyclerView.scrollToPosition(newMessages.size - 1)
                    }

                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    isLoading = false
                    Toast.makeText(this@ChatActivity, "Failed to load more messages", Toast.LENGTH_SHORT).show()
                }
            })
    }
}



