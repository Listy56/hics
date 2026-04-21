package com.example.hics

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private  lateinit var homeBt: LinearLayout
    private lateinit var chartBt: LinearLayout
    private lateinit var settingBt: LinearLayout
    private lateinit var imgHome: ImageView
    private lateinit var tvHome: TextView
    private lateinit var imgChart: ImageView
    private lateinit var tvChart: TextView
    private lateinit var imgSetting: ImageView
    private lateinit var tvSetting: TextView
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = getColor(R.color.hijau_start)
        window.navigationBarColor = getColor(R.color.white)

        homeBt = findViewById(R.id.homeBt)
        chartBt = findViewById(R.id.chartBt)
        settingBt = findViewById(R.id.settingBt)
        imgHome = findViewById(R.id.imgHome)
        tvHome = findViewById(R.id.tvHome)
        imgChart = findViewById(R.id.imgChart)
        tvChart = findViewById(R.id.tvChart)
        imgSetting = findViewById(R.id.imgSetting)
        tvSetting = findViewById(R.id.tvSetting)


        if (savedInstanceState == null) {
            currentFragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.mainFragment, currentFragment!!)
                .commit()
        }

        homeBt.setOnClickListener {
            replaceFragment(HomeFragment(), 0)
        }

        chartBt.setOnClickListener {
            replaceFragment(ChartFragment(), 1)
        }

        settingBt.setOnClickListener {
            replaceFragment(SettingFragment(), 2)
        }
    }

    private fun replaceFragment(fragment: Fragment, mode: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        if (mode == 0) {
            imgHome.setImageResource(R.drawable.home_green)
            tvHome.setTextColor(resources.getColor(R.color.hijau))
            imgChart.setImageResource(R.drawable.chart_abu)
            tvChart.setTextColor(resources.getColor(R.color.abu))
            imgSetting.setImageResource(R.drawable.setting_grey)
            tvSetting.setTextColor(resources.getColor(R.color.abu))

        } else if(mode == 1) {
            imgHome.setImageResource(R.drawable.home_grey)
            tvHome.setTextColor(resources.getColor(R.color.abu))
            imgChart.setImageResource(R.drawable.chart_hijau)
            tvChart.setTextColor(resources.getColor(R.color.hijau))
            imgSetting.setImageResource(R.drawable.setting_grey)
            tvSetting.setTextColor(resources.getColor(R.color.abu))
        } else if(mode == 2) {
            imgHome.setImageResource(R.drawable.home_grey)
            tvHome.setTextColor(resources.getColor(R.color.abu))
            imgChart.setImageResource(R.drawable.chart_abu)
            tvChart.setTextColor(resources.getColor(R.color.abu))
            imgSetting.setImageResource(R.drawable.setting_green)
            tvSetting.setTextColor(resources.getColor(R.color.hijau))
        }

        transaction.replace(R.id.mainFragment, fragment)
        transaction.commit()

        currentFragment = fragment
    }
}