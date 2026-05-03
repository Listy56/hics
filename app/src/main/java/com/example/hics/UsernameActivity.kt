package com.example.hics

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class UsernameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.username)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val email = intent.getStringExtra("email") ?: ""
        val defaultUsername = intent.getStringExtra("defaultUsername") ?: ""

        etUsername.setText(defaultUsername)

        val database = FirebaseDatabase.getInstance().reference

        btnSave.setOnClickListener {

            val username = etUsername.text.toString().trim()

            // 🔴 VALIDASI
            if (username.isEmpty()) {
                Toast.makeText(this, "Username wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ CEK USERNAME LANGSUNG KE "User"
            database.child("User").get()
                .addOnSuccessListener { snapshot ->

                    for (snap in snapshot.children) {
                        val dbUsername = snap.child("userName").value.toString()
                        if (dbUsername == username) {
                            Toast.makeText(this, "Username sudah dipakai", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                    }

                    // 🔥 BUAT user_X
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

                    // 🔥 SIMPAN USER
                    database.child("User")
                        .child(key)
                        .setValue(userMap)
                        .addOnSuccessListener {

                            val indexFix = key.substringAfter("_").toIntOrNull()

                            getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
                                .putInt("index", indexFix ?: -1)
                                .apply()

                            Toast.makeText(this, "Register berhasil", Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal simpan user", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal ambil data user", Toast.LENGTH_SHORT).show()
                }
        }
    }
}