package com.vinh.dyvat.ui.screens.statistics

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.DailySummary
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

enum class StatsPeriodMode { MONTH, YEAR }

data class StatisticsUiState(
    val mode: StatsPeriodMode = StatsPeriodMode.MONTH,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val dailyData: List<DailySummary> = emptyList(),
    val totalPurchaseVnd: Long = 0L,
    val totalSaleVnd: Long = 0L,
    val totalCostVnd: Long = 0L,
    val profitVnd: Long = 0L,
    val purchaseTicketCount: Int = 0,
    val saleTicketCount: Int = 0,
    val isExporting: Boolean = false,
    val exportError: String? = null,
    val exportSuccessMessage: String? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: StatisticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()
    private var exportJob: Job? = null

    init {
        loadData()
    }

    fun setMode(mode: StatsPeriodMode) {
        _uiState.update { it.copy(mode = mode) }
        loadData()
    }

    fun setMonth(month: Int) {
        _uiState.update { it.copy(selectedMonth = month) }
        loadData()
    }

    fun setYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
        loadData()
    }

    fun previousPeriod() {
        val s = _uiState.value
        if (s.mode == StatsPeriodMode.MONTH) {
            if (s.selectedMonth == 1) {
                _uiState.update { it.copy(selectedMonth = 12, selectedYear = s.selectedYear - 1) }
            } else {
                _uiState.update { it.copy(selectedMonth = s.selectedMonth - 1) }
            }
        } else {
            _uiState.update { it.copy(selectedYear = s.selectedYear - 1) }
        }
        loadData()
    }

    fun nextPeriod() {
        val s = _uiState.value
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1

        if (s.mode == StatsPeriodMode.MONTH) {
            val isCurrentPeriod = s.selectedYear == currentYear && s.selectedMonth == currentMonth
            if (isCurrentPeriod) return
            if (s.selectedMonth == 12) {
                _uiState.update { it.copy(selectedMonth = 1, selectedYear = s.selectedYear + 1) }
            } else {
                _uiState.update { it.copy(selectedMonth = s.selectedMonth + 1) }
            }
        } else {
            if (s.selectedYear >= currentYear) return
            _uiState.update { it.copy(selectedYear = s.selectedYear + 1) }
        }
        loadData()
    }

    fun retry() = loadData()

    fun getDefaultExportFileName(): String {
        val s = _uiState.value
        return if (s.mode == StatsPeriodMode.MONTH) {
            val month = s.selectedMonth.toString().padStart(2, '0')
            "Báo cáo kinh doanh tháng $month-${s.selectedYear}.xlsx"
        } else {
            "Báo cáo kinh doanh năm ${s.selectedYear}.xlsx"
        }
    }

    fun exportReport(context: Context, uri: Uri) {
        if (_uiState.value.isExporting) return

        exportJob = viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportError = null, exportSuccessMessage = null) }
            val snapshot = _uiState.value
            val period = snapshot.toReportPeriod()
            val writer = StatisticsExcelReportWriter()

            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        writer.writeReport(
                            outputStream = output,
                            period = period,
                            dailyData = snapshot.dailyData,
                            loadPurchasePage = { page ->
                                when (val result = repository.getPurchaseExportRows(period.startDate, period.endDate, page)) {
                                    is Result.Success -> result.data
                                    is Result.Error -> throw IllegalStateException(result.message)
                                    is Result.Loading -> emptyList()
                                }
                            },
                            loadSalePage = { page ->
                                when (val result = repository.getSaleExportRows(period.startDate, period.endDate, page)) {
                                    is Result.Success -> result.data
                                    is Result.Error -> throw IllegalStateException(result.message)
                                    is Result.Loading -> emptyList()
                                }
                            }
                        )
                    } ?: throw IllegalStateException("Không thể mở file để ghi báo cáo")
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportSuccessMessage = "Đã xuất báo cáo Excel",
                        exportError = null
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isExporting = false) }
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportError = e.message ?: "Lỗi khi xuất báo cáo Excel"
                    )
                }
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        exportJob = null
        _uiState.update { it.copy(isExporting = false) }
    }

    fun clearExportMessages() {
        _uiState.update { it.copy(exportError = null, exportSuccessMessage = null) }
    }

    private fun loadData() {
        val s = _uiState.value
        viewModelScope.launch {
            val flow = if (s.mode == StatsPeriodMode.MONTH) {
                repository.getMonthlySummary(s.selectedYear, s.selectedMonth)
            } else {
                repository.getYearlySummary(s.selectedYear)
            }
            flow.collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Result.Success -> {
                        val data = result.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                dailyData = data,
                                totalPurchaseVnd = data.sumOf { d -> d.totalPurchaseVnd },
                                totalSaleVnd = data.sumOf { d -> d.totalSaleVnd },
                                totalCostVnd = data.sumOf { d -> d.totalCostVnd },
                                profitVnd = data.sumOf { d -> d.profitVnd },
                                purchaseTicketCount = data.sumOf { d -> d.purchaseTicketCount },
                                saleTicketCount = data.sumOf { d -> d.saleTicketCount }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun StatisticsUiState.toReportPeriod(): StatisticsReportPeriod {
        return if (mode == StatsPeriodMode.MONTH) {
            val monthStr = selectedMonth.toString().padStart(2, '0')
            val daysInMonth = when (selectedMonth) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> if (isLeapYear(selectedYear)) 29 else 28
                else -> 30
            }
            StatisticsReportPeriod(
                mode = mode,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                startDate = "$selectedYear-$monthStr-01",
                endDate = "$selectedYear-$monthStr-$daysInMonth"
            )
        } else {
            StatisticsReportPeriod(
                mode = mode,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                startDate = "$selectedYear-01-01",
                endDate = "$selectedYear-12-31"
            )
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    }
}
