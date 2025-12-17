package com.example.kotlingcspractice.ui.utils

import kotlin.math.pow
import kotlin.math.round

fun formatValue(v: Float, places: Int = 2): String {
    val factor = 10f.pow(places)
    return (round(v * factor) / factor).toString()
}

fun formatNullableFloat(value: Float?, unit: String = "", places: Int = 2): String {
    return value?.let { "${formatValue(it, places)}${if (unit.isNotEmpty()) " $unit" else ""}" } ?: "—"
}

fun formatNullableInt(value: Int?): String {
    return value?.toString() ?: "—"
}

fun formatNullableDouble(value: Double?, places: Int = 6): String {
    return value?.let { formatValue(it.toFloat(), places) } ?: "—"
}

fun formatBooleanYesNo(value: Boolean): String {
    return if (value) "Yes" else "No"
}

