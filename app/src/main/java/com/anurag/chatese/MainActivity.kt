package com.anurag.chatese

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anurag.chatese.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userList: ArrayList<UserModel>
    private lateinit var adapter: RecyclerViewAdapter
    private var currentUid: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            observeUsers()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        currentUid = firebaseAuth.currentUser?.uid
        database = FirebaseDatabase.getInstance()

        setupUserPresence()

        binding.spinKit.visibility = View.VISIBLE

        binding.logoutImage.setOnClickListener {
            val editor = getSharedPreferences("loginPrefs", MODE_PRIVATE).edit()
            editor.putBoolean("rememberMe", false)
            editor.apply()
            firebaseAuth.signOut()
            updateUserStatus("offline")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        userList = ArrayList()
        adapter = RecyclerViewAdapter(userList, currentUid!!) { selectedUser ->
            handleUserClick(selectedUser)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)

        loadWelcomeName()
        observeUsers() // will refresh list and red dot
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun setupUserPresence() {
        currentUid?.let { uid ->
            val userStatusRef = database.getReference("Users").child(uid).child("status")
            val connectedRef = database.getReference(".info/connected")

            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        userStatusRef.onDisconnect().setValue("offline")
                        userStatusRef.setValue("online")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity", "Presence error: ${error.message}")
                }
            })
        }
    }

    private fun updateUserStatus(status: String) {
        currentUid?.let { uid ->
            database.getReference("Users").child(uid).child("status").setValue(status)
        }
    }

    private fun loadWelcomeName() {
        currentUid?.let { uid ->
            database.getReference("Users").child(uid).child("name")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val username = snapshot.getValue(String::class.java) ?: "User"
                        binding.welcomeText.text = "Welcome $username"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MainActivity", "Database error: ${error.message}")
                    }
                })
        }
    }

    private fun observeUsers() {
        val usersRef = database.getReference("Users")

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedUsers = ArrayList<UserModel>()
                val myRequests = snapshot.child(currentUid ?: "").child("requests")

                for (userSnap in snapshot.children) {
                    val uid = userSnap.key ?: continue
                    if (uid == currentUid) continue

                    val name = userSnap.child("name").getValue(String::class.java) ?: "User"
                    val status = userSnap.child("status").getValue(String::class.java) ?: "offline"

                    // Corrected hasNewMessage logic: checking YOUR node for red dot from THEM
                    val hasNewMessage = snapshot.child(currentUid ?: "")
                        .child("newMessages")
                        .child(uid)
                        .getValue(Boolean::class.java) ?: false

                    val meToOther = myRequests.child(uid).getValue(String::class.java) ?: ""
                    val otherToMe = userSnap.child("requests").child(currentUid ?: "").getValue(String::class.java) ?: ""

                    val requestType = when {
                        meToOther == "accepted" && otherToMe == "accepted" -> "accepted"
                        meToOther == "sent" && otherToMe == "received" -> "sent"
                        meToOther == "received" && otherToMe == "sent" -> "received"
                        else -> ""
                    }

                    updatedUsers.add(UserModel(uid, name, status, hasNewMessage, requestType))
                }

                val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstVisibleItemPosition()
                val view = layoutManager.findViewByPosition(position)
                val offset = view?.top ?: 0

                userList.clear()
                userList.addAll(updatedUsers)
                adapter.notifyDataSetChanged()

                layoutManager.scrollToPositionWithOffset(position, offset)
                binding.spinKit.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "observeUsers failed: ${error.message}")
            }
        })
    }

    private fun handleUserClick(user: UserModel) {
        val userRef = database.getReference("Users")

        when (user.requestType) {
            "" -> {
                userRef.child(currentUid!!).child("requests").child(user.uid).setValue("sent")
                userRef.child(user.uid).child("requests").child(currentUid!!).setValue("received")
                Toast.makeText(this, "Friend request sent", Toast.LENGTH_SHORT).show()
            }

            "received" -> {
                userRef.child(currentUid!!).child("requests").child(user.uid).setValue("accepted")
                userRef.child(user.uid).child("requests").child(currentUid!!).setValue("accepted")
                Toast.makeText(this, "Friend request accepted", Toast.LENGTH_SHORT).show()
            }

            "sent" -> {
                Toast.makeText(this, "Friend request already sent", Toast.LENGTH_SHORT).show()
            }

            "accepted" -> {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("uid", user.uid)
                intent.putExtra("name", user.name)
                startActivity(intent)
            }
        }
    }
}


