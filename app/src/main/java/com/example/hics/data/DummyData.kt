package com.example.hics.data

data class SensorData(
    val ph: Float,
    val nutrisi: Int,
    val suhuUdara: Float,
    val suhuAir: Float,
    val kelembapanUdara: Int,
    val air: Int,
    val pompaOn: Boolean
)

object DummyData {
    fun getData(): SensorData {
        return SensorData(
            ph = 6.2f,
            nutrisi = 900,
            suhuUdara = 28f,
            suhuAir = 26f,
            kelembapanUdara = 75,
            air = 80,
            pompaOn = true
        )
    }
}
