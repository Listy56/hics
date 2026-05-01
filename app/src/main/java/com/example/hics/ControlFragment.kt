package com.example.hics

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ControlFragment: Fragment() {
    private lateinit var mode: TextView
    private lateinit var pumpLayout: LinearLayout
    private lateinit var switchPump: LinearLayout
    private lateinit var circlePump: View
    private lateinit var switchMode: LinearLayout
    private lateinit var circleMode: View
    private lateinit var tvStatusMode: TextView
    private lateinit var statusSwitch: TextView

    private var deviceID: String? = ""
    private var firebaseDatabase = FirebaseDatabase.getInstance()

    private var modeStatus: Boolean = false
    private var isOn: Boolean = false
    private var online: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.setting_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mode         = view.findViewById(R.id.mode)
        pumpLayout   = view.findViewById(R.id.pumpLayout)
        switchPump   = view.findViewById(R.id.switchPump)
        circlePump   = view.findViewById(R.id.circlePump)
        switchMode   = view.findViewById(R.id.switchMode)
        circleMode   = view.findViewById(R.id.circleMode)
        statusSwitch = view.findViewById(R.id.statusSwitch)
        tvStatusMode = view.findViewById(R.id.tvStatusMode)


        val accPref      = requireActivity().getSharedPreferences("ACCOUNT", MODE_PRIVATE)
        deviceID         = accPref.getString("deviceID", "")

        Log.d("MonitoringFragment", "DeviceID: $deviceID")

        var baseFirebase = firebaseDatabase.getReference("Hics")

        if (deviceID != null && deviceID.toString().isNotEmpty()) {
            baseFirebase.child(deviceID.toString()).child("control").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        modeStatus = snapshot.child("mode").value.toString().toBoolean()
                        isOn       = snapshot.child("waterPump").value.toString().toBoolean()

                        Log.d("ControlFragment", "ada data")
                        online = true

                        if(modeStatus) {
                            mode.text = "Auto"
                            modeSwitchUI(true)
                            tvStatusMode.visibility = View.VISIBLE
                            pumpLayout.visibility = View.GONE
                        } else {
                            mode.text = "Manual"
                            modeSwitchUI(false)
                            pumpSwitchUI(isOn)
                            tvStatusMode.visibility = View.GONE
                            pumpLayout.visibility = View.VISIBLE
                        }
                    } else {
                        online = false
                        Log.d("ControlFragment", "tidak ada data")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        switchMode.setOnClickListener {
            if(online) {
                modeStatus = !modeStatus
                modeSwitchUI(modeStatus)

                if (deviceID != null && deviceID.toString().isNotEmpty()) {
                    baseFirebase.child(deviceID!!).child("control").child("mode").setValue(modeStatus)
                }
            }
        }

        switchPump.setOnClickListener {
            if(online) {
                isOn = !isOn
                pumpSwitchUI(isOn)

                if (deviceID != null && deviceID.toString().isNotEmpty()) {
                    baseFirebase.child(deviceID!!).child("control").child("waterPump").setValue(isOn)
                }
            }
        }
    }

    fun modeSwitchUI(isOn: Boolean) {
        if (isOn) {
            switchMode.setBackgroundResource(R.drawable.bg_switch_on)
            circleMode.animate().translationX(60f).setDuration(200).start()
        } else {
            switchMode.setBackgroundResource(R.drawable.bg_switch_off)
            circleMode.animate().translationX(0f).setDuration(200).start()
        }
    }

    fun pumpSwitchUI(isOn: Boolean) {
        if (isOn) {
            switchPump.setBackgroundResource(R.drawable.bg_switch_on)
            circlePump.animate().translationX(60f).setDuration(200).start()
            statusSwitch.text = "ON"
        } else {
            switchPump.setBackgroundResource(R.drawable.bg_switch_off)
            circlePump.animate().translationX(0f).setDuration(200).start()
            statusSwitch.text = "OFF"
        }
    }
}