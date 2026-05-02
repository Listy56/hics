package com.example.hics

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference

        btnBack.setOnClickListener { finish() }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRegister.setOnClickListener {

            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            when {
                username.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                }

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                }

                password.length < 6 -> {
                    Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                }

                password != confirm -> {
                    Toast.makeText(this, "Password tidak sama", Toast.LENGTH_SHORT).show()
                }

                else -> {

                    btnRegister.isEnabled = false

                    // 🔥 CEK DUPLIKAT
                    database.child("User")
                        .get()
                        .addOnSuccessListener { snapshot ->

                            var usernameExist = false
                            var emailExist = false

                            for (userSnap in snapshot.children) {
                                val dbUsername = userSnap.child("userName").value.toString()
                                val dbEmail = userSnap.child("email").value.toString()

                                if (username == dbUsername) usernameExist = true
                                if (email == dbEmail) emailExist = true
                            }

                            when {
                                usernameExist -> {
                                    btnRegister.isEnabled = true
                                    Toast.makeText(this, "Username sudah terdaftar", Toast.LENGTH_SHORT).show()
                                }

                                emailExist -> {
                                    btnRegister.isEnabled = true
                                    Toast.makeText(this, "Email sudah terdaftar", Toast.LENGTH_SHORT).show()
                                }

                                else -> {

                                    // 🔥 REGISTER AUTH
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnSuccessListener {

                                            // 🔥 BUAT KEY user_X
                                            var index = 1
                                            var key: String

                                            do {
                                                key = "user_$index"
                                                index++
                                            } while (snapshot.hasChild(key))

                                            val userMap = HashMap<String, Any>()
                                            userMap["userName"] = username
                                            userMap["email"] = email
                                            userMap["id"] = ""

                                            // 🔥 SIMPAN KE DATABASE + SIMPAN INDEX
                                            database.child("User")
                                                .child(key)
                                                .setValue(userMap)
                                                .addOnSuccessListener {

                                                    // 🔥 ambil index dari key
                                                    val indexFix = key.substringAfter("_").toIntOrNull()

                                                    // 🔥 simpan ke SharedPreferences
                                                    getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
                                                        .putInt("index", indexFix ?: -1)
                                                        .apply()

                                                    Toast.makeText(this, "Register berhasil", Toast.LENGTH_SHORT).show()

                                                    startActivity(Intent(this, MainActivity::class.java))
                                                    finish()
                                                }
                                                .addOnFailureListener {
                                                    btnRegister.isEnabled = true
                                                    Toast.makeText(this, "Gagal simpan data", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        .addOnFailureListener {
                                            btnRegister.isEnabled = true
                                            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }

                        }
                        .addOnFailureListener {
                            btnRegister.isEnabled = true
                            Toast.makeText(this, "Gagal cek data", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Login Google belum tersedia", Toast.LENGTH_SHORT).show()
        }
    }
}