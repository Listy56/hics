package com.example.hics

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max

class HomeFragment : Fragment() {

    private lateinit var phTextView: TextView
    private lateinit var nutrisiTextView: TextView
    private lateinit var switchPompa: LinearLayout
    private lateinit var circle: View
    private lateinit var statusSwitch: TextView
    private lateinit var intensitas: TextView
    private lateinit var airTemp: TextView
    private lateinit var waterTemp: TextView
    private lateinit var waterLevelPercent: TextView
    private lateinit var waterLevel: LinearLayout
    private lateinit var baseWaterLevel: LinearLayout
    private var isOn = true

    var suhuUdara = 0
    var suhuAir   = 0
    var pH        = 0
    var nutrisi   = 0
    var intensitasCahaya = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        phTextView        = view.findViewById(R.id.tvPh)
        nutrisiTextView   = view.findViewById(R.id.tvNutrisi)
        switchPompa       = view.findViewById(R.id.switchPompa)
        circle            = view.findViewById(R.id.circle)
        statusSwitch      = view.findViewById(R.id.statusSwitch)
        intensitas        = view.findViewById(R.id.tvIntensitas)
        airTemp           = view.findViewById(R.id.airTemp)
        waterTemp         = view.findViewById(R.id.waterTemp)
        waterLevel        = view.findViewById(R.id.waterLevel)
        baseWaterLevel    = view.findViewById(R.id.baseWaterLevel)
        waterLevelPercent = view.findViewById(R.id.waterLevelPercent)

        suhuAir = 30
        suhuUdara = 35
        pH = 6
        nutrisi = 900
        intensitasCahaya = 10000

        phTextView.text         = pH.toString()
        nutrisiTextView.text    = nutrisi.toString()
        airTemp.text            = "$suhuAir\u00B0C"
        waterTemp.text          = "$suhuUdara\u00B0C"
        intensitas.text         = intensitasCahaya.toString()

        var level = 80

        baseWaterLevel.post {
            val maxHeight = baseWaterLevel.height
            val newHeight = (level * maxHeight) / 100.0
            val params = waterLevel.layoutParams
            params.height = newHeight.toInt()
            waterLevel.layoutParams = params
            waterLevelPercent.text = "$level%"
        }


        if (isOn) {
            switchPompa.setBackgroundResource(R.drawable.bg_switch_on)
            circle.animate()
                .translationX(60f)
                .setDuration(200)
                .start()
            statusSwitch.text = "ON"
        } else {
            switchPompa.setBackgroundResource(R.drawable.bg_switch_off)
            circle.animate()
                .translationX(0f)
                .setDuration(200)
                .start()
            statusSwitch.text = "OFF"
        }

        switchPompa.setOnClickListener {
            isOn = !isOn

            if (isOn) {
                switchPompa.setBackgroundResource(R.drawable.bg_switch_on)
                circle.animate()
                    .translationX(60f)
                    .setDuration(200)
                    .start()
                statusSwitch.text = "ON"
            } else {
                switchPompa.setBackgroundResource(R.drawable.bg_switch_off)
                circle.animate()
                    .translationX(0f)
                    .setDuration(200)
                    .start()
                statusSwitch.text = "OFF"
            }
        }
    }
}