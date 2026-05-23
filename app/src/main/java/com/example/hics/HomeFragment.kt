package com.example.hics

import android.animation.ValueAnimator
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private lateinit var phTextView: TextView
    private lateinit var nutrisiTextView: TextView
    private lateinit var statusSwitch: TextView
    private lateinit var intensitas: TextView
    private lateinit var airTemp: TextView
    private lateinit var waterTemp: TextView
    private lateinit var waterLevelPercent: TextView
    private lateinit var waterLevel: LinearLayout
    private lateinit var baseWaterLevel: FrameLayout

    private var isOn = true
    private var deviceID: String? = ""

    private var firebaseDatabase = FirebaseDatabase.getInstance()

    var suhuUdara = 0.0
    var suhuAir = 0.0
    var pH = 0.0
    var nutrisi = 0
    var intensitasCahaya = 0
    var level = 0.0

    var waterAnimator: ValueAnimator? = null

    // ================= SETTING =================

    private var phMinSetting = 5.5
    private var phMaxSetting = 7.5

    private var ppmMinSetting = 800
    private var ppmMaxSetting = 1500

    private var notifAlert = true
    private var tempUnit = "C"

    // ================= STATUS NOTIF =================

    private var phHighSent = false
    private var phLowSent = false

    private var ppmHighSent = false
    private var ppmLowSent = false

    private var waterTempHighSent = false
    private var waterTempLowSent = false

    private var airTempHighSent = false
    private var airTempLowSent = false

    private var waterLevelLowSent = false

    // ================= COOLDOWN =================

    private var lastNotifTime = 0L

    private val NOTIF_COOLDOWN = 15000L
    // 15 detik

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) {

                requireActivity().finish()
            }

        phTextView = view.findViewById(R.id.tvPh)
        nutrisiTextView = view.findViewById(R.id.tvNutrisi)
        statusSwitch = view.findViewById(R.id.statusSwitch)
        intensitas = view.findViewById(R.id.tvIntensitas)
        airTemp = view.findViewById(R.id.airTemp)
        waterTemp = view.findViewById(R.id.waterTemp)
        waterLevel = view.findViewById(R.id.waterLevel)
        baseWaterLevel = view.findViewById(R.id.baseWaterLevel)
        waterLevelPercent = view.findViewById(R.id.waterLevelPercent)

        val accPref =
            requireActivity()
                .getSharedPreferences(
                    "ACCOUNT",
                    MODE_PRIVATE
                )

        deviceID =
            accPref.getString("deviceID", "")

        Log.d("HomeFragment", "DeviceID: $deviceID")

        val baseFirebase =
            firebaseDatabase.getReference("Hics")

        if (!deviceID.isNullOrEmpty()) {

            baseFirebase
                .child(deviceID!!)
                .addValueEventListener(object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        if (!snapshot.exists()) return

                        // ================= AMBIL SETTING =================

                        phMinSetting =
                            snapshot.child("setting")
                                .child("phMin")
                                .value.toString()
                                .toDoubleOrNull() ?: 5.5

                        phMaxSetting =
                            snapshot.child("setting")
                                .child("phMax")
                                .value.toString()
                                .toDoubleOrNull() ?: 7.5

                        ppmMinSetting =
                            snapshot.child("setting")
                                .child("ppmMin")
                                .value.toString()
                                .toIntOrNull() ?: 800

                        ppmMaxSetting =
                            snapshot.child("setting")
                                .child("ppmMax")
                                .value.toString()
                                .toIntOrNull() ?: 1500

                        notifAlert =
                            snapshot.child("setting")
                                .child("notifAlert")
                                .value.toString()
                                .toBoolean()

                        tempUnit =
                            snapshot.child("setting")
                                .child("tempUnit")
                                .value.toString()

                        // ================= DATA SENSOR =================

                        suhuAir =
                            snapshot.child("dataStream")
                                .child("waterTemp")
                                .value.toString()
                                .toDoubleOrNull() ?: 0.0

                        suhuUdara =
                            snapshot.child("dataStream")
                                .child("airTemp")
                                .value.toString()
                                .toDoubleOrNull() ?: 0.0

                        pH =
                            snapshot.child("dataStream")
                                .child("pH")
                                .value.toString()
                                .toDoubleOrNull() ?: 0.0

                        nutrisi =
                            snapshot.child("dataStream")
                                .child("ppm")
                                .value.toString()
                                .toIntOrNull() ?: 0

                        level =
                            snapshot.child("dataStream")
                                .child("waterLevel")
                                .value.toString()
                                .toDoubleOrNull() ?: 0.0

                        intensitasCahaya =
                            snapshot.child("dataStream")
                                .child("light")
                                .value.toString()
                                .toIntOrNull() ?: 0

                        isOn =
                            snapshot.child("control")
                                .child("waterPump")
                                .value.toString()
                                .toBoolean()

                        // ================= NOTIFIKASI =================

                        if (notifAlert) {

                            // SUHU AIR TINGGI
                            checkNotification(
                                condition = suhuAir > 30,
                                flag = waterTempHighSent,
                                title = "Suhu Air Tinggi",
                                message = "Suhu air terlalu panas : $suhuAir°C",
                                onSent = {
                                    waterTempHighSent = true
                                },
                                onReset = {
                                    waterTempHighSent = false
                                }
                            )

                            // SUHU AIR RENDAH
                            checkNotification(
                                condition = suhuAir < 18,
                                flag = waterTempLowSent,
                                title = "Suhu Air Rendah",
                                message = "Suhu air terlalu dingin : $suhuAir°C",
                                onSent = {
                                    waterTempLowSent = true
                                },
                                onReset = {
                                    waterTempLowSent = false
                                }
                            )

                            // SUHU UDARA TINGGI
                            checkNotification(
                                condition = suhuUdara > 35,
                                flag = airTempHighSent,
                                title = "Suhu Udara Tinggi",
                                message = "Suhu udara terlalu panas : $suhuUdara°C",
                                onSent = {
                                    airTempHighSent = true
                                },
                                onReset = {
                                    airTempHighSent = false
                                }
                            )

                            // SUHU UDARA RENDAH
                            checkNotification(
                                condition = suhuUdara < 20,
                                flag = airTempLowSent,
                                title = "Suhu Udara Rendah",
                                message = "Suhu udara terlalu dingin : $suhuUdara°C",
                                onSent = {
                                    airTempLowSent = true
                                },
                                onReset = {
                                    airTempLowSent = false
                                }
                            )

                            // pH TINGGI
                            checkNotification(
                                condition = pH > phMaxSetting,
                                flag = phHighSent,
                                title = "pH Tinggi",
                                message = "Nilai pH terlalu tinggi : $pH",
                                onSent = {
                                    phHighSent = true
                                },
                                onReset = {
                                    phHighSent = false
                                }
                            )

                            // pH RENDAH
                            checkNotification(
                                condition = pH < phMinSetting,
                                flag = phLowSent,
                                title = "pH Rendah",
                                message = "Nilai pH terlalu rendah : $pH",
                                onSent = {
                                    phLowSent = true
                                },
                                onReset = {
                                    phLowSent = false
                                }
                            )

                            // PPM TINGGI
                            checkNotification(
                                condition = nutrisi > ppmMaxSetting,
                                flag = ppmHighSent,
                                title = "Nutrisi Tinggi",
                                message = "PPM terlalu tinggi : $nutrisi",
                                onSent = {
                                    ppmHighSent = true
                                },
                                onReset = {
                                    ppmHighSent = false
                                }
                            )

                            // PPM RENDAH
                            checkNotification(
                                condition = nutrisi < ppmMinSetting,
                                flag = ppmLowSent,
                                title = "Nutrisi Rendah",
                                message = "PPM terlalu rendah : $nutrisi",
                                onSent = {
                                    ppmLowSent = true
                                },
                                onReset = {
                                    ppmLowSent = false
                                }
                            )

                            // AIR RENDAH
                            checkNotification(
                                condition = level < 20,
                                flag = waterLevelLowSent,
                                title = "Air Rendah",
                                message = "Level air tinggal $level%",
                                onSent = {
                                    waterLevelLowSent = true
                                },
                                onReset = {
                                    waterLevelLowSent = false
                                }
                            )
                        }

                        // ================= BATAS WATER LEVEL =================

                        if (level < 15) {
                            level = 15.0
                        } else if (level > 100) {
                            level = 100.0
                        }

                        // ================= KONVERSI SUHU =================

                        var displayAirTemp = suhuUdara
                        var displayWaterTemp = suhuAir
                        var unit = "°C"

                        if (tempUnit == "F") {

                            displayAirTemp =
                                (suhuUdara * 9 / 5) + 32

                            displayWaterTemp =
                                (suhuAir * 9 / 5) + 32

                            unit = "°F"
                        }

                        // ================= UI =================

                        phTextView.text = pH.toString()

                        nutrisiTextView.text =
                            nutrisi.toString()

                        airTemp.text =
                            String.format(
                                "%.1f%s",
                                displayAirTemp,
                                unit
                            )

                        waterTemp.text =
                            String.format(
                                "%.1f%s",
                                displayWaterTemp,
                                unit
                            )

                        intensitas.text =
                            intensitasCahaya.toString()

                        statusSwitch.text =
                            if (isOn) "ON" else "OFF"

                        // ================= WATER LEVEL =================

                        baseWaterLevel.post {

                            val maxHeight =
                                baseWaterLevel.height

                            val newHeight =
                                (level * maxHeight) / 100.0

                            waterLevelPercent.text =
                                "$level%"

                            animateWaterLevel(
                                newHeight.toInt()
                            )
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
        }
    }

    // ================= CEK COOLDOWN =================

    private fun canSendNotif(): Boolean {

        val currentTime =
            System.currentTimeMillis()

        return if (
            currentTime - lastNotifTime
            > NOTIF_COOLDOWN
        ) {

            lastNotifTime = currentTime

            true

        } else {

            false
        }
    }

    // ================= HELPER NOTIF =================

    private fun checkNotification(
        condition: Boolean,
        flag: Boolean,
        title: String,
        message: String,
        onSent: () -> Unit,
        onReset: () -> Unit
    ) {

        if (condition) {

            if (!flag && canSendNotif()) {

                NotificationHelper.saveNotification(
                    deviceID!!,
                    title,
                    message
                )

                NotificationHelper.showNotification(
                    requireContext(),
                    title,
                    message
                )

                onSent()
            }

        } else {

            onReset()
        }
    }

    // ================= ANIMASI WATER =================

    private fun animateWaterLevel(targetHeight: Int) {

        waterAnimator?.cancel()

        val startHeight =
            waterLevel.height

        waterAnimator =
            ValueAnimator.ofInt(
                startHeight,
                targetHeight
            ).apply {

                duration = 300

                interpolator =
                    DecelerateInterpolator()

                addUpdateListener {

                    val params =
                        waterLevel.layoutParams

                    params.height =
                        it.animatedValue as Int

                    waterLevel.layoutParams =
                        params
                }

                start()
            }
    }
}