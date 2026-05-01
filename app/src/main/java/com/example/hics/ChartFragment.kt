package com.example.hics

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.example.hics.databinding.FragmentChartBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class ChartFragment : Fragment() {

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

    private val dataTypeList = listOf("Nutrisi", "Suhu Air", "Suhu Udara", "pH", "Intensitas Cahaya")
    private val periodList   = listOf("Weekly", "Daily", "Monthly")

    private val weeklyLabels  = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val dailyLabels   = listOf("00", "04", "08", "12", "16", "20", "24")
    private val monthlyLabels = listOf("W1", "W2", "W3", "W4")

    private val chartData: Map<String, Map<String, List<Float>>> = mapOf(
        "Nutrisi"           to mapOf("Weekly" to listOf(680f,750f,710f,760f,900f,790f,770f), "Daily" to listOf(650f,700f,740f,800f,870f,820f,780f), "Monthly" to listOf(710f,760f,800f,850f)),
        "Suhu Air"          to mapOf("Weekly" to listOf(26f,27f,27.5f,28f,29f,28f,27f),      "Daily" to listOf(25f,25.5f,27f,28.5f,29f,27.5f,26f),  "Monthly" to listOf(26.5f,27f,28f,27.5f)),
        "Suhu Udara"        to mapOf("Weekly" to listOf(28f,30f,31f,32f,34f,31f,29f),        "Daily" to listOf(24f,25f,29f,33f,34f,31f,27f),         "Monthly" to listOf(29f,31f,32f,30f)),
        "pH"                to mapOf("Weekly" to listOf(6.5f,6.8f,7.0f,7.1f,6.9f,6.7f,6.8f),"Daily" to listOf(6.4f,6.6f,6.9f,7.1f,7.0f,6.8f,6.6f), "Monthly" to listOf(6.7f,6.9f,7.0f,6.8f)),
        "Intensitas Cahaya" to mapOf("Weekly" to listOf(200f,400f,600f,800f,1000f,750f,500f),"Daily" to listOf(0f,50f,400f,900f,1000f,600f,100f),     "Monthly" to listOf(500f,700f,850f,750f))
    )

    private var selectedDataType = "Nutrisi"
    private var selectedPeriod   = "Weekly"
    private val colorTeal        = Color.parseColor("#2BBFA4")

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

        setupSpinners()
        setupChart()
        updateChart()
    }

    private fun makeAdapter(items: List<String>, selectedColor: Int): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(requireContext(), R.layout.item_spinner_dropdown, items) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as TextView).apply {
                    text = items[position]
                    setTextColor(selectedColor)
                    setBackgroundColor(Color.TRANSPARENT)
                    textSize = 13f
                    setPadding(0, 0, 0, 0)
                }
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as TextView).apply {
                    text = items[position]
                    setTextColor(Color.parseColor("#2D3A4A"))
                    textSize = 13f
                    setBackgroundResource(R.drawable.bg_dropdown_item)
                }
                return v
            }
        }.also { it.setDropDownViewResource(R.layout.item_spinner_dropdown) }
    }

    private fun setupSpinners() {
        binding.spinnerDataType.background = null
        binding.spinnerDataType.adapter = makeAdapter(dataTypeList, Color.WHITE)
        binding.spinnerDataType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                selectedDataType = dataTypeList[pos]; updateChart()
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        binding.layoutSpinnerData.setOnClickListener {
            binding.spinnerDataType.performClick()
        }

        binding.spinnerPeriod.adapter = makeAdapter(periodList, Color.parseColor("#2D3A4A"))
        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                selectedPeriod = periodList[pos]; updateChart()
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        binding.layoutSpinnerPeriod.setOnClickListener {
            binding.spinnerPeriod.performClick()
        }
    }

    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            legend.isEnabled      = false
            setTouchEnabled(true)
            isDragEnabled         = true
            isScaleXEnabled       = false
            isScaleYEnabled       = false
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            extraBottomOffset = 8f
            extraTopOffset    = 16f

            xAxis.apply {
                position             = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor            = Color.parseColor("#8A9BB0")
                textSize             = 11f
                granularity          = 1f
                isGranularityEnabled = true
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor     = Color.parseColor("#E8EEF4")
                gridLineWidth = 0.8f
                enableGridDashedLine(6f, 4f, 0f)
                setDrawAxisLine(false)
                textColor     = Color.parseColor("#8A9BB0")
                textSize      = 11f
                setLabelCount(6, true)
            }

            axisRight.isEnabled = false

            val mv = CustomMarkerView(requireContext(), R.layout.marker_view_chart)
            mv.chartView = this
            marker = mv
        }
    }

    private fun updateChart() {
        val values = chartData[selectedDataType]?.get(selectedPeriod) ?: return
        val labels = when (selectedPeriod) {
            "Daily"   -> dailyLabels
            "Monthly" -> monthlyLabels
            else      -> weeklyLabels
        }

        val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v) }

        val gradientFill = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#4D2BBFA4"), Color.parseColor("#002BBFA4"))
        )

        val dataSet = LineDataSet(entries, selectedDataType).apply {
            color              = colorTeal
            lineWidth          = 2.5f
            setCircleColor(colorTeal)
            circleRadius       = 4.5f
            circleHoleRadius   = 2.5f
            circleHoleColor    = Color.WHITE
            setDrawValues(false)
            mode               = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity     = 0.2f
            fillDrawable       = gradientFill
            setDrawFilled(true)
            highLightColor     = Color.parseColor("#F5A623")
            highlightLineWidth = 1.5f
            enableDashedHighlightLine(0f, 0f, 0f)
            isHighlightEnabled = true
        }

        binding.lineChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            labelCount     = labels.size
        }

        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.animateX(800, Easing.EaseInOutQuart)
        binding.lineChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CustomMarkerView(
    context: android.content.Context,
    layoutResource: Int
) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvMarkerContent)

    override fun refreshContent(e: Entry, highlight: Highlight) {
        tvContent.text = if (e.y % 1 == 0f) e.y.toInt().toString() else "%.1f".format(e.y)
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -(height + 16f))
}