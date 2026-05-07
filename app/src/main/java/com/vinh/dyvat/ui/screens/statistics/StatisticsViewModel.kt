package com.vinh.dyvat.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.DailySummary
import com.vinh.dyvat.data.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import com.vinh.dyvat.data.model.Result

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
    val saleTicketCount: Int = 0
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: StatisticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

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
}
