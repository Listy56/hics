package com.example.hics

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class CalibrationFragment : Fragment() {

    private lateinit var spSensor: Spinner
    private lateinit var spReference: Spinner

    private lateinit var btnCalibration: Button
    private lateinit var btnCancel: Button

    private lateinit var progressCalibration: ProgressBar

    private lateinit var tvStatus: TextView
    private lateinit var tvAverage: TextView
    private lateinit var tvOffset: TextView
    private lateinit var tvReference: TextView
    private lateinit var tvProgress: TextView
    private var calibrationListener: ValueEventListener? = null

    private lateinit var database: DatabaseReference

    private lateinit var deviceID: String
    private var ignoreSensorChange = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_calibration,
            container,
            false
        )

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pref = requireActivity().getSharedPreferences(
            "ACCOUNT",
            MODE_PRIVATE
        )

        deviceID = pref.getString("deviceID", "") ?: ""
        if (deviceID.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Device belum terhubung",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        database = FirebaseDatabase.getInstance().reference
            .child("Hics")
            .child(deviceID)
            .child("calibration")

        spSensor = view.findViewById(R.id.spSensor)
        spReference = view.findViewById(R.id.spReference)

        btnCalibration = view.findViewById(R.id.btnCalibration)
        btnCancel = view.findViewById(R.id.btnCancel)

        progressCalibration =
            view.findViewById(R.id.progressCalibration)

        tvStatus = view.findViewById(R.id.tvStatus)
        tvAverage = view.findViewById(R.id.tvAverage)
        tvOffset = view.findViewById(R.id.tvOffset)
        tvReference = view.findViewById(R.id.tvReference)
        tvProgress = view.findViewById(R.id.tvProgress)

        progressCalibration.max = 100
        btnCancel.isEnabled = false

        initSpinner()

        listenCalibration()

        btnCalibration.setOnClickListener {

            startCalibration()

        }

        btnCancel.setOnClickListener {

            cancelCalibration()

        }


    }

    private fun initSpinner() {

        val sensorAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(
                "pH",
                "PPM"
            )
        )

        spSensor.adapter = sensorAdapter

        loadReference()

        spReference.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (ignoreSensorChange) return

                    tvReference.text =
                        "Referensi : ${spReference.selectedItem}"
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}

            }

    }

    private fun loadReference(sensor: String = spSensor.selectedItem.toString()) {

        val data =
            if (sensor == "pH") {
                arrayOf(
                    "4.01",
                    "6.86",
                    "7.00",
                    "9.18"
                )
            } else {
                arrayOf(
                    "342",
                    "707",
                    "1413"
                )
            }

        spReference.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            data
        )
    }
    private fun startCalibration() {

        val sensor = spSensor.selectedItem.toString()
        val reference = spReference.selectedItem.toString()

        btnCalibration.isEnabled = false
        btnCancel.isEnabled = true

        spSensor.isEnabled = false
        spReference.isEnabled = false

        progressCalibration.progress = 0

        tvProgress.text = "0 %"
        tvStatus.text = "Mengirim perintah..."

        tvReference.text = "Referensi : $reference"
        val data = HashMap<String, Any>()

        data["sensor"] = sensor
        data["reference"] = reference
        data["command"] = "start"
        data["status"] = "waiting"
        data["progress"] = 0
        data["average"] = 0
        data["timestamp"] = ServerValue.TIMESTAMP

        database.updateChildren(data)
            .addOnSuccessListener {

                tvStatus.text = "Menunggu ESP32..."
            }
            .addOnFailureListener {

                btnCalibration.isEnabled = true
                btnCancel.isEnabled = false

                spSensor.isEnabled = true
                spReference.isEnabled = true

                tvStatus.text = "Gagal mengirim"

                Toast.makeText(
                    requireContext(),
                    "Kalibrasi gagal dimulai",
                    Toast.LENGTH_SHORT
                ).show()

            }

    }

    private fun cancelCalibration() {

        val data = HashMap<String, Any>()

        data["command"] = "cancel"
        data["status"] = "cancel"

        database.updateChildren(data)
            .addOnSuccessListener {

                btnCalibration.isEnabled = true
                btnCancel.isEnabled = false

                progressCalibration.progress = 0

                tvProgress.text = "0 %"

                tvStatus.text = "Kalibrasi dibatalkan"

            }

    }

    private fun listenCalibration() {

        calibrationListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (!snapshot.exists()) return

                val progress =
                    snapshot.child("progress")
                        .getValue(Int::class.java) ?: 0

                val status =
                    snapshot.child("status")
                        .getValue(String::class.java) ?: "idle"

                val sensor =
                    snapshot.child("sensor")
                        .getValue(String::class.java) ?: "pH"

                val reference =
                    snapshot.child("reference")
                        .getValue(String::class.java) ?: ""


                val average = snapshot.child("average").value
                    ?.toString()
                    ?.toDoubleOrNull() ?: 0.0

                val offset = snapshot.child("offset").value
                    ?.toString()
                    ?.toDoubleOrNull() ?: 0.0

                val sensorIndex = if (sensor == "pH") 0 else 1

                ignoreSensorChange = true

                if (spSensor.selectedItemPosition != sensorIndex) {
                    spSensor.setSelection(sensorIndex, false)
                }

                loadReference(sensor)

                val adapter = spReference.adapter as ArrayAdapter<String>

                val index = adapter.getPosition(reference)

                if (index != spReference.selectedItemPosition && index >= 0) {
                    spReference.setSelection(index, false)
                }

                ignoreSensorChange = false

                tvReference.text = "Referensi : $reference"

                progressCalibration.progress = progress.coerceIn(0, 100)

                tvProgress.text = "$progress %"

                tvAverage.text =
                    "Rata-rata Sensor : %.2f".format(average)

                tvOffset.text =
                    "Offset : %.3f".format(offset)
                val running = status in listOf(
                    "waiting",
                    "reading",
                    "averaging",
                    "calculating",
                    "saving"
                )

                btnCalibration.isEnabled = !running
                btnCancel.isEnabled = running

                spSensor.isEnabled = !running
                spReference.isEnabled = !running

                when (status) {

                    "idle" -> {

                        tvStatus.text = "Siap dikalibrasi"

                        btnCalibration.isEnabled = true
                        btnCancel.isEnabled = false

                        spSensor.isEnabled = true
                        spReference.isEnabled = true

                        progressCalibration.progress = 0
                        tvProgress.text = "0 %"
                    }

                    "waiting" -> {

                        tvStatus.text = "Menunggu ESP32..."

                    }

                    "reading" -> {

                        tvStatus.text = "Membaca Sensor..."

                    }

                    "averaging" -> {

                        tvStatus.text = "Menghitung Rata-rata..."

                    }

                    "calculating" -> {

                        tvStatus.text = "Menghitung Offset..."

                    }

                    "saving" -> {

                        tvStatus.text = "Menyimpan Kalibrasi..."

                    }

                    "done" -> {

                        tvStatus.text = "✅ Kalibrasi Berhasil"

                        btnCalibration.isEnabled = true
                        btnCancel.isEnabled = false

                        spSensor.isEnabled = true
                        spReference.isEnabled = true

                        progressCalibration.progress = 100
                        tvProgress.text = "100 %"
                    }

                    "cancel" -> {

                        tvStatus.text = "Kalibrasi Dibatalkan"

                        btnCalibration.isEnabled = true

                        btnCancel.isEnabled = false
                        spSensor.isEnabled = true
                        spReference.isEnabled = true

                        progressCalibration.progress = 0

                        tvProgress.text = "0 %"

                    }

                    "error" -> {

                        tvStatus.text = "Kalibrasi Gagal"

                        btnCalibration.isEnabled = true

                        btnCancel.isEnabled = false
                        spSensor.isEnabled = true
                        spReference.isEnabled = true

                    }

                }

            }

            override fun onCancelled(error: DatabaseError) {

            }
        }
        database.addValueEventListener(calibrationListener!!)


    }

    override fun onDestroyView() {
        super.onDestroyView()

        calibrationListener?.let {
            database.removeEventListener(it)
        }
    }
}