package com.vinh.dyvat.ui.screens.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vinh.dyvat.ui.theme.TextSilver

@Composable
fun StatisticsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Màn hình Thống kê\n(Phase tiếp theo)",
            color = TextSilver,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
