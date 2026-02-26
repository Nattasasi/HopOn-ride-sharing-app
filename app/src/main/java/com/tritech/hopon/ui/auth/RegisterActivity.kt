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
import com.tritech.hopon.ui.rideDiscovery.core.ApiRegisterRequest
import com.tritech.hopon.ui.rideDiscovery.core.AuthService
import com.tritech.hopon.ui.rideDiscovery.screen.RootHostActivity
import com.tritech.hopon.utils.SessionManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            hopOnComposeTheme {
                registerScreen(
                    onRegister = { form -> handleRegister(form) },
                    onSignIn   = { finish() }   // back to LoginActivity
                )
            }
        }
    }

    private fun handleRegister(form: RegisterFormState) {
        if (form.firstName.isEmpty() || form.lastName.isEmpty() ||
            form.email.isEmpty() || form.password.isEmpty() ||
            form.dob.isEmpty() || form.phone.isEmpty()
        ) {
            Toast.makeText(this, getString(R.string.register_required_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val authService = ApiClient.create<AuthService>(this)
        val request = ApiRegisterRequest(
            first_name   = form.firstName,
            last_name    = form.lastName,
            email        = form.email,
            dob          = form.dob,
            password     = form.password,
            phone_number = form.phone,
            role         = "rider"
        )

        lifecycleScope.launch {
            try {
                val response = authService.register(request)

                // Persist tokens and navigate straight to the main screen.
                SessionManager.setToken(this@RegisterActivity, response.token)
                SessionManager.setRefreshToken(this@RegisterActivity, response.refreshToken)
                val userId = response.userId ?: extractSubFromJwt(response.token) ?: form.email
                SessionManager.setCurrentUserId(this@RegisterActivity, userId)
                SessionManager.setDisplayName(this@RegisterActivity, "${form.firstName} ${form.lastName}".trim())
                SessionManager.setLoggedIn(this@RegisterActivity, true)

                startActivity(Intent(this@RegisterActivity, RootHostActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() in 400..409) {
                    getString(R.string.register_failed)
                } else {
                    getString(R.string.register_error)
                }
                Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this@RegisterActivity, getString(R.string.register_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
