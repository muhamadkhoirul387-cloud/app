package com.example.mangaku

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mangaku.databinding.ActivityLoginBinding
import com.example.mangaku.util.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = SessionManager(this)

        // If already logged in, navigate directly to MainActivity
        if (sessionManager.isLoggedIn()) {
            goToMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupActions()
    }

    private fun setupActions() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty()) {
                binding.etUsername.error = "Masukkan nama atau username pembaca"
                binding.etUsername.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.etPassword.error = "Masukkan kata sandi"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            // Save login session
            sessionManager.saveLogin(username)
            Toast.makeText(this, "Selamat datang, $username!", Toast.LENGTH_SHORT).show()
            goToMainActivity()
        }

        binding.btnGuestLogin.setOnClickListener {
            sessionManager.saveLogin("OtakuReader")
            Toast.makeText(this, "Masuk sebagai OtakuReader", Toast.LENGTH_SHORT).show()
            goToMainActivity()
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
