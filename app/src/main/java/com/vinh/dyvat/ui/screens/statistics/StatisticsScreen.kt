package com.vinh.dyvat.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.ui.components.EmptyState
import com.vinh.dyvat.ui.components.ErrorState
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.theme.AnnouncementBlue
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.NegativeRed
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextWhite
import com.vinh.dyvat.ui.theme.WarningOrange

// ---------------------------------------------------------------------------
// Main Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text("THỐNG KÊ", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NearBlack)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NearBlack)
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { PeriodModeTabs(mode = state.mode, onModeChange = { viewModel.setMode(it) }) }
            item { PeriodNavigator(state = state, onPrevious = { viewModel.previousPeriod() }, onNext = { viewModel.nextPeriod() }) }
            item { StatisticsExportControls(state = state, viewModel = viewModel) }

            when {
                state.isLoading -> item { LoadingIndicator() }
                state.error != null -> item {
                    ErrorState(message = state.error ?: "", onRetry = { viewModel.retry() })
                }
                state.dailyData.isEmpty() -> item {
                    EmptyState(
                        icon = Icons.Default.BarChart,
                        title = "Chưa có dữ liệu",
                        subtitle = "Chưa có phiếu nhập hoặc bán trong kỳ này"
                    )
                }
                else -> {
                    // Summary cards
                    item {
                        SectionLabel(text = "TỔNG QUAN")
                        SummarySection(state = state)
                    }

                    // Chart 1: Purchase vs Sale
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionLabel(text = "NHẬP HÀNG & BÁN HÀNG")
                        BarChartSection(
                            bars = buildPurchaseSaleBars(
                                state.dailyData, state.mode, state.selectedYear, state.selectedMonth
                            ),
                            bar1Color = WarningOrange,
                            bar2Color = SpotifyGreen,
                            bar1Label = "Nhập",
                            bar2Label = "Bán"
                        )
                    }

                    // Chart 2: Revenue vs Profit
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionLabel(text = "DOANH THU & LỢI NHUẬN")
                        BarChartSection(
                            bars = buildRevenueProfitBars(
                                state.dailyData, state.mode, state.selectedYear, state.selectedMonth
                            ),
                            bar1Color = SpotifyGreen,
                            bar2Color = AnnouncementBlue,
                            bar1Label = "Doanh thu",
                            bar2Label = "Lợi nhuận",
                            negativeBar2Color = NegativeRed
                        )
                    }

                    // Detail list
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionLabel(
                            text = if (state.mode == StatsPeriodMode.MONTH) "CHI TIẾT THEO NGÀY"
                            else "CHI TIẾT THEO THÁNG"
                        )
                    }

                    if (state.mode == StatsPeriodMode.MONTH) {
                        items(
                            items = state.dailyData.sortedByDescending { it.businessDate },
                            key = { it.businessDate }
                        ) { day ->
                            DailyRow(summary = day)
                        }
                    } else {
                        val grouped = state.dailyData
                            .groupBy { it.businessDate.substring(0, 7) }
                            .toSortedMap(reverseOrder())
                        grouped.forEach { (month, days) ->
                            item(key = month) { MonthlyGroupRow(monthKey = month, days = days) }
                        }
                    }
                }
            }
        }
    }
}
