package com.vinh.dyvat.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.InventoryLotCard
import com.vinh.dyvat.data.model.InventoryLotDetail
import com.vinh.dyvat.data.model.LotStatus
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class InventoryListUiState(
    val lots: List<InventoryLotCard> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showOutOfStock: Boolean = false,
    val searchQuery: String = "",
    val sortOption: InventorySortOption = InventorySortOption.DATE_OLDEST,
    val fromDate: String = "",
    val toDate: String = "",
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 0
)

data class InventoryDetailUiState(
    val ticketId: String = "",
    val lotCode: String = "",
    val purchaseDate: String = "",
    val lotStatus: LotStatus = LotStatus.IN_STOCK,
    val totalValue: Long = 0L,
    val products: List<InventoryLotDetail> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class InventorySortOption(val label: String) {
    DATE_OLDEST("Ngày nhập cũ nhất trước"),
    DATE_NEWEST("Ngày nhập mới nhất trước"),
    VALUE_HIGHEST("Tổng giá trị tồn cao nhất"),
    VALUE_LOWEST("Tổng giá trị tồn thấp nhất")
}

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(InventoryListUiState())
    val listState: StateFlow<InventoryListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(InventoryDetailUiState())
    val detailState: StateFlow<InventoryDetailUiState> = _detailState.asStateFlow()

    private var loadedLots: List<InventoryLotCard> = emptyList()
    private var listLoadJob: Job? = null

    companion object {
        private const val PAGE_SIZE = 5
    }

    init {
        loadLots()
    }

    fun loadLots() {
        listLoadJob?.cancel()
        listLoadJob = viewModelScope.launch {
            val state = _listState.value
            _listState.value = state.copy(
                isLoading = true,
                isLoadingMore = false,
                error = null,
                currentPage = 0
            )
            inventoryRepository.getLotCards(
                startDate = state.fromDate.toApiDateOrNull(),
                endDate = state.toDate.toApiDateOrNull(),
                showOutOfStock = state.showOutOfStock
            ).collect { result ->
                _listState.value = when (result) {
                    is Result.Loading -> _listState.value.copy(isLoading = true, error = null)
                    is Result.Success -> {
                        loadedLots = result.data
                        val sortedLots = applySearchAndSort(result.data)
                        _listState.value.copy(
                            isLoading = false,
                            lots = sortedLots.take(PAGE_SIZE),
                            hasMore = sortedLots.size > PAGE_SIZE,
                            currentPage = 0,
                            error = null
                        )
                    }
                    is Result.Error -> _listState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun loadMore() {
        val state = _listState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _listState.value = state.copy(isLoadingMore = true, error = null)
            val nextPage = state.currentPage + 1
            val sortedLots = applySearchAndSort(loadedLots)
            val offset = nextPage * PAGE_SIZE
            val nextLots = sortedLots.drop(offset).take(PAGE_SIZE)

            _listState.value = _listState.value.copy(
                isLoadingMore = false,
                lots = state.lots + nextLots,
                hasMore = sortedLots.size > offset + nextLots.size,
                currentPage = nextPage
            )
        }
    }

    fun toggleShowOutOfStock() {
        _listState.value = _listState.value.copy(
            showOutOfStock = !_listState.value.showOutOfStock
        )
        loadLots()
    }

    fun setSearchQuery(query: String) {
        _listState.value = _listState.value.copy(
            searchQuery = query
        )
        loadLots()
    }

    fun setSortOption(option: InventorySortOption) {
        _listState.value = _listState.value.copy(
            sortOption = option
        )
        loadLots()
    }

    fun setFromDate(date: String) {
        _listState.value = _listState.value.copy(fromDate = date)
        loadLots()
    }

    fun setToDate(date: String) {
        _listState.value = _listState.value.copy(toDate = date)
        loadLots()
    }

    fun clearDateFilters() {
        _listState.value = _listState.value.copy(fromDate = "", toDate = "")
        loadLots()
    }

    fun loadLotDetail(ticketId: String) {
        viewModelScope.launch {
            _detailState.value = InventoryDetailUiState(ticketId = ticketId, isLoading = true)
            val lotCard = when (val cardResult = inventoryRepository.getLotCardByTicketId(ticketId)) {
                is Result.Success -> cardResult.data
                is Result.Error -> {
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = cardResult.message
                    )
                    return@launch
                }
                is Result.Loading -> null
            }

            inventoryRepository.getLotDetails(ticketId).collect { result ->
                _detailState.value = when (result) {
                    is Result.Loading -> _detailState.value.copy(isLoading = true)
                    is Result.Success -> {
                        val products = result.data
                        _detailState.value.copy(
                            isLoading = false,
                            products = products,
                            lotCode = lotCard?.lotCode ?: products.firstOrNull()?.lotCode ?: "",
                            purchaseDate = lotCard?.purchaseDate ?: products.firstOrNull()?.purchaseDate ?: "",
                            lotStatus = lotCard?.lotStatus ?: deriveLotStatus(products),
                            totalValue = products.sumOf { it.remainingValueVnd }
                        )
                    }
                    is Result.Error -> _detailState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _listState.value = _listState.value.copy(error = null)
    }

    private fun applySearchAndSort(
        lots: List<InventoryLotCard>,
        query: String = _listState.value.searchQuery,
        sortOption: InventorySortOption = _listState.value.sortOption
    ): List<InventoryLotCard> {
        val filtered = if (query.isBlank()) {
            lots
        } else {
            lots.filter {
                it.lotCode.contains(query, ignoreCase = true) ||
                    it.purchaseTicketId.contains(query, ignoreCase = true)
            }
        }

        return when (sortOption) {
            InventorySortOption.DATE_OLDEST -> filtered.sortedBy { it.purchaseDate }
            InventorySortOption.DATE_NEWEST -> filtered.sortedByDescending { it.purchaseDate }
            InventorySortOption.VALUE_HIGHEST -> filtered.sortedByDescending { it.totalInventoryValueVnd }
            InventorySortOption.VALUE_LOWEST -> filtered.sortedBy { it.totalInventoryValueVnd }
        }
    }

    private fun deriveLotStatus(products: List<InventoryLotDetail>): LotStatus {
        if (products.isEmpty()) return LotStatus.OUT_OF_STOCK

        val today = java.time.LocalDate.now().toString()
        val hasExpiredItem = products.any { product ->
            val expiryDate = product.expiryDate?.split("T")?.firstOrNull()
            expiryDate != null && expiryDate < today
        }

        return if (hasExpiredItem) {
            LotStatus.HAS_EXPIRED_ITEM
        } else {
            LotStatus.IN_STOCK
        }
    }

    private fun String.toApiDateOrNull(): String? {
        if (isBlank()) return null
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = inputFormat.parse(this)
            outputFormat.format(date!!)
        } catch (_: Exception) {
            null
        }
    }
}
