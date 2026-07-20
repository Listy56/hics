package com.example.hics

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import kotlin.random.Random

class CalibrationFragment : Fragment() {

    private lateinit var spSensor: Spinner
    private lateinit var spReference: Spinner // tidak lagi dipakai user, disembunyikan

    private lateinit var btnCalibration: Button
    private lateinit var btnCancel: Button

    private lateinit var progressCalibration: ProgressBar

    private lateinit var tvStatus: TextView
    private lateinit var tvReference: TextView
    private lateinit var tvProgress: TextView
    private var calibrationListener: ValueEventListener? = null

    private lateinit var database: DatabaseReference

    private lateinit var deviceID: String
    private var ignoreSensorChange = false

    private var rawDataListener: ValueEventListener? = null

    // ---- Konstanta kalibrasi ----
    private val TDS_REFERENCE = 1382.0
    private val PH_BUFFERS = listOf(4.0, 6.86, 9.18)

    // ---- State kalibrasi pH multi-step ----
    private data class PhSample(val average: Double, val reference: Double)
    private val phSamples = mutableListOf<PhSample>()
    private var phStepIndex = 0

    // true ketika sedang menunggu user pindah buffer & klik "Kalibrasi" lagi.
    // Selama true, listenCalibration() TIDAK BOLEH mengubah state tombol,
    // supaya status sisa dari ESP (mis. "waiting"/"reading") tidak menimpa
    // tombol yang sudah sengaja kita enable.
    private var awaitingNextPhStep = false

    private lateinit var btDebug : Button

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

        val sensorAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_black,
            arrayOf("pH", "PPM")
        )
        sensorAdapter.setDropDownViewResource(R.layout.spinner_item_black)

        spSensor.adapter = sensorAdapter

        spReference = view.findViewById(R.id.spReference)

        btnCalibration = view.findViewById(R.id.btnCalibration)
        btnCancel = view.findViewById(R.id.btnCancel)

        progressCalibration = view.findViewById(R.id.progressCalibration)

        tvStatus = view.findViewById(R.id.tvStatus)
        tvReference = view.findViewById(R.id.tvReference)
        tvProgress = view.findViewById(R.id.tvProgress)

        btDebug = view.findViewById(R.id.btDebug)

        progressCalibration.max = 100
        btnCancel.isEnabled = false

        // spReference tidak lagi dipilih manual oleh user
        spReference.visibility = View.GONE

        initSpinner()
        resetCalibrationOnEntry()
//        listenCalibration()

        btnCalibration.setOnClickListener {
            startCalibration()
        }

        btnCancel.setOnClickListener {
            cancelCalibration()
        }

        btDebug.setOnClickListener {
            val rawData = hashMapOf<String, Int>()

            for (i in 0..30) {
                rawData["data$i"] = Random.nextInt(1378, 1390)
            }

            database.child("rawData")
                .setValue(rawData)
        }
    }

    private fun initSpinner() {

        val sensorAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("pH", "PPM")
        )

        spSensor.adapter = sensorAdapter

        updateReferenceInfoText()

        spSensor.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // ganti sensor -> reset progress kalibrasi pH kalau lagi jalan
                    resetPhCalibrationState()
                    updateReferenceInfoText()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun resetCalibrationOnEntry() {

        resetPhCalibrationState()   // reset phSamples, phStepIndex, awaitingNextPhStep

        btnCalibration.isEnabled = false
        btnCancel.isEnabled = false
        spSensor.isEnabled = false
        tvStatus.text = "Menyiapkan..."
        progressCalibration.progress = 0
        tvProgress.text = "0 %"

        val resetData = HashMap<String, Any?>()
        resetData["command"] = "idle"
        resetData["status"] = "idle"
        resetData["progress"] = 0
        resetData["average"] = 0
        resetData["currentData"] = 0
        resetData["rawData"] = null

        database.updateChildren(resetData)
            .addOnCompleteListener {
                listenCalibration()   // baru pasang listener SETELAH reset selesai

                if (!it.isSuccessful) {
                    resetCalibrationUIEnabled()
                    tvStatus.text = "Siap dikalibrasi"
                }
            }
    }

    private fun updateReferenceInfoText() {
        val sensor = spSensor.selectedItem?.toString() ?: "pH"
        tvReference.text = if (sensor == "PPM") {
            "Referensi : $TDS_REFERENCE ppm"
        } else {
            val current = PH_BUFFERS.getOrNull(phStepIndex) ?: PH_BUFFERS.first()
            "Referensi : pH $current  (Sampel ${phStepIndex + 1}/${PH_BUFFERS.size})"
        }
    }

    // ================= ROUTING =================

    private fun startCalibration() {
        val sensor = spSensor.selectedItem.toString()

        when (sensor) {
            "PPM" -> startCalibrationPPM()
            "pH" -> startCalibrationPhStep()
        }
    }

    // ================= PPM =================

    private fun startCalibrationPPM() {

        btnCalibration.isEnabled = false
        btnCancel.isEnabled = true
        spSensor.isEnabled = false

        progressCalibration.progress = 0
        tvProgress.text = "0 %"
        tvStatus.text = "Mengirim perintah..."
        tvReference.text = "Referensi : $TDS_REFERENCE ppm"

        val data = HashMap<String, Any?>()
        data["sensor"] = "PPM"
        data["reference"] = TDS_REFERENCE.toString()
        data["command"] = "start"
        data["status"] = "waiting"
        data["progress"] = 0
        data["average"] = 0
        data["currentData"] = 0
        data["rawData"] = null
        data["timestamp"] = ServerValue.TIMESTAMP

        database.updateChildren(data)
            .addOnSuccessListener {
                listenCalibrationRawDataPPM()
            }
            .addOnFailureListener {
                resetCalibrationUIEnabled()
                tvStatus.text = "Gagal mengirim"
                Toast.makeText(
                    requireContext(),
                    "Kalibrasi gagal dimulai",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun listenCalibrationRawDataPPM() {
        val rawDataRef = database.child("rawData")

        rawDataListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()

                val progress = ((count.toFloat() / 31f) * 100).toInt()
                progressCalibration.progress = progress
                tvProgress.text = "$progress %"
                tvStatus.text = "Menerima data... ($count/31)"

                if (count >= 31) {
                    rawDataRef.removeEventListener(this)
                    rawDataListener = null
                    processCalibrationPPM(snapshot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                tvStatus.text = "Gagal membaca data"
                resetCalibrationUIEnabled()
            }
        }
        rawDataRef.addValueEventListener(rawDataListener!!)
    }

    private fun processCalibrationPPM(snapshot: DataSnapshot) {

        val values = mutableListOf<Double>()
        for (i in 0 until 31) {
            val v = snapshot.child("data$i").getValue(Double::class.java)
            if (v != null) values.add(v)
        }

        if (values.size < 31) {
            tvStatus.text = "Data tidak lengkap (${values.size}/31)"
            resetCalibrationUIEnabled()
            return
        }

        val sorted = values.sorted()
        val middle = sorted.subList(10, 21) // 11 nilai tengah
        val average = middle.average()

        tvStatus.text = "Mengambil faktor pengali..."

        val tdsFactorRef = FirebaseDatabase.getInstance()
            .getReference("Hics/$deviceID/setting/tdsFactor")

        tdsFactorRef.get()
            .addOnSuccessListener { factorSnapshot ->
                val faktorLama = factorSnapshot.getValue(Double::class.java)

                if (faktorLama == null || faktorLama == 0.0) {
                    tvStatus.text = "Faktor pengali tidak valid"
                    resetCalibrationUIEnabled()
                    return@addOnSuccessListener
                }

                val x = average / faktorLama

                if (x == 0.0) {
                    tvStatus.text = "Nilai X = 0, kalibrasi gagal"
                    resetCalibrationUIEnabled()
                    return@addOnSuccessListener
                }

                val y = TDS_REFERENCE / x

                database.updateChildren(
                    hashMapOf<String, Any>(
                        "average" to average,
                        "status" to "done",
                        "progress" to 100
                    )
                )

                tdsFactorRef.setValue(y)
                    .addOnSuccessListener {
                        progressCalibration.progress = 100
                        tvProgress.text = "100 %"
                        tvStatus.text = "Kalibrasi PPM selesai"
                        resetCalibrationUIEnabled()

                        Toast.makeText(
                            requireContext(),
                            "Kalibrasi berhasil, faktor baru: $y",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        tvStatus.text = "Gagal menyimpan faktor baru"
                        resetCalibrationUIEnabled()
                    }
            }
            .addOnFailureListener {
                tvStatus.text = "Gagal mengambil faktor pengali"
                resetCalibrationUIEnabled()
            }
    }

    // ================= pH (3 langkah otomatis) =================

    private fun startCalibrationPhStep() {

        awaitingNextPhStep = false

        val referenceValue = PH_BUFFERS.getOrNull(phStepIndex)
        if (referenceValue == null) {
            // harusnya tidak pernah kejadian, tapi jaga-jaga
            resetPhCalibrationState()
            resetCalibrationUIEnabled()
            return
        }

        btnCalibration.isEnabled = false
        btnCancel.isEnabled = true
        spSensor.isEnabled = false

        progressCalibration.progress = 0
        tvProgress.text = "0 %"
        tvStatus.text = "Mengirim perintah... (Sampel ${phStepIndex + 1}/${PH_BUFFERS.size})"
        tvReference.text = "Referensi : pH $referenceValue  (Sampel ${phStepIndex + 1}/${PH_BUFFERS.size})"

        val data = HashMap<String, Any?>()
        data["sensor"] = "pH"
        data["reference"] = referenceValue.toString()
        data["command"] = "start"
        data["status"] = "waiting"
        data["progress"] = 0
        data["average"] = 0
        data["currentData"] = 0
        data["rawData"] = null
        data["timestamp"] = ServerValue.TIMESTAMP

        database.updateChildren(data)
            .addOnSuccessListener {
                tvStatus.text = "Menunggu ESP32... (Sampel ${phStepIndex + 1}/${PH_BUFFERS.size})"
                listenPhRawData(referenceValue)
            }
            .addOnFailureListener {
                resetCalibrationUIEnabled()
                tvStatus.text = "Gagal mengirim"
            }
    }

    private fun listenPhRawData(referenceValue: Double) {
        val rawDataRef = database.child("rawData")

        rawDataListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()

                val progress = ((count.toFloat() / 31f) * 100).toInt()
                progressCalibration.progress = progress
                tvProgress.text = "$progress %"
                tvStatus.text = "Menerima data... ($count/31) — Sampel ${phStepIndex + 1}/${PH_BUFFERS.size}"

                if (count >= 31) {
                    rawDataRef.removeEventListener(this)
                    rawDataListener = null
                    processPhSample(snapshot, referenceValue)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                tvStatus.text = "Gagal membaca data"
                resetCalibrationUIEnabled()
            }
        }
        rawDataRef.addValueEventListener(rawDataListener!!)
    }

    private fun processPhSample(snapshot: DataSnapshot, referenceValue: Double) {

        val values = mutableListOf<Double>()
        for (i in 0 until 31) {
            val v = snapshot.child("data$i").getValue(Double::class.java)
            if (v != null) values.add(v)
        }

        if (values.size < 31) {
            tvStatus.text = "Data tidak lengkap (${values.size}/31)"
            resetCalibrationUIEnabled()
            return
        }

        val sorted = values.sorted()
        val middle = sorted.subList(10, 21)
        val average = middle.average()

        phSamples.add(PhSample(average, referenceValue))
        phStepIndex++

        database.updateChildren(
            hashMapOf<String, Any>(
                "average" to average,
                "status" to "sample_done"
            )
        )

        if (phStepIndex < PH_BUFFERS.size) {
            val nextBuffer = PH_BUFFERS[phStepIndex]
            tvStatus.text = "Sampel $phStepIndex/${PH_BUFFERS.size} selesai. " +
                    "Celupkan probe ke buffer pH $nextBuffer, lalu tekan Kalibrasi lagi."
            tvReference.text = "Referensi : pH $nextBuffer  (Sampel ${phStepIndex + 1}/${PH_BUFFERS.size})"

            awaitingNextPhStep = true // kunci UI dari campur tangan listenCalibration()

            btnCalibration.isEnabled = true
            btnCancel.isEnabled = true
            spSensor.isEnabled = false // tetap terkunci ke pH sampai 3 sampel selesai
        } else {
            finishPhCalibration()
        }
    }

    private fun finishPhCalibration() {

        tvStatus.text = "Menghitung slope & offset..."

        val n = phSamples.size
        val sumX = phSamples.sumOf { it.average }
        val sumY = phSamples.sumOf { it.reference }
        val sumXY = phSamples.sumOf { it.average * it.reference }
        val sumX2 = phSamples.sumOf { it.average * it.average }

        val denominator = (n * sumX2) - (sumX * sumX)

        if (denominator == 0.0) {
            tvStatus.text = "Data pH tidak valid, kalibrasi gagal"
            resetPhCalibrationState()
            resetCalibrationUIEnabled()
            return
        }

        val slope = ((n * sumXY) - (sumX * sumY)) / denominator
        val offset = (sumY - (slope * sumX)) / n

        val phSlopeRef = FirebaseDatabase.getInstance()
            .getReference("Hics/$deviceID/setting/phSlope")
        val phOffsetRef = FirebaseDatabase.getInstance()
            .getReference("Hics/$deviceID/setting/phOffset")

        phSlopeRef.setValue(slope)
            .addOnSuccessListener {
                phOffsetRef.setValue(offset)
                    .addOnSuccessListener {
                        database.updateChildren(
                            hashMapOf<String, Any>(
                                "status" to "done",
                                "progress" to 100
                            )
                        )

                        progressCalibration.progress = 100
                        tvProgress.text = "100 %"
                        tvStatus.text = "Kalibrasi pH selesai"

                        Toast.makeText(
                            requireContext(),
                            "Slope: $slope, Offset: $offset",
                            Toast.LENGTH_LONG
                        ).show()

                        resetPhCalibrationState()
                        resetCalibrationUIEnabled()
                    }
                    .addOnFailureListener {
                        tvStatus.text = "Gagal menyimpan phOffset"
                        resetCalibrationUIEnabled()
                    }
            }
            .addOnFailureListener {
                tvStatus.text = "Gagal menyimpan phSlope"
                resetCalibrationUIEnabled()
            }
    }

    private fun resetPhCalibrationState() {
        phSamples.clear()
        phStepIndex = 0
        awaitingNextPhStep = false
    }

    // ================= UMUM =================

    private fun resetCalibrationUIEnabled() {
        btnCalibration.isEnabled = true
        btnCancel.isEnabled = false
        spSensor.isEnabled = true
        updateReferenceInfoText()
    }

    private fun cancelCalibration() {

        rawDataListener?.let {
            database.child("rawData").removeEventListener(it)
            rawDataListener = null
        }

        resetPhCalibrationState()

        val data = HashMap<String, Any>()
        data["command"] = "cancel"
        data["status"] = "cancel"

        database.updateChildren(data)
            .addOnSuccessListener {
                btnCalibration.isEnabled = true
                btnCancel.isEnabled = false
                spSensor.isEnabled = true

                progressCalibration.progress = 0
                tvProgress.text = "0 %"
                tvStatus.text = "Kalibrasi dibatalkan"
                updateReferenceInfoText()
            }
    }

    private fun listenCalibration() {

        calibrationListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (!snapshot.exists()) return

                val status =
                    snapshot.child("status").getValue(String::class.java) ?: "idle"

                val sensor =
                    snapshot.child("sensor").getValue(String::class.java) ?: "pH"

                val sensorIndex = if (sensor == "pH") 0 else 1

                ignoreSensorChange = true
                if (spSensor.selectedItemPosition != sensorIndex) {
                    spSensor.setSelection(sensorIndex, false)
                }
                ignoreSensorChange = false

                // Lagi jeda menunggu user pindah buffer & klik lanjut ke sampel
                // berikutnya -> jangan biarkan status sisa dari ESP (waiting/
                // reading/dst yang datang telat) menimpa tombol yang sudah
                // sengaja kita enable di processPhSample().
                if (awaitingNextPhStep) return

                val running = status in listOf(
                    "waiting",
                    "reading",
                    "averaging",
                    "calculating",
                    "saving"
                )

                // Saat sedang berjalan (dikirim ESP), kunci UI kecuali tombol cancel
                if (running) {
                    btnCalibration.isEnabled = false
                    btnCancel.isEnabled = true
                    spSensor.isEnabled = false
                }

                when (status) {

                    "idle" -> {
                        tvStatus.text = "Siap dikalibrasi"
                        resetCalibrationUIEnabled()
                        progressCalibration.progress = 0
                        tvProgress.text = "0 %"
                    }

                    "reading" -> tvStatus.text = "Membaca Sensor..."
                    "averaging" -> tvStatus.text = "Menghitung Rata-rata..."
                    "calculating" -> tvStatus.text = "Menghitung Offset..."
                    "saving" -> tvStatus.text = "Menyimpan Kalibrasi..."

                    "done" -> {
                        tvStatus.text = "✅ Kalibrasi Berhasil"
                        resetCalibrationUIEnabled()
                        progressCalibration.progress = 100
                        tvProgress.text = "100 %"
                    }

                    "cancel" -> {
                        tvStatus.text = "Kalibrasi Dibatalkan"
                        resetCalibrationUIEnabled()
                        progressCalibration.progress = 0
                        tvProgress.text = "0 %"
                    }

                    "error" -> {
                        tvStatus.text = "Kalibrasi Gagal"
                        resetCalibrationUIEnabled()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        database.addValueEventListener(calibrationListener!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        calibrationListener?.let { database.removeEventListener(it) }
        rawDataListener?.let { database.child("rawData").removeEventListener(it) }
    }
}


//package com.example.hics
//
//import android.content.Context.MODE_PRIVATE
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.*
//import androidx.fragment.app.Fragment
//import com.google.firebase.database.*
//
//class CalibrationFragment : Fragment() {
//
//    private lateinit var spSensor: Spinner
//    private lateinit var spReference: Spinner
//
//    private lateinit var btnCalibration: Button
//    private lateinit var btnCancel: Button
//
//    private lateinit var progressCalibration: ProgressBar
//
//    private lateinit var tvStatus: TextView
//    private lateinit var tvReference: TextView
//    private lateinit var tvProgress: TextView
//    private var calibrationListener: ValueEventListener? = null
//
//    private lateinit var database: DatabaseReference
//
//    private lateinit var deviceID: String
//    private var ignoreSensorChange = false
//
//    private var rawDataListener: ValueEventListener? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//
//        return inflater.inflate(
//            R.layout.fragment_calibration,
//            container,
//            false
//        )
//
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val pref = requireActivity().getSharedPreferences(
//            "ACCOUNT",
//            MODE_PRIVATE
//        )
//
//        deviceID = pref.getString("deviceID", "") ?: ""
//        if (deviceID.isBlank()) {
//            Toast.makeText(
//                requireContext(),
//                "Device belum terhubung",
//                Toast.LENGTH_SHORT
//            ).show()
//            return
//        }
//
//        database = FirebaseDatabase.getInstance().reference
//            .child("Hics")
//            .child(deviceID)
//            .child("calibration")
//
//        spSensor = view.findViewById(R.id.spSensor)
//        spReference = view.findViewById(R.id.spReference)
//
//        btnCalibration = view.findViewById(R.id.btnCalibration)
//        btnCancel = view.findViewById(R.id.btnCancel)
//
//        progressCalibration =
//            view.findViewById(R.id.progressCalibration)
//
//        tvStatus = view.findViewById(R.id.tvStatus)
//        tvReference = view.findViewById(R.id.tvReference)
//        tvProgress = view.findViewById(R.id.tvProgress)
//
//        progressCalibration.max = 100
//        btnCancel.isEnabled = false
//
//        initSpinner()
//
//        listenCalibration()
//
//        btnCalibration.setOnClickListener {
//            startCalibration()
//        }
//
//        btnCancel.setOnClickListener {
//            cancelCalibration()
//        }
//    }
//
//    private fun initSpinner() {
//
//        val sensorAdapter = ArrayAdapter(
//            requireContext(),
//            android.R.layout.simple_spinner_dropdown_item,
//            arrayOf(
//                "pH",
//                "PPM"
//            )
//        )
//
//        spSensor.adapter = sensorAdapter
//
//        loadReference(spSensor.selectedItem.toString())
//
//        spSensor.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//
//                override fun onItemSelected(
//                    parent: AdapterView<*>?,
//                    view: View?,
//                    position: Int,
//                    id: Long
//                ) {
//                    val selectedSensor = spSensor.selectedItem.toString()
//                    loadReference(selectedSensor)
//                }
//
//                override fun onNothingSelected(parent: AdapterView<*>?) {}
//            }
//
//        spReference.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//
//                override fun onItemSelected(
//                    parent: AdapterView<*>?,
//                    view: View?,
//                    position: Int,
//                    id: Long
//                ) {
//
//                    if (ignoreSensorChange) return
//
//                    tvReference.text =
//                        "Referensi : ${spReference.selectedItem}"
//                }
//                override fun onNothingSelected(parent: AdapterView<*>?) {}
//
//            }
//    }
//
////    private fun loadReference(sensor: String) {
////
////        val data =
////            if (sensor == "pH") {
////                arrayOf(
////                    "4.0",
////                    "6.86",
////                    "9.18"
////                )
////            } else {
////                arrayOf(
////                    "1382"
////                )
////            }
////
////        spReference.adapter = ArrayAdapter(
////            requireContext(),
////            android.R.layout.simple_spinner_dropdown_item,
////            data
////        )
////    }
//
//    private fun loadReference(sensor: String) {
//        val referenceList = when (sensor) {
//            "pH" -> arrayOf("4.0", "6.86", "9.18")
//            "PPM" -> arrayOf("1382")
//            else -> arrayOf()
//        }
//
//        val referenceAdapter = ArrayAdapter(
//            requireContext(),
//            android.R.layout.simple_spinner_dropdown_item,
//            referenceList
//        )
//
//        spReference.adapter = referenceAdapter   // replace, bukan append
//    }
//    private fun startCalibration() {
//
//        val sensor = spSensor.selectedItem.toString()
//        val reference = spReference.selectedItem.toString()
//
//        btnCalibration.isEnabled = false
//        btnCancel.isEnabled = true
//
//        spSensor.isEnabled = false
//        spReference.isEnabled = false
//
//        progressCalibration.progress = 0
//
//        tvProgress.text = "0 %"
//        tvStatus.text = "Mengirim perintah..."
//
//        tvReference.text = "Referensi : $reference"
//
//        val data = HashMap<String, Any>()
//
//        data["sensor"] = sensor
//        data["reference"] = reference
//        data["command"] = "start"
//        data["status"] = "waiting"
//        data["progress"] = 0
//        data["average"] = 0
//        data["currentData"] = 0
//        data["rawData"] = 0
//        data["timestamp"] = ServerValue.TIMESTAMP
//
//        database.updateChildren(data)
//            .addOnSuccessListener {
//
//                listenCalibrationRawData(sensor, reference)
//            }
//            .addOnFailureListener {
//
//                btnCalibration.isEnabled = true
//                btnCancel.isEnabled = false
//
//                spSensor.isEnabled = true
//                spReference.isEnabled = true
//
//                tvStatus.text = "Gagal mengirim"
//
//                Toast.makeText(
//                    requireContext(),
//                    "Kalibrasi gagal dimulai",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//    }
//
//    private fun listenCalibrationRawData(sensor: String, reference: String) {
//        val rawDataRef = database.child("rawData")
//
//        rawDataListener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val count = snapshot.childrenCount.toInt()
//
//                val progress = ((count.toFloat() / 31f) * 100).toInt()
//                progressCalibration.progress = progress
//                tvProgress.text = "$progress %"
//                tvStatus.text = "Menerima data... ($count/31)"
//
//                if (count >= 31) {
//                    rawDataRef.removeEventListener(this)
//                    rawDataListener = null
//                    processCalibrationData(snapshot, sensor, reference)
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                tvStatus.text = "Gagal membaca data"
//                resetCalibrationUIEnabled()
//            }
//        }
//        rawDataRef.addValueEventListener(rawDataListener!!)
//    }
//
//    private fun processCalibrationData(
//        snapshot: DataSnapshot,
//        sensor: String,
//        reference: String
//    ) {
//
//        val values = mutableListOf<Double>()
//        for (i in 0 until 31) {
//            val v = snapshot.child("data$i").getValue(Double::class.java)
//            if (v != null) values.add(v)
//        }
//
//        if (values.size < 31) {
//            tvStatus.text = "Data tidak lengkap (${values.size}/31)"
//            resetCalibrationUIEnabled()
//            return
//        }
//
//        // Buang 10 terendah & 10 tertinggi, ambil 11 tengah, rata-ratakan
//        val sorted = values.sorted()
//        val middle = sorted.subList(10, 21) // index 10..20 = 11 nilai tengah
//        val average = middle.average()
//
//        tvStatus.text = "Mengambil faktor pengali..."
//
//        // ASUMSI PATH — sesuaikan kalau strukturmu beda
//        val faktorRef = FirebaseDatabase.getInstance()
//            .getReference("hics/$deviceID/setting/$sensor")
//
//        faktorRef.get()
//            .addOnSuccessListener { faktorSnapshot ->
//                val faktorLama = faktorSnapshot.getValue(Double::class.java)
//
//                if (faktorLama == null || faktorLama == 0.0) {
//                    tvStatus.text = "Faktor pengali tidak valid"
//                    resetCalibrationUIEnabled()
//                    return@addOnSuccessListener
//                }
//
//                val referenceValue = reference.toDoubleOrNull()
//                if (referenceValue == null) {
//                    tvStatus.text = "Referensi tidak valid"
//                    resetCalibrationUIEnabled()
//                    return@addOnSuccessListener
//                }
//
//                // X = nilai asli sensor
//                val x = average / faktorLama
//
//                if (x == 0.0) {
//                    tvStatus.text = "Nilai X = 0, kalibrasi gagal"
//                    resetCalibrationUIEnabled()
//                    return@addOnSuccessListener
//                }
//
//                // X * Y = reference  ->  Y = reference / X
//                val y = referenceValue / x
//
//                val resultData = HashMap<String, Any>()
//                resultData["average"] = average
//                resultData["status"] = "done"
//                resultData["progress"] = 100
//
//                database.updateChildren(resultData)
//
//                faktorRef.setValue(y)
//                    .addOnSuccessListener {
//                        progressCalibration.progress = 100
//                        tvProgress.text = "100 %"
//                        tvStatus.text = "Kalibrasi selesai"
//                        resetCalibrationUIEnabled()
//
//                        Toast.makeText(
//                            requireContext(),
//                            "Kalibrasi berhasil, faktor baru: $y",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                    .addOnFailureListener {
//                        tvStatus.text = "Gagal menyimpan faktor baru"
//                        resetCalibrationUIEnabled()
//                    }
//            }
//            .addOnFailureListener {
//                tvStatus.text = "Gagal mengambil faktor pengali"
//                resetCalibrationUIEnabled()
//            }
//    }
//
//    private fun resetCalibrationUIEnabled() {
//        btnCalibration.isEnabled = true
//        btnCancel.isEnabled = false
//        spSensor.isEnabled = true
//        spReference.isEnabled = true
//    }
//
//    private fun cancelCalibration() {
//
//        val data = HashMap<String, Any>()
//
//        data["command"] = "cancel"
//        data["status"] = "cancel"
//
//        database.updateChildren(data)
//            .addOnSuccessListener {
//
//                btnCalibration.isEnabled = true
//                btnCancel.isEnabled = false
//
//                progressCalibration.progress = 0
//
//                tvProgress.text = "0 %"
//
//                tvStatus.text = "Kalibrasi dibatalkan"
//
//            }
//    }
//
//    private fun listenCalibration() {
//
//        calibrationListener = object : ValueEventListener {
//
//            override fun onDataChange(snapshot: DataSnapshot) {
//
//                if (!snapshot.exists()) return
//
//                val progress =
//                    snapshot.child("progress")
//                        .getValue(Int::class.java) ?: 0
//
//                val status =
//                    snapshot.child("status")
//                        .getValue(String::class.java) ?: "idle"
//
//                val sensor =
//                    snapshot.child("sensor")
//                        .getValue(String::class.java) ?: "pH"
//
//                val reference =
//                    snapshot.child("reference")
//                        .getValue(String::class.java) ?: ""
//
//                val average = snapshot.child("average").value
//                    ?.toString()
//                    ?.toDoubleOrNull() ?: 0.0
//
//                val offset = snapshot.child("offset").value
//                    ?.toString()
//                    ?.toDoubleOrNull() ?: 0.0
//
//                val sensorIndex = if (sensor == "pH") 0 else 1
//
//                ignoreSensorChange = true
//
//                if (spSensor.selectedItemPosition != sensorIndex) {
//                    spSensor.setSelection(sensorIndex, false)
//                }
//
//                loadReference(sensor)
//
//                val adapter = spReference.adapter as ArrayAdapter<String>
//
//                val index = adapter.getPosition(reference)
//
//                if (index != spReference.selectedItemPosition && index >= 0) {
//                    spReference.setSelection(index, false)
//                }
//
//                ignoreSensorChange = false
//
//                tvReference.text = "Referensi : $reference"
//
////                progressCalibration.progress = progress.coerceIn(0, 100)
////
////                tvProgress.text = "$progress %"
//
//                val running = status in listOf(
//                    "waiting",
//                    "reading",
//                    "averaging",
//                    "calculating",
//                    "saving"
//                )
//
//                btnCalibration.isEnabled = !running
//                btnCancel.isEnabled = running
//
//                spSensor.isEnabled = !running
//                spReference.isEnabled = !running
//
//                when (status) {
//
//                    "idle" -> {
//
//                        tvStatus.text = "Siap dikalibrasi"
//
//                        btnCalibration.isEnabled = true
//                        btnCancel.isEnabled = false
//
//                        spSensor.isEnabled = true
//                        spReference.isEnabled = true
//
//                        progressCalibration.progress = 0
//                        tvProgress.text = "0 %"
//                    }
//
//                    "waiting" -> {
//
////                        tvStatus.text = "Menunggu ESP32..."
//
//                    }
//
//                    "reading" -> {
//
//                        tvStatus.text = "Membaca Sensor..."
//
//                    }
//
//                    "averaging" -> {
//
//                        tvStatus.text = "Menghitung Rata-rata..."
//
//                    }
//
//                    "calculating" -> {
//
//                        tvStatus.text = "Menghitung Offset..."
//
//                    }
//
//                    "saving" -> {
//
//                        tvStatus.text = "Menyimpan Kalibrasi..."
//
//                    }
//
//                    "done" -> {
//
//                        tvStatus.text = "✅ Kalibrasi Berhasil"
//
//                        btnCalibration.isEnabled = true
//                        btnCancel.isEnabled = false
//
//                        spSensor.isEnabled = true
//                        spReference.isEnabled = true
//
//                        progressCalibration.progress = 100
//                        tvProgress.text = "100 %"
//                    }
//
//                    "cancel" -> {
//
//                        tvStatus.text = "Kalibrasi Dibatalkan"
//
//                        btnCalibration.isEnabled = true
//
//                        btnCancel.isEnabled = false
//                        spSensor.isEnabled = true
//                        spReference.isEnabled = true
//
//                        progressCalibration.progress = 0
//
//                        tvProgress.text = "0 %"
//
//                    }
//
//                    "error" -> {
//
//                        tvStatus.text = "Kalibrasi Gagal"
//
//                        btnCalibration.isEnabled = true
//
//                        btnCancel.isEnabled = false
//                        spSensor.isEnabled = true
//                        spReference.isEnabled = true
//
//                    }
//
//                }
//
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//
//            }
//        }
//        database.addValueEventListener(calibrationListener!!)
//
//
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//
//        calibrationListener?.let {
//            database.removeEventListener(it)
//        }
//    }
//}