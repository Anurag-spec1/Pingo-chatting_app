package com.anurag.chatese

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.anurag.chatese.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.signupText.setOnClickListener{
            startActivity(Intent(this, SignupActivity::class.java))
        }
        binding.buttonLogin.setOnClickListener {
            val usernew = binding.usernameEditText.text.toString().trim()
            val passnew = binding.passwordEditText.text.toString().trim()

            if (usernew.isNotEmpty() && passnew.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(usernew, passnew).addOnCompleteListener {
                    if (it.isSuccessful) {

                        // ✅ Save rememberMe state AFTER successful login
                        val sharedPref = getSharedPreferences("loginPrefs", MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putBoolean("rememberMe", binding.isCheck.isChecked)
                        editor.apply()

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Create your account first", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Fields can not be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
