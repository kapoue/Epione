package com.epione.app.util

import kotlin.math.*

/** Calcule la distance à vol d'oiseau entre deux coordonnées GPS (formule Haversine). */
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * r * asin(sqrt(a))
}

/** Formate une distance en km pour affichage. */
fun formatDistanceKm(km: Double): String = when {
    km < 1.0  -> "< 1 km"
    km < 10.0 -> "%.1f km".format(km)
    else      -> "%d km".format(km.toInt())
}
