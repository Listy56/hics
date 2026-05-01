package com.example.hics

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail) // sekarang bisa email / username
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgot = findViewById<TextView>(R.id.tvForgot)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference
        tvForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        // pindah ke register
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }


        btnLogin.setOnClickListener {

            val input = etEmail.text.toString().trim() // bisa email / username
            val password = etPassword.text.toString().trim()

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email / Username & Password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            // 🔥 CEK: input ini email atau username
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {

                // ✅ langsung login kalau email
                loginWithEmail(auth, input, password)

            } else {

                // 🔥 cari email dari username di database
                database.child("User")
                    .get()
                    .addOnSuccessListener { snapshot ->

                        var emailDitemukan: String? = null

                        for (userSnap in snapshot.children) {
                            val dbUsername = userSnap.child("username").value.toString()
                            val dbEmail = userSnap.child("email").value.toString()

                            if (input == dbUsername) {
                                emailDitemukan = dbEmail
                                break
                            }
                        }

                        if (emailDitemukan != null) {
                            loginWithEmail(auth, emailDitemukan, password)
                        } else {
                            btnLogin.isEnabled = true
                            Toast.makeText(this, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }

                    }
                    .addOnFailureListener {
                        btnLogin.isEnabled = true
                        Toast.makeText(this, "Gagal ambil data", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // 🔥 FUNCTION LOGIN EMAIL
    private fun loginWithEmail(auth: FirebaseAuth, email: String, password: String) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {

                Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                findViewById<Button>(R.id.btnLogin).isEnabled = true
                Toast.makeText(this, "Login gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}