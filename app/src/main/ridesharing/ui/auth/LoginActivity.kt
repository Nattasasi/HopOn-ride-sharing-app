package com.tritech.hopon.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
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

        setContent {
            loginScreen(
                onLogin = { email, password ->
                    handleLogin(email, password)
                }
            )
        }
    }

    private fun handleLogin(email: String, password: String) {
        // Enforce required fields before proceeding.
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.login_required_fields), Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Mark session as logged-in and navigate to main map screen.
        SessionManager.setLoggedIn(this, true)
        navigateToHome()
    }

    private fun navigateToHome() {
        // Finish login so users do not return here via back press.
        startActivity(Intent(this, MapsActivity::class.java))
        finish()
    }
}
