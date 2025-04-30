package com.anurag.chatese

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView

class splashscreen : AppCompatActivity() {
    private lateinit var lottieView: LottieAnimationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        lottieView = findViewById(R.id.lottieAnimationView)
        lottieView.playAnimation()
        val sharedPref = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val rememberMe = sharedPref.getBoolean("rememberMe", false)

        Handler(Looper.getMainLooper()).postDelayed({
            lottieView.cancelAnimation()
            // Optionally hide it
            lottieView.visibility = View.GONE
            if (rememberMe) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()

        }, 4100)


    }
}