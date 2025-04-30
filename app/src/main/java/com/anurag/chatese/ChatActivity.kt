package com.anurag.chatese

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
    private var isLoading = false
    private var lastKey: String? = null
    private val loadedMessageKeys = HashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        clearNewMessageIndicator() // clear red dot on open (only by receiver)

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

        val messageRef = database.child("chats").child(chatId).child(messageId)
        messageRef.setValue(message)
            .addOnSuccessListener {
                messageRef.child("status").setValue(MessageStatus.DELIVERED.toString())

                // Mark "new message" flag for receiver (so sender doesn't see red dot)
                database.child("Users")
                    .child(receiverId)
                    .child("newMessages")
                    .child(senderId)
                    .setValue(true)
            }

        binding.messageEditText.setText("")
    }

    private fun listenForMessages() {
        database.child("chats").child(chatId)
            .limitToLast(50)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    loadedMessageKeys.clear()
                    for (snap in snapshot.children) {
                        val msg = snap.getValue(MessageModel::class.java)
                        if (msg != null && snap.key != null && !loadedMessageKeys.contains(snap.key)) {
                            loadedMessageKeys.add(snap.key!!)
                            val decrypted = decryptMessage(msg.message)
                            messageList.add(msg.copy(message = decrypted))
                            lastKey = snap.key

                            // Mark message as seen if it's for this user
                            if (msg.receiverId == senderId && msg.status != MessageStatus.SEEN) {
                                snap.ref.child("status").setValue(MessageStatus.SEEN.toString())
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                    binding.messageRecyclerView.scrollToPosition(messageList.size - 1)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
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
                        adapter.notifyDataSetChanged()
                        binding.messageRecyclerView.scrollToPosition(newMessages.size - 1)
                    }

                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    isLoading = false
                }
            })
    }

    private fun clearNewMessageIndicator() {
        // Current user (senderId) is opening chat with receiverId
        // So we clear the indicator set by the other user
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
}



