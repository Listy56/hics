package com.example.hics

import android.content.Context.MODE_PRIVATE
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MonitoringFragment: Fragment() {

    private lateinit var etPhMin : EditText
    private lateinit var etPhMax : EditText
    private lateinit var etPpmMin : EditText
    private lateinit var etPpmMax : EditText

    private lateinit var switchNotif : LinearLayout
    private lateinit var circleNotif : View
    private lateinit var save : CardView
    private lateinit var layoutWarning: LinearLayout

    private lateinit var spinnerInterval : Spinner
    private lateinit var spinnerSuhu : Spinner


    private var firebaseDatabase = FirebaseDatabase.getInstance()
    private var indexAcc: Int?    = 0
    private var isOn: Boolean     = false
    private var deviceID: String? = ""

    private var online: Boolean = false

    var phMin: String? = ""
    var phMax: String? = ""
    var ppmMin: String? = ""
    var ppmMax: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.setting_monitoring, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etPhMin     = view.findViewById(R.id.phMin)
        etPhMax     = view.findViewById(R.id.phMax)
        etPpmMin    = view.findViewById(R.id.ppmMin)
        etPpmMax    = view.findViewById(R.id.ppmMax)
        switchNotif = view.findViewById(R.id.switchNotif)
        circleNotif = view.findViewById(R.id.circleNotif)
        spinnerInterval  = view.findViewById(R.id.spinnerInterval)
        spinnerSuhu      = view.findViewById(R.id.spinnerSuhu)
        save             = view.findViewById(R.id.save)
        layoutWarning = view.findViewById(R.id.layoutWarning)

        val intervalList = listOf("10", "30", "60", "120")
        val suhuList     = listOf("C", "F")

        spinnerInterval.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, intervalList)
        spinnerSuhu.adapter     = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, suhuList)


        val accPref      = requireActivity().getSharedPreferences("ACCOUNT", MODE_PRIVATE)
        deviceID         = accPref.getString("deviceID", "")

        Log.d("MonitoringFragment", "DeviceID: $deviceID")

        var baseFirebase = firebaseDatabase.getReference("Hics")

        if (!deviceID.isNullOrEmpty()) {

            baseFirebase.child(deviceID!!)
                .child("setting")
                .addValueEventListener(object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        if (snapshot.exists()) {

                            online = true

                            spinnerInterval.visibility = View.VISIBLE
                            spinnerSuhu.visibility = View.VISIBLE

                            // ================= DATA FIREBASE =================
                            phMin = snapshot.child("phMin")
                                .getValue(String::class.java) ?: ""

                            phMax = snapshot.child("phMax")
                                .getValue(String::class.java) ?: ""

                            ppmMin = snapshot.child("ppmMin")
                                .getValue(String::class.java) ?: ""

                            ppmMax = snapshot.child("ppmMax")
                                .getValue(String::class.java) ?: ""

                            isOn = snapshot.child("notifAlert")
                                .getValue(Boolean::class.java) ?: false

                            val tempUnit = snapshot.child("tempUnit")
                                .getValue(String::class.java) ?: "C"

                            val interval = snapshot.child("intervalUpdate")
                                .getValue(String::class.java) ?: "10"

                            // ================= UPDATE UI =================
                            etPhMin.hint = phMin
                            etPhMax.hint = phMax
                            etPpmMin.hint = ppmMin
                            etPpmMax.hint = ppmMax

                            updateSwitchUI(isOn)

                            spinnerSuhu.setSelection(
                                if (tempUnit == "F") 1 else 0
                            )

                            spinnerInterval.setSelection(
                                when (interval) {
                                    "10" -> 0
                                    "30" -> 1
                                    "60" -> 2
                                    "120" -> 3
                                    else -> 0
                                }
                            )

                            // Simpan data terakhir
                            saveLastState()

                            setMonitoringEnabled(true)

                        } else {

                            // Firebase kosong / device offline
                            online = false

                            loadLastState()

                            spinnerInterval.visibility = View.VISIBLE
                            spinnerSuhu.visibility = View.VISIBLE

                            setMonitoringEnabled(false)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                        online = false

                        loadLastState()

                        spinnerInterval.visibility = View.VISIBLE
                        spinnerSuhu.visibility = View.VISIBLE

                        setMonitoringEnabled(false)

                        Toast.makeText(
                            requireContext(),
                            "Error: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })

        } else {

            online = false

            loadLastState()

            spinnerInterval.visibility = View.VISIBLE
            spinnerSuhu.visibility = View.VISIBLE

            setMonitoringEnabled(false)
        }

        switchNotif.setOnClickListener {

            if(!online){

                Toast.makeText(
                    requireContext(),
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            isOn = !isOn
            updateSwitchUI(isOn)
        }

        save.setOnClickListener {

            // Cek apakah device belum terhubung
            if (!online) {
                Toast.makeText(
                    requireContext(),
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // ===== Proses Save =====
            val newPhMax = etPhMax.text.toString().ifEmpty { phMax }
            val newPhMin = etPhMin.text.toString().ifEmpty { phMin }
            val newPpmMax = etPpmMax.text.toString().ifEmpty { ppmMax }
            val newPpmMin = etPpmMin.text.toString().ifEmpty { ppmMin }

            if (!deviceID.isNullOrEmpty()) {
                baseFirebase.child(deviceID!!)
                    .child("setting")
                    .updateChildren(
                        mapOf(
                            "phMax" to newPhMax,
                            "phMin" to newPhMin,
                            "ppmMax" to newPpmMax,
                            "ppmMin" to newPpmMin,
                            "notifAlert" to isOn,
                            "tempUnit" to spinnerSuhu.selectedItem.toString(),
                            "intervalUpdate" to spinnerInterval.selectedItem.toString()
                        )
                    )

                // Update variabel agar SharedPreferences menyimpan data terbaru
                phMin = newPhMin
                phMax = newPhMax
                ppmMin = newPpmMin
                ppmMax = newPpmMax

                saveLastState()
            }

            Toast.makeText(requireContext(), "Data Updated", Toast.LENGTH_SHORT).show()

            val fragment = SettingFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.mainFragment, fragment)
            transaction.commit()
        }
    }
    private fun saveLastState() {

        val pref = requireContext().getSharedPreferences("MONITORING_STATE", MODE_PRIVATE)

        pref.edit()
            .putString("phMin", phMin)
            .putString("phMax", phMax)
            .putString("ppmMin", ppmMin)
            .putString("ppmMax", ppmMax)
            .putBoolean("notifAlert", isOn)
            .putString("tempUnit", spinnerSuhu.selectedItem.toString())
            .putString("interval", spinnerInterval.selectedItem.toString())
            .apply()
    }
    private fun setMonitoringEnabled(enable: Boolean) {

        etPhMin.isEnabled = enable
        etPhMax.isEnabled = enable
        etPpmMin.isEnabled = enable
        etPpmMax.isEnabled = enable

        spinnerInterval.isEnabled = enable
        spinnerSuhu.isEnabled = enable

        save.alpha = if (enable) 1f else 0.5f
        switchNotif.alpha = if (enable) 1f else 0.5f

        layoutWarning.visibility =
            if (enable) View.GONE else View.VISIBLE
    }
    private fun loadLastState() {

        val pref = requireContext().getSharedPreferences("MONITORING_STATE", MODE_PRIVATE)

        phMin = pref.getString("phMin", "")
        phMax = pref.getString("phMax", "")
        ppmMin = pref.getString("ppmMin", "")
        ppmMax = pref.getString("ppmMax", "")
        isOn = pref.getBoolean("notifAlert", false)

        val temp = pref.getString("tempUnit", "C")
        val interval = pref.getString("interval", "10")

        etPhMin.hint = phMin
        etPhMax.hint = phMax
        etPpmMin.hint = ppmMin
        etPpmMax.hint = ppmMax

        updateSwitchUI(isOn)

        spinnerSuhu.setSelection(
            if (temp == "F") 1 else 0
        )

        spinnerInterval.setSelection(
            when(interval){
                "10" -> 0
                "30" -> 1
                "60" -> 2
                "120" -> 3
                else -> 0
            }
        )
    }

    fun updateSwitchUI(isOn: Boolean) {
        switchNotif.post {
            if (isOn) {
                switchNotif.setBackgroundResource(R.drawable.bg_switch_on)
                circleNotif.animate().translationX(
                    (switchNotif.width - circleNotif.width - 12).toFloat()
                ).setDuration(200).start()
            } else {
                switchNotif.setBackgroundResource(R.drawable.bg_switch_off)
                circleNotif.animate().translationX(0f).setDuration(200).start()
            }
        }
    }
}