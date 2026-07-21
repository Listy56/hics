package com.example.hics

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import androidx.appcompat.app.AlertDialog
import android.widget.Toast


class NotifActivity: AppCompatActivity() {
    private lateinit var back: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private lateinit var notifList : MutableList<NotificationModel>
    private lateinit var btnDeleteAll: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notif)

        back         = findViewById(R.id.back)
        btnDeleteAll = findViewById(R.id.btnDeleteAll)
        recyclerView = findViewById(R.id.recyclerView)
        notifList    = mutableListOf()
        adapter      = NotificationAdapter(notifList)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter       = adapter

        val accPref =
            getSharedPreferences("ACCOUNT", MODE_PRIVATE)

        val deviceID =
            accPref.getString("deviceID", "") ?: ""
        btnDeleteAll.setOnClickListener {

            if (notifList.isEmpty()) {
                Toast.makeText(this, "Tidak ada notifikasi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Hapus Semua")
                .setMessage("Apakah Anda yakin ingin menghapus semua notifikasi?")
                .setPositiveButton("Hapus") { _, _ ->

                    FirebaseDatabase.getInstance()
                        .getReference("Hics")
                        .child(deviceID)
                        .child("notifications")
                        .removeValue()
                        .addOnSuccessListener {

                            // Reset status notifikasi
                            val pref = getSharedPreferences("STATE", MODE_PRIVATE)
                            pref.edit().clear().apply()

                            Toast.makeText(
                                this,
                                "Semua notifikasi berhasil dihapus",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                        .addOnFailureListener {

                            Toast.makeText(
                                this,
                                "Gagal menghapus notifikasi",
                                Toast.LENGTH_SHORT
                            ).show()

                        }

                }
                .setNegativeButton("Batal", null)
                .show()
        }

        FirebaseDatabase.getInstance()
            .getReference("Hics")
            .child(deviceID)
            .child("notifications")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    notifList.clear()

                    for (data in snapshot.children.reversed()) {

                        val notif =
                            data.getValue(NotificationModel::class.java)

                        if (notif != null) {
                            notifList.add(notif)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

        back.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}