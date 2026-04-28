package com.vinh.dyvat.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.InventoryLotCard
import com.vinh.dyvat.data.model.InventoryLotDetail
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryListUiState(
    val lots: List<InventoryLotCard> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showOutOfStock: Boolean = false
)

data class InventoryDetailUiState(
    val ticketId: String = "",
    val lotCode: String = "",
    val purchaseDate: String = "",
    val lotStatus: com.vinh.dyvat.data.model.LotStatus = com.vinh.dyvat.data.model.LotStatus.IN_STOCK,
    val totalValue: Long = 0L,
    val products: List<InventoryLotDetail> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(InventoryListUiState())
    val listState: StateFlow<InventoryListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(InventoryDetailUiState())
    val detailState: StateFlow<InventoryDetailUiState> = _detailState.asStateFlow()

    init {
        loadLots()
    }

    fun loadLots() {
        viewModelScope.launch {
            inventoryRepository.getLotCards(
                showOutOfStock = _listState.value.showOutOfStock
            ).collect { result ->
                _listState.value = when (result) {
                    is Result.Loading -> _listState.value.copy(isLoading = true, error = null)
                    is Result.Success -> _listState.value.copy(
                        isLoading = false,
                        lots = result.data,
                        error = null
                    )
                    is Result.Error -> _listState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun toggleShowOutOfStock() {
        _listState.value = _listState.value.copy(
            showOutOfStock = !_listState.value.showOutOfStock
        )
        loadLots()
    }

    fun loadLotDetail(ticketId: String) {
        viewModelScope.launch {
            _detailState.value = InventoryDetailUiState(ticketId = ticketId, isLoading = true)

            inventoryRepository.getLotDetails(ticketId).collect { result ->
                _detailState.value = when (result) {
                    is Result.Loading -> _detailState.value.copy(isLoading = true)
                    is Result.Success -> {
                        val products = result.data
                        _detailState.value.copy(
                            isLoading = false,
                            products = products,
                            lotCode = products.firstOrNull()?.lotCode ?: "",
                            purchaseDate = products.firstOrNull()?.purchaseDate ?: "",
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
}
