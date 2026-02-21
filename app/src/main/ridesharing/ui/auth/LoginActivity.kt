package com.tritech.hopon.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.tritech.hopon.R
import com.tritech.hopon.databinding.ActivityLoginBinding
import com.tritech.hopon.ui.components.hopOnButton
import com.tritech.hopon.ui.maps.MapsActivity
import com.tritech.hopon.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip login screen when a previous authenticated session exists.
        if (SessionManager.isLoggedIn(this)) {
            navigateToHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val emailEditText = binding.emailEditText
        val passwordEditText = binding.passwordEditText
        val loginButton = binding.loginButton

        setUpLoginButtonCompose(emailEditText, passwordEditText)
    }

    private fun setUpLoginButtonCompose(emailEditText: EditText, passwordEditText: EditText) {
        val loginButton = binding.loginButton
        loginButton.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        loginButton.setContent {
            hopOnButton(
                text = "Login",
                onClick = {
                    handleLoginClick(emailEditText, passwordEditText)
                }
            )
        }
    }

    private fun handleLoginClick(emailEditText: EditText, passwordEditText: EditText) {
        // Read and normalize user input from the login form.
        val email = emailEditText.text?.toString()?.trim().orEmpty()
        val password = passwordEditText.text?.toString().orEmpty()

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
