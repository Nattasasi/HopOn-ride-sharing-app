package com.tritech.hopon.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.tritech.hopon.R
import com.tritech.hopon.ui.rideDiscovery.screen.RootHostActivity
import com.tritech.hopon.utils.SessionManager
import java.util.Locale

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
        SessionManager.setCurrentUserId(this, resolveMockUserId(email))
        SessionManager.setLoggedIn(this, true)
        navigateToHome()
    }

    private fun resolveMockUserId(email: String): String {
        val mockUserIds = listOf("u001", "u002", "u003", "u004", "u005", "u006")
        val normalized = email.trim().lowercase(Locale.ROOT)
        val positiveHash = (normalized.hashCode().toLong() and 0x7fffffffL).toInt()
        return mockUserIds[positiveHash % mockUserIds.size]
    }

    private fun navigateToHome() {
        // Finish login so users do not return here via back press.
        startActivity(Intent(this, RootHostActivity::class.java))
        finish()
    }
}
