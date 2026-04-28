package com.vinh.dyvat.ui.components

import java.text.DecimalFormat

fun Long.toVnd(): String {
    if (this == 0L) return "0 VND"
    val formatter = DecimalFormat("#,###")
    return "${formatter.format(this)} VND"
}

fun Long.toVndShort(): String {
    if (this == 0L) return "0"
    val vnd = this
    return when {
        vnd >= 1_000_000_000 -> {
            val billions = vnd / 1_000_000_000.0
            String.format("%.1fB", billions)
        }
        vnd >= 1_000_000 -> {
            val millions = vnd / 1_000_000.0
            String.format("%.1fM", millions)
        }
        vnd >= 1_000 -> {
            val thousands = vnd / 1_000.0
            String.format("%.1fK", thousands)
        }
        else -> vnd.toString()
    }
}

fun String.parseVndNumber(): Long {
    val cleaned = this.replace(" VND", "")
        .replace(".", "")
        .replace(",", "")
        .trim()
    return cleaned.toLongOrNull() ?: 0L
}
