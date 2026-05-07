package com.vinh.dyvat.ui.screens.statistics

import com.vinh.dyvat.data.model.DailySummary

// ---------------------------------------------------------------------------
// Data helpers
// ---------------------------------------------------------------------------

internal data class BarGroup(val label: String, val bar1: Long, val bar2: Long)

private fun daysInMonth(year: Int, month: Int): Int = when {
    month in listOf(1, 3, 5, 7, 8, 10, 12) -> 31
    month in listOf(4, 6, 9, 11) -> 30
    month == 2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 30
}

internal fun fillMonthDays(data: List<DailySummary>, year: Int, month: Int): List<DailySummary> {
    val monthStr = month.toString().padStart(2, '0')
    val dataMap = data.associateBy { it.businessDate }
    return (1..daysInMonth(year, month)).map { day ->
        val dayStr = day.toString().padStart(2, '0')
        val key = "$year-$monthStr-$dayStr"
        dataMap[key] ?: DailySummary(businessDate = key)
    }
}

internal fun buildPurchaseSaleBars(
    data: List<DailySummary>,
    mode: StatsPeriodMode,
    year: Int,
    month: Int
): List<BarGroup> = if (mode == StatsPeriodMode.MONTH) {
    fillMonthDays(data, year, month).map { d ->
        val day = d.businessDate.substring(8).trimStart('0').ifEmpty { "0" }
        BarGroup(label = day, bar1 = d.totalPurchaseVnd, bar2 = d.totalSaleVnd)
    }
} else {
    (1..12).map { m ->
        val mStr = m.toString().padStart(2, '0')
        val days = data.filter { it.businessDate.length >= 7 && it.businessDate.substring(5, 7) == mStr }
        BarGroup(label = "T$m", bar1 = days.sumOf { it.totalPurchaseVnd }, bar2 = days.sumOf { it.totalSaleVnd })
    }
}

internal fun buildRevenueProfitBars(
    data: List<DailySummary>,
    mode: StatsPeriodMode,
    year: Int,
    month: Int
): List<BarGroup> = if (mode == StatsPeriodMode.MONTH) {
    fillMonthDays(data, year, month).map { d ->
        val day = d.businessDate.substring(8).trimStart('0').ifEmpty { "0" }
        BarGroup(label = day, bar1 = d.totalSaleVnd, bar2 = d.profitVnd)
    }
} else {
    (1..12).map { m ->
        val mStr = m.toString().padStart(2, '0')
        val days = data.filter { it.businessDate.length >= 7 && it.businessDate.substring(5, 7) == mStr }
        BarGroup(label = "T$m", bar1 = days.sumOf { it.totalSaleVnd }, bar2 = days.sumOf { it.profitVnd })
    }
}
