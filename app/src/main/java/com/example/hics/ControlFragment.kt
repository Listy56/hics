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
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import com.google.firebase.database.DatabaseReference

class ControlFragment : Fragment() {

    private lateinit var mode: TextView
    private lateinit var tvStatusMode: TextView

    // MODE
    private lateinit var switchMode: LinearLayout
    private lateinit var circleMode: View

    // POMPA AIR
    private lateinit var pumpLayout: CardView
    private lateinit var switchPump: LinearLayout
    private lateinit var circlePump: View
    private lateinit var statusSwitch: TextView

    // PH UP
    private lateinit var phUpLayout: CardView
    private lateinit var switchPhUp: LinearLayout
    private lateinit var circlePhUp: View
    private lateinit var statusPhUp: TextView

    // PH DOWN
    private lateinit var phDownLayout: CardView
    private lateinit var switchPhDown: LinearLayout
    private lateinit var circlePhDown: View
    private lateinit var statusPhDown: TextView

    // NUTRISI UP
    private lateinit var nutrisiUpLayout: CardView
    private lateinit var edtNutrisiA: EditText
    private lateinit var btnTambahA: Button
    private lateinit var tvEstimasiA: TextView
    private lateinit var tvStatusA: TextView


    // NUTRISI DOWN
    private lateinit var nutrisiDownLayout: CardView
    private lateinit var edtNutrisiB: EditText
    private lateinit var btnTambahB: Button
    private lateinit var tvEstimasiB: TextView
    private lateinit var tvStatusB: TextView

    private var deviceID: String? = ""
    private var firebaseDatabase = FirebaseDatabase.getInstance()

    // STATUS
    private var modeStatus = false
    private var pumpOn = false
    private var phUpOn = false
    private var phDownOn = false
    private lateinit var layoutWarning: LinearLayout

    private var online = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.setting_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // MODE
        mode = view.findViewById(R.id.mode)
        switchMode = view.findViewById(R.id.switchMode)
        circleMode = view.findViewById(R.id.circleMode)
        tvStatusMode = view.findViewById(R.id.tvStatusMode)

        // POMPA
        pumpLayout = view.findViewById(R.id.pumpLayout)
        switchPump = view.findViewById(R.id.switchPump)
        circlePump = view.findViewById(R.id.circlePump)
        statusSwitch = view.findViewById(R.id.statusSwitch)

        // PH UP
        phUpLayout = view.findViewById(R.id.phUpLayout)
        switchPhUp = view.findViewById(R.id.switchPhUp)
        circlePhUp = view.findViewById(R.id.circlePhUp)
        statusPhUp = view.findViewById(R.id.statusPhUp)

        // PH DOWN
        phDownLayout = view.findViewById(R.id.phDownLayout)
        switchPhDown = view.findViewById(R.id.switchPhDown)
        circlePhDown = view.findViewById(R.id.circlePhDown)
        statusPhDown = view.findViewById(R.id.statusPhDown)

        // NUTRISI UP
        nutrisiUpLayout = view.findViewById(R.id.nutrisiUpLayout)
        edtNutrisiA = view.findViewById(R.id.edtNutrisiA)
        btnTambahA = view.findViewById(R.id.btnTambahA)
        tvEstimasiA = view.findViewById(R.id.tvEstimasiA)
        tvStatusA = view.findViewById(R.id.tvStatusA)

        // NUTRISI DOWN
        nutrisiDownLayout = view.findViewById(R.id.nutrisiDownLayout)
        edtNutrisiB = view.findViewById(R.id.edtNutrisiB)
        btnTambahB = view.findViewById(R.id.btnTambahB)
        tvEstimasiB = view.findViewById(R.id.tvEstimasiB)
        tvStatusB = view.findViewById(R.id.tvStatusB)

        layoutWarning = view.findViewById(R.id.layoutWarning)

        val accPref =
            requireActivity().getSharedPreferences("ACCOUNT", MODE_PRIVATE)

        deviceID = accPref.getString("deviceID", "")

        Log.d("ControlFragment", "DeviceID: $deviceID")

        val baseFirebase = firebaseDatabase.getReference("Hics")
        listenStatusNutrisiA(baseFirebase)
        listenStatusNutrisiB(baseFirebase)

        // ================= GET DATA FIREBASE =================
        if (!deviceID.isNullOrEmpty()) {

            baseFirebase.child(deviceID!!)
                .child("control")
                .addValueEventListener(object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        if (snapshot.exists()) {

                            online = true

                            modeStatus =
                                snapshot.child("mode")
                                    .value.toString().toBoolean()

                            pumpOn =
                                snapshot.child("waterPump")
                                    .value.toString().toBoolean()

                            phUpOn =
                                snapshot.child("phUp")
                                    .value.toString().toBoolean()

                            phDownOn =
                                snapshot.child("phDown")
                                    .value.toString().toBoolean()


                            saveLastState()
                            updateControlUI()
                            setControlEnabled(true)

                        } else {
                            online = false
                            loadLastState()
                            updateControlUI()
                            setControlEnabled(false)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                        Toast.makeText(
                            requireContext(),
                            "Error: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }else{

            online = false
            loadLastState()
            updateControlUI()
            setControlEnabled(false)

        }
// ================= MODE =================
        switchMode.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    requireContext(),
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            vibratePhone()

            modeStatus = !modeStatus
            updateControlUI()

            baseFirebase.child(deviceID!!)
                .child("control")
                .child("mode")
                .setValue(modeStatus)
                .addOnSuccessListener {
                    saveLastState()
                }
                .addOnFailureListener {

                    modeStatus = !modeStatus
                    updateControlUI()

                    Toast.makeText(
                        requireContext(),
                        "Gagal mengubah mode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

// ================= POMPA =================
        switchPump.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    requireContext(),
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            vibratePhone()

            pumpOn = !pumpOn
            pumpSwitchUI(pumpOn)

            baseFirebase.child(deviceID!!)
                .child("control")
                .child("waterPump")
                .setValue(pumpOn)
                .addOnSuccessListener {
                    saveLastState()
                }
                .addOnFailureListener {

                    pumpOn = !pumpOn
                    pumpSwitchUI(pumpOn)

                    Toast.makeText(
                        requireContext(),
                        "Gagal mengubah status pompa",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

// ================= PH UP =================
        switchPhUp.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    requireContext(),
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            vibratePhone()

            phUpOn = !phUpOn
            phUpSwitchUI(phUpOn)

            baseFirebase.child(deviceID!!)
                .child("control")
                .child("phUp")
                .setValue(phUpOn)
                .addOnSuccessListener {
                    saveLastState()
                }
                .addOnFailureListener {

                    phUpOn = !phUpOn
                    phUpSwitchUI(phUpOn)

                    Toast.makeText(
                        requireContext(),
                        "Gagal mengubah PH Up",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

// ================= PH DOWN =================
        switchPhDown.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    requireContext(),
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            vibratePhone()

            phDownOn = !phDownOn
            phDownSwitchUI(phDownOn)

            baseFirebase.child(deviceID!!)
                .child("control")
                .child("phDown")
                .setValue(phDownOn)
                .addOnSuccessListener {
                    saveLastState()
                }
                .addOnFailureListener {

                    phDownOn = !phDownOn
                    phDownSwitchUI(phDownOn)

                    Toast.makeText(
                        requireContext(),
                        "Gagal mengubah PH Down",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

// ================= NUTRISI UP =================
        btnTambahA.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    requireContext(),
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val volume = edtNutrisiA.text.toString().toIntOrNull()

            if (volume == null || volume <= 0) {
                Toast.makeText(
                    requireContext(),
                    "Masukkan volume",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val durasi = volume * 1.4

            // Tetap tampilkan volume dan estimasi
            edtNutrisiA.setText(volume.toString())
            tvEstimasiA.text = "Estimasi : ${durasi.toInt()} detik"

            tvStatusA.text = "Mengirim..."

            btnTambahA.isEnabled = false
            edtNutrisiA.isEnabled = false

            btnTambahA.alpha = 0.5f
            edtNutrisiA.alpha = 0.5f

            val control = baseFirebase.child(deviceID!!)
                .child("control")
                .child("nutrisiA")

            control.child("volume").setValue(volume)
            control.child("start").setValue(true)
            control.child("status").setValue("running")
        }
        btnTambahB.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    requireContext(),
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val volume = edtNutrisiB.text.toString().toIntOrNull()

            if (volume == null || volume <= 0) {
                Toast.makeText(
                    requireContext(),
                    "Masukkan volume",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val durasi = volume * 1.4

            // Tetap tampilkan volume dan estimasi
            edtNutrisiB.setText(volume.toString())
            tvEstimasiB.text = "Estimasi : ${durasi.toInt()} detik"

            tvStatusB.text = "Mengirim..."

            btnTambahB.isEnabled = false
            edtNutrisiB.isEnabled = false

            btnTambahB.alpha = 0.5f
            edtNutrisiB.alpha = 0.5f

            val control = baseFirebase.child(deviceID!!)
                .child("control")
                .child("nutrisiB")

            control.child("volume").setValue(volume)
            control.child("start").setValue(true)
            control.child("status").setValue("running")
        }
        }
    // ================= MODE UI =================
    private fun modeSwitchUI(isOn: Boolean) {

        switchMode.post {

            if (isOn) {

                switchMode.setBackgroundResource(R.drawable.bg_switch_on)

                circleMode.translationX =
                    (switchMode.width - circleMode.width - 12).toFloat()

            } else {

                switchMode.setBackgroundResource(R.drawable.bg_switch_off)

                circleMode.translationX = 0f
            }
        }
    }

    // ================= POMPA UI =================
    private fun pumpSwitchUI(isOn: Boolean) {

        switchPump.post {

            if (isOn) {

                switchPump.setBackgroundResource(R.drawable.bg_switch_on)

                circlePump.translationX =
                    (switchPump.width - circlePump.width - 12).toFloat()

                statusSwitch.text = "ON"

            } else {

                switchPump.setBackgroundResource(R.drawable.bg_switch_off)

                circlePump.translationX = 0f

                statusSwitch.text = "OFF"
            }
        }
    }

    // ================= PH UP UI =================
    private fun phUpSwitchUI(isOn: Boolean) {

        switchPhUp.post {

            if (isOn) {

                switchPhUp.setBackgroundResource(R.drawable.bg_switch_on)

                circlePhUp.translationX =
                    (switchPhUp.width - circlePhUp.width - 12).toFloat()

                statusPhUp.text = "ON"

            } else {

                switchPhUp.setBackgroundResource(R.drawable.bg_switch_off)

                circlePhUp.translationX = 0f

                statusPhUp.text = "OFF"
            }
        }
    }

    // ================= PH DOWN UI =================
    private fun phDownSwitchUI(isOn: Boolean) {

        switchPhDown.post {

            if (isOn) {

                switchPhDown.setBackgroundResource(R.drawable.bg_switch_on)

                circlePhDown.translationX =
                    (switchPhDown.width - circlePhDown.width - 12).toFloat()

                statusPhDown.text = "ON"

            } else {

                switchPhDown.setBackgroundResource(R.drawable.bg_switch_off)

                circlePhDown.translationX = 0f

                statusPhDown.text = "OFF"
            }
        }
    }


    private fun setControlEnabled(enable: Boolean) {

        // Switch tetap bisa diklik agar Toast tetap muncul
        switchMode.isEnabled = true
        switchPump.isEnabled = true
        switchPhUp.isEnabled = true
        switchPhDown.isEnabled = true


        // Efek abu-abu saat offline
        switchMode.alpha = if (enable) 1f else 0.5f
        switchPump.alpha = if (enable) 1f else 0.5f
        switchPhUp.alpha = if (enable) 1f else 0.5f
        switchPhDown.alpha = if (enable) 1f else 0.5f

        pumpLayout.alpha = if (enable) 1f else 0.5f
        phUpLayout.alpha = if (enable) 1f else 0.5f
        phDownLayout.alpha = if (enable) 1f else 0.5f
        nutrisiUpLayout.alpha = if (enable) 1f else 0.5f
        nutrisiDownLayout.alpha = if (enable) 1f else 0.5f

        mode.alpha = if (enable) 1f else 0.5f
        tvStatusMode.alpha = if (enable) 1f else 0.5f

        layoutWarning.visibility =
            if (enable) View.GONE else View.VISIBLE
    }
    private fun saveLastState() {

        val pref =
            requireContext().getSharedPreferences("CONTROL_STATE", MODE_PRIVATE)

        pref.edit()
            .putBoolean("mode", modeStatus)
            .putBoolean("pump", pumpOn)
            .putBoolean("phUp", phUpOn)
            .putBoolean("phDown", phDownOn)
            .apply()
    }
    private fun loadLastState() {

        val pref =
            requireContext().getSharedPreferences("CONTROL_STATE", MODE_PRIVATE)

        modeStatus = pref.getBoolean("mode", false)
        pumpOn = pref.getBoolean("pump", false)
        phUpOn = pref.getBoolean("phUp", false)
        phDownOn = pref.getBoolean("phDown", false)
    }
    private fun updateControlUI() {

        if (modeStatus) {

            mode.text = "Auto"
            modeSwitchUI(true)

            tvStatusMode.visibility = View.VISIBLE

            pumpLayout.visibility = View.GONE
            phUpLayout.visibility = View.GONE
            phDownLayout.visibility = View.GONE
            nutrisiUpLayout.visibility = View.GONE
            nutrisiDownLayout.visibility = View.GONE

        } else {

            mode.text = "Manual"
            modeSwitchUI(false)

            tvStatusMode.visibility = View.GONE

            pumpLayout.visibility = View.VISIBLE
            phUpLayout.visibility = View.VISIBLE
            phDownLayout.visibility = View.VISIBLE
            nutrisiUpLayout.visibility = View.VISIBLE
            nutrisiDownLayout.visibility = View.VISIBLE

            pumpSwitchUI(pumpOn)
            phUpSwitchUI(phUpOn)
            phDownSwitchUI(phDownOn)
        }
    }
    private fun listenStatusNutrisiA(baseFirebase: DatabaseReference) {

        if (deviceID.isNullOrEmpty()) return

        baseFirebase.child(deviceID!!)
            .child("control")
            .child("nutrisiA")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val status = snapshot.child("status")
                        .getValue(String::class.java) ?: "idle"

                    val volume = snapshot.child("volume")
                        .getValue(Int::class.java) ?: 0

                    when (status.lowercase()) {

                        "running" -> {

                            btnTambahA.isEnabled = false
                            edtNutrisiA.isEnabled = false

                            btnTambahA.alpha = 0.5f
                            edtNutrisiA.alpha = 0.5f

                            edtNutrisiA.setText(volume.toString())

                            val durasi = volume * 1.4
                            tvEstimasiA.text = "Estimasi : ${durasi.toInt()} detik"

                            tvStatusA.text = "Sedang berjalan..."
                        }

                        "done" -> {

                            btnTambahA.isEnabled = true
                            edtNutrisiA.isEnabled = true

                            btnTambahA.alpha = 1f
                            edtNutrisiA.alpha = 1f

                            edtNutrisiA.text.clear()
                            tvEstimasiA.text = ""

                            tvStatusA.text = "Selesai"
                        }

                        else -> {

                            btnTambahA.isEnabled = true
                            edtNutrisiA.isEnabled = true

                            btnTambahA.alpha = 1f
                            edtNutrisiA.alpha = 1f

                            if (volume > 0) {
                                edtNutrisiA.setText(volume.toString())

                                val durasi = volume * 1.4
                                tvEstimasiA.text = "Estimasi : ${durasi.toInt()} detik"
                            } else {
                                edtNutrisiA.text.clear()
                                tvEstimasiA.text = ""
                            }

                            tvStatusA.text = "Idle"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun listenStatusNutrisiB(baseFirebase: DatabaseReference) {

        if (deviceID.isNullOrEmpty()) return

        baseFirebase.child(deviceID!!)
            .child("control")
            .child("nutrisiB")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val status =
                        snapshot.child("status")
                            .getValue(String::class.java) ?: "idle"
                    val volume = snapshot.child("volume")
                        .getValue(Int::class.java) ?: 0

                    when (status.lowercase()) {

                        "running" -> {

                            btnTambahB.isEnabled = false
                            edtNutrisiB.isEnabled = false

                            btnTambahB.alpha = 0.5f
                            edtNutrisiB.alpha = 0.5f

                            edtNutrisiB.setText(volume.toString())

                            val durasi = volume * 1.4
                            tvEstimasiB.text = "Estimasi : ${durasi.toInt()} detik"

                            tvStatusB.text = "Sedang berjalan..."
                        }

                        "done" -> {

                            btnTambahB.isEnabled = true
                            edtNutrisiB.isEnabled = true

                            btnTambahB.alpha = 1f
                            edtNutrisiB.alpha = 1f

                            edtNutrisiB.text.clear()
                            tvEstimasiB.text = ""

                            tvStatusB.text = "Selesai"
                        }

                        else -> {

                            btnTambahB.isEnabled = true
                            edtNutrisiB.isEnabled = true

                            btnTambahB.alpha = 1f
                            edtNutrisiB.alpha = 1f

                            if (volume > 0) {
                                edtNutrisiB.setText(volume.toString())

                                val durasi = volume * 1.4
                                tvEstimasiB.text = "Estimasi : ${durasi.toInt()} detik"
                            } else {
                                edtNutrisiB.text.clear()
                                tvEstimasiB.text = ""
                            }

                            tvStatusB.text = "Idle"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ================= GETAR =================
    private fun vibratePhone() {

        val vibrator =
            requireContext().getSystemService(
                Context.VIBRATOR_SERVICE
            ) as Vibrator

        // Android baru
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    40, // lama getar
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } else {

            // Android lama
            vibrator.vibrate(80)
        }
    }
}