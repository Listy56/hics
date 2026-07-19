package com.example.hics

data class HistoryModel(

    var timestamp: String = "",

    var airTemp: String = "",
    var humidity: String = "",
    var ph: String = "",
    var ppm: String = "",
    var waterLevel: String = "",
    var waterTemp: String = "",

    // BMKG
    var weather: String = "",
    var forecastTemp: String = "",
    var forecastHumidity: String = "",
    var wind: String = ""

)