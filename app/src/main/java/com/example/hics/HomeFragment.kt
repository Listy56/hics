package com.example.hics

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var suhuUdara = 0.0
    var suhuAir   = 0.0
    var pH        = 0.0
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

        suhuAir = 30.0
        suhuUdara = 35.0
        pH = 6.5
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
            val params    = waterLevel.layoutParams
            params.height = newHeight.toInt()
            waterLevel.layoutParams = params
            waterLevelPercent.text  = "$level%"
        }

        //ini untuk dummy data
        lifecycleScope.launch {
            while (true) {

                suhuAir     += (-1..1).random()
                suhuUdara   += (-1..1).random()
                pH          += listOf(-0.1, 0.0, 0.1).random()
                nutrisi     += (-20..20).random()
                intensitasCahaya += (-500..500).random()
                level       += (-2..2).random()

                suhuAir     = suhuAir.coerceIn(20.0, 40.0)
                suhuUdara   = suhuUdara.coerceIn(20.0, 45.0)
                pH          = pH.coerceIn(5.5, 7.5)
                nutrisi     = nutrisi.coerceIn(500, 1500)
                intensitasCahaya = intensitasCahaya.coerceIn(0, 20000)
                level       = level.coerceIn(0, 100)

                phTextView.text = String.format("%.1f", pH)
                nutrisiTextView.text = nutrisi.toString()
                airTemp.text    = "$suhuAir\u00B0C"
                waterTemp.text  = "$suhuUdara\u00B0C"
                intensitas.text = intensitasCahaya.toString()

                waterLevelPercent.text = "$level%"

                baseWaterLevel.post {
                    val maxHeight = baseWaterLevel.height
                    val newHeight = (level * maxHeight) / 100.0

                    val params = waterLevel.layoutParams
                    params.height = newHeight.toInt()
                    waterLevel.layoutParams = params
                }

                delay(2000)
            }
        }


        switchPompa.post {
            updateSwitchUI(isOn)
        }

        switchPompa.setOnClickListener {
            isOn = !isOn
            updateSwitchUI(isOn)
        }
    }

    fun updateSwitchUI(isOn: Boolean) {
        if (isOn) {
            switchPompa.setBackgroundResource(R.drawable.bg_switch_on)
            circle.animate().translationX(60f).setDuration(200).start()
            statusSwitch.text = "ON"
        } else {
            switchPompa.setBackgroundResource(R.drawable.bg_switch_off)
            circle.animate().translationX(0f).setDuration(200).start()
            statusSwitch.text = "OFF"
        }
    }
}