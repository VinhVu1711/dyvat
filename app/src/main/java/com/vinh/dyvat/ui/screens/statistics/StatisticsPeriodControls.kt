package com.vinh.dyvat.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

// ---------------------------------------------------------------------------
// Period controls
// ---------------------------------------------------------------------------

@Composable
internal fun PeriodModeTabs(mode: StatsPeriodMode, onModeChange: (StatsPeriodMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PeriodTab(
            label = "Theo tháng",
            selected = mode == StatsPeriodMode.MONTH,
            onClick = { onModeChange(StatsPeriodMode.MONTH) },
            modifier = Modifier.weight(1f)
        )
        PeriodTab(
            label = "Theo năm",
            selected = mode == StatsPeriodMode.YEAR,
            onClick = { onModeChange(StatsPeriodMode.YEAR) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PeriodTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SpotifyGreen.copy(alpha = 0.15f) else DarkCard)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) SpotifyGreen else TextSilver,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
internal fun PeriodNavigator(state: StatisticsUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    val label = if (state.mode == StatsPeriodMode.MONTH) {
        "Tháng ${state.selectedMonth}/${state.selectedYear}"
    } else {
        "Năm ${state.selectedYear}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Kỳ trước", tint = TextSilver, modifier = Modifier.size(18.dp))
        }
        Text(text = label, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ArrowForwardIos, contentDescription = "Kỳ sau", tint = TextSilver, modifier = Modifier.size(18.dp))
        }
    }
}
