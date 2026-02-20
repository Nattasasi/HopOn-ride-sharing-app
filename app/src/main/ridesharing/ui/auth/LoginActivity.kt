package com.tritech.hopon.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tritech.hopon.R
import com.tritech.hopon.ui.maps.MapsActivity
import com.tritech.hopon.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip login screen when a previous authenticated session exists.
        if (SessionManager.isLoggedIn(this)) {
            navigateToHome()
            return
        }

        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            // Read and normalize user input from the login form.
            val email = emailEditText.text?.toString()?.trim().orEmpty()
            val password = passwordEditText.text?.toString().orEmpty()

            // Enforce required fields before proceeding.
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.login_required_fields), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Mark session as logged-in and navigate to main map screen.
            SessionManager.setLoggedIn(this, true)
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        // Finish login so users do not return here via back press.
        startActivity(Intent(this, MapsActivity::class.java))
        finish()
    }
}
