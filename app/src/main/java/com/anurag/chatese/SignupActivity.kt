package com.anurag.chatese

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anurag.chatese.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        binding.buttonSignup.setOnClickListener {
            val email = binding.usernameEditText2.text.toString().trim()
            val password = binding.passwordEditText2.text.toString().trim()
            val name = binding.nameEditText2.text.toString().trim()




            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val uid = firebaseAuth.currentUser?.uid
                        val userMap = hashMapOf(
                            "name" to name
                        )


                        database.child("Users").child(uid!!).setValue(userMap).addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                Toast.makeText(this, "Account created and data stored", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Data not saved: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } else {
                        Toast.makeText(this, "Failed to create account", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
