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

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgot = findViewById<TextView>(R.id.tvForgot)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference

        tvForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {

            val input = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email / Username & Password wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {

                // ✅ LOGIN EMAIL
                loginWithEmail(auth, input, password, null)

            } else {

                // 🔥 CARI EMAIL DARI USERNAME
                database.child("User")
                    .get()
                    .addOnSuccessListener { snapshot ->

                        var emailDitemukan: String? = null

                        for (userSnap in snapshot.children) {
                            val dbUsername = userSnap.child("userName").value.toString()
                            val dbEmail = userSnap.child("email").value.toString()

                            if (input == dbUsername) {
                                emailDitemukan = dbEmail
                                break
                            }
                        }

                        if (emailDitemukan != null) {
                            loginWithEmail(auth, emailDitemukan, password, input)
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

    // 🔥 FUNCTION LOGIN BARU (SUDAH ADA INDEX)
    private fun loginWithEmail(
        auth: FirebaseAuth,
        email: String,
        password: String,
        inputUsername: String?
    ) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {

                val database = FirebaseDatabase.getInstance().reference

                database.child("User")
                    .get()
                    .addOnSuccessListener { snapshot ->

                        var foundIndex: Int? = null

                        for (userSnap in snapshot.children) {

                            val dbEmail = userSnap.child("email").value.toString()
                            val dbUsername = userSnap.child("userName").value.toString()

                            if (dbEmail == email || dbUsername == inputUsername) {

                                val key = userSnap.key // user_1
                                val index = key?.substringAfter("_")?.toIntOrNull()

                                foundIndex = index
                                break
                            }
                        }

                        if (foundIndex != null) {

                            // 🔥 SIMPAN INDEX
                            getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
                                .putInt("index", foundIndex)
                                .apply()

                            Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()

                        } else {
                            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener {
                findViewById<Button>(R.id.btnLogin).isEnabled = true
                Toast.makeText(this, "Login gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}