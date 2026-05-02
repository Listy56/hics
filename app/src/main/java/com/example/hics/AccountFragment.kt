package com.example.hics

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AccountFragment: Fragment() {

    private lateinit var userNameTv : TextView
    private lateinit var emailTv    : TextView
    private var deviceID: String? = ""
    private var indexAcc: Int?    = -1
    private var firebaseDatabase = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.setting_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userNameTv = view.findViewById(R.id.userName)
        emailTv    = view.findViewById(R.id.email)


        val accPref      = requireActivity().getSharedPreferences("ACCOUNT", MODE_PRIVATE)
        indexAcc         = accPref.getInt("index", -1)
        deviceID         = accPref.getString("deviceID", "")

        Log.d("DeviceFragment", "indexAcc: $indexAcc")

        var baseFirebase = firebaseDatabase.getReference("Hics")
        var accFirebase  = firebaseDatabase.getReference("User")


        accFirebase.child("user_$indexAcc").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val email    = snapshot.child("email").value.toString()
                    val userName = snapshot.child("userName").value.toString()

                    emailTv.text    = email
                    userNameTv.text = userName
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}