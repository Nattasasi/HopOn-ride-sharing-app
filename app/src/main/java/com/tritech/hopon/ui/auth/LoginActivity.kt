package com.tritech.hopon.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.hopOnComposeTheme
import com.tritech.hopon.ui.rideDiscovery.core.ApiClient
import com.tritech.hopon.ui.rideDiscovery.core.ApiLoginRequest
import com.tritech.hopon.ui.rideDiscovery.core.AuthService
import com.tritech.hopon.ui.rideDiscovery.core.UsersService
import com.tritech.hopon.ui.rideDiscovery.screen.RootHostActivity
import com.tritech.hopon.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip login screen when a previous authenticated session exists.
        if (SessionManager.isLoggedIn(this)) {
            navigateToHome()
            return
        }

        setContent {
            hopOnComposeTheme {
                loginScreen(
                    onLogin = { email, password -> handleLogin(email, password) },
                    onRegister = { navigateToRegister() }
                )
            }
        }
    }

    private fun handleLogin(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.login_required_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val authService = ApiClient.create<AuthService>(this)

        lifecycleScope.launch {
            try {
                val response = authService.login(ApiLoginRequest(email, password))

                // Persist tokens and user identity.
                SessionManager.setToken(this@LoginActivity, response.token)
                SessionManager.setRefreshToken(this@LoginActivity, response.refreshToken)
                // Decode userId from JWT payload (sub claim).
                val userId = response.userId ?: extractSubFromJwt(response.token) ?: email
                SessionManager.setCurrentUserId(this@LoginActivity, userId)
                SessionManager.setLoggedIn(this@LoginActivity, true)

                // Fetch and cache the user's display name.
                runCatching {
                    val usersService = ApiClient.create<UsersService>(this@LoginActivity)
                    val user = usersService.getUser(userId)
                    SessionManager.setDisplayName(this@LoginActivity, user.fullName)
                }

                navigateToHome()
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 401 || e.code() == 400) {
                    getString(R.string.login_failed)
                } else {
                    getString(R.string.login_error)
                }
                Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this@LoginActivity, getString(R.string.login_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, RootHostActivity::class.java))
        finish()
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}

/**
 * Extracts the `sub` (subject/userId) field from the JWT payload without a
 * third-party JWT library.  Returns null on any parse failure so callers can
 * fall back gracefully.
 */
internal fun extractSubFromJwt(token: String): String? {
    return try {
        val payload = token.split(".").getOrNull(1) ?: return null
        // Base64 URL-decode (add padding if needed).
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = String(android.util.Base64.decode(padded, android.util.Base64.URL_SAFE))
        val jsonObj = org.json.JSONObject(decoded)
        jsonObj.optString("sub").takeIf { it.isNotBlank() }
            ?: jsonObj.optString("id").takeIf { it.isNotBlank() }
            ?: jsonObj.optString("_id").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

