package com.example.hics

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    @SuppressLint("WrongViewCast", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        // 🔙 Back ke login
        btnBack.setOnClickListener {
            finish()
        }

        // 🔁 Pindah ke login
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // 🔐 Register manual
        btnRegister.setOnClickListener {

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            when {
                name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() -> {
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
                    Toast.makeText(this, "Register berhasil", Toast.LENGTH_SHORT).show()
                    finish() // balik ke login
                }
            }
        }

        // Register dengan Google (dummy dulu)
        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Login dengan Google (belum diimplementasikan)", Toast.LENGTH_SHORT).show()
        }
    }
}