package com.example.hics

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class ChartFragment : Fragment() {

    private lateinit var chart: LineChart
    private lateinit var spinnerData: Spinner
    private lateinit var spinnerPeriode: Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_chart, container, false)

        chart = view.findViewById(R.id.lineChart)
        spinnerData = view.findViewById(R.id.spinnerData)
        spinnerPeriode = view.findViewById(R.id.spinnerPeriode)

        setupSpinner()

        return view
    }

    private fun setupSpinner() {

        val dataList = listOf("Nutrisi", "PH", "Suhu")
        val periodeList = listOf("Weekly", "Monthly")

        spinnerData.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, dataList)
        spinnerPeriode.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, periodeList)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateChart(
                    spinnerData.selectedItem.toString(),
                    spinnerPeriode.selectedItem.toString()
                )
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerData.onItemSelectedListener = listener
        spinnerPeriode.onItemSelectedListener = listener
    }

    // 🔥 DATA DUMMY DINAMIS
    private fun generateDummyData(type: String, periode: String): ArrayList<Entry> {

        val list = ArrayList<Entry>()
        val total = if (periode == "Weekly") 7 else 12

        for (i in 0 until total) {

            val value = when (type) {
                "Nutrisi" -> (650..950).random()
                "PH" -> (60..80).random() / 10f
                "Suhu" -> (24..32).random()
                else -> 0
            }

            list.add(Entry(i.toFloat(), value.toFloat()))
        }

        return list
    }

    private fun updateChart(type: String, periode: String) {
        val entries = generateDummyData(type, periode)
        setupChart(entries, periode)
    }

    private fun setupChart(entries: ArrayList<Entry>, periode: String) {

        val dataSet = LineDataSet(entries, "")

        // 🔥 STYLE GARIS
        dataSet.color = "#00ACC1".toColorInt()
        dataSet.lineWidth = 3f

        // 🔥 TITIK
        dataSet.setDrawCircles(true)
        dataSet.setCircleColor("#FF9800".toColorInt())
        dataSet.circleRadius = 6f

        // 🔥 SMOOTH
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        dataSet.setDrawValues(false)

        // 🔥 AREA BAWAH
        dataSet.setDrawFilled(true)
        dataSet.fillColor = "#B2EBF2".toColorInt()

        chart.data = LineData(dataSet)

        // ======================
        // 🔥 STYLE CHART
        // ======================

        chart.setViewPortOffsets(60f, 40f, 40f, 60f)

        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false

        chart.axisLeft.apply {
            textColor = "#90A4AE".toColorInt()
            gridColor = "#ECEFF1".toColorInt()
            setDrawAxisLine(false)
        }

        val labels = if (periode == "Weekly") {
            listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
        } else {
            listOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
        }

        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            granularity = 1f
            position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            textColor = "#90A4AE".toColorInt()
            setDrawGridLines(false)
            setDrawAxisLine(false)
        }

        chart.animateX(800)
        chart.invalidate()
    }
}