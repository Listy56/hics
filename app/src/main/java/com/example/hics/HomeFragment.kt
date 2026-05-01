package com.example.hics

import android.animation.ValueAnimator
import android.content.Context.MODE_PRIVATE
import androidx.fragment.app.Fragment
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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var suhuAir   = 0.0
    var pH        = 0.0
    var nutrisi   = 0
    var intensitasCahaya = 0

    var level   = 0.0

    var waterAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

        phTextView        = view.findViewById(R.id.tvPh)
        nutrisiTextView   = view.findViewById(R.id.tvNutrisi)
        statusSwitch      = view.findViewById(R.id.statusSwitch)
        intensitas        = view.findViewById(R.id.tvIntensitas)
        airTemp           = view.findViewById(R.id.airTemp)
        waterTemp         = view.findViewById(R.id.waterTemp)
        waterLevel        = view.findViewById(R.id.waterLevel)
        baseWaterLevel    = view.findViewById(R.id.baseWaterLevel)
        waterLevelPercent = view.findViewById(R.id.waterLevelPercent)

        val accPref      = requireActivity().getSharedPreferences("ACCOUNT", MODE_PRIVATE)
        deviceID         = accPref.getString("deviceID", "")

        Log.d("MonitoringFragment", "DeviceID: $deviceID")

        var baseFirebase = firebaseDatabase.getReference("Hics")

        if (deviceID != null && deviceID.toString().isNotEmpty()) {
            baseFirebase.child(deviceID.toString()).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        suhuAir           = snapshot.child("dataStream").child("waterTemp").value.toString().toDouble()
                        suhuUdara         = snapshot.child("dataStream").child("airTemp").value.toString().toDouble()
                        pH                = snapshot.child("dataStream").child("pH").value.toString().toDouble()
                        nutrisi           = snapshot.child("dataStream").child("ppm").value.toString().toInt()
                        level             = snapshot.child("dataStream").child("waterLevel").value.toString().toDouble()
                        intensitasCahaya  = snapshot.child("dataStream").child("light").value.toString().toInt()
                        isOn              = snapshot.child("control").child("waterPump").value.toString().toBoolean()

                        if(level < 15) level = 15.0
                        else if (level > 100) level = 100.0

                        phTextView.text         = pH.toString()
                        nutrisiTextView.text    = nutrisi.toString()
                        airTemp.text            = "$suhuAir\u00B0C"
                        waterTemp.text          = "$suhuUdara\u00B0C"
                        intensitas.text         = intensitasCahaya.toString()

                        if(isOn) statusSwitch.text = "ON"
                        else statusSwitch.text     = "OFF"

                        baseWaterLevel.post {
                            val maxHeight           = baseWaterLevel.height
                            val newHeight           = (level * maxHeight) / 100.0
                            waterLevelPercent.text  = "$level%"
                            animateWaterLevel(newHeight.toInt())
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        }
    }

    fun animateWaterLevel(targetHeight: Int) {
        waterAnimator?.cancel()
        val startHeight = waterLevel.height

        waterAnimator = ValueAnimator.ofInt(startHeight, targetHeight).apply {
            duration     = 300
            interpolator = DecelerateInterpolator()

            addUpdateListener {
                val params    = waterLevel.layoutParams
                params.height = it.animatedValue as Int
                waterLevel.layoutParams = params
            }
            start()
        }
    }
}