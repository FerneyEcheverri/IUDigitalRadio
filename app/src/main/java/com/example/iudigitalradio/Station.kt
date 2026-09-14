package com.example.iudigitalradio

// Representa la información de cada emisora dentro de la app
data class Station(
    val id: Int,
    val name: String,
    val frequency: String,
    val streamUrl: String
)