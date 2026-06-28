package com.example.hics

data class HistoryModel(
    var timestamp: String = "",
    var airTemp: String = "",
    var humidity: String = "",
    var ph: String = "",
    var ppm: String = "",
    var waterLevel: String = "",
    var waterTemp: String = ""
)