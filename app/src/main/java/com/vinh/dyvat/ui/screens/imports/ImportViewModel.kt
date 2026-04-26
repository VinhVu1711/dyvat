package com.vinh.dyvat.ui.screens.imports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.*
import com.vinh.dyvat.data.repository.ImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DateFilter {
    DAY, MONTH, YEAR, ALL
}

data class ImportUiState(
    val importItems: List<ImportItemWithDetails> = emptyList(),
    val filteredItems: List<ImportItemWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val dateFilter: DateFilter = DateFilter.ALL,
    val showAddEditDialog: Boolean = false,
    val editingItem: ImportItem? = null,
    val showStatusDialog: Boolean = false,
    val statusItem: ImportItemWithDetails? = null,
    val categories: List<Category> = emptyList(),
    val units: List<UnitModel> = emptyList(),
    val suppliers: List<Supplier> = emptyList()
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: ImportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    init {
        loadImportItems()
        loadDropdownData()
    }

    fun loadImportItems() {
        viewModelScope.launch {
            repository.getAllImportItems().collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            importItems = result.data,
                            filteredItems = filterItems(result.data, _uiState.value.dateFilter),
                            error = null
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadDropdownData() {
        viewModelScope.launch {
            repository.getCategories().collect { result ->
                if (result is Result.Success) {
                    _uiState.value = _uiState.value.copy(categories = result.data)
                }
            }
        }
        viewModelScope.launch {
            repository.getUnits().collect { result ->
                if (result is Result.Success) {
                    _uiState.value = _uiState.value.copy(units = result.data)
                }
            }
        }
        viewModelScope.launch {
            repository.getSuppliers().collect { result ->
                if (result is Result.Success) {
                    _uiState.value = _uiState.value.copy(suppliers = result.data)
                }
            }
        }
    }

    fun setDateFilter(filter: DateFilter) {
        _uiState.value = _uiState.value.copy(
            dateFilter = filter,
            filteredItems = filterItems(_uiState.value.importItems, filter)
        )
    }

    private fun filterItems(
        items: List<ImportItemWithDetails>,
        filter: DateFilter
    ): List<ImportItemWithDetails> {
        return items
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingItem = null
        )
    }

    fun showEditDialog(item: ImportItem) {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingItem = item
        )
    }

    fun hideAddEditDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = false,
            editingItem = null
        )
    }

    fun showStatusDialog(item: ImportItemWithDetails) {
        _uiState.value = _uiState.value.copy(
            showStatusDialog = true,
            statusItem = item
        )
    }

    fun hideStatusDialog() {
        _uiState.value = _uiState.value.copy(
            showStatusDialog = false,
            statusItem = null
        )
    }

    fun saveImportItem(
        productName: String,
        categoryId: String?,
        unitId: String,
        supplierId: String?,
        totalImportAmount: Double,
        unitPrice: Double,
        totalQuantity: Int,
        quantityForSale: Int,
        expiryDate: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val item = ImportItem(
                id = _uiState.value.editingItem?.id ?: "",
                importReceiptId = _uiState.value.editingItem?.importReceiptId ?: "",
                categoryId = categoryId,
                unitId = unitId,
                supplierId = supplierId,
                productName = productName,
                totalImportAmount = totalImportAmount,
                unitPrice = unitPrice,
                totalQuantity = totalQuantity,
                quantityForSale = quantityForSale,
                quantityInStock = totalQuantity - quantityForSale,
                status = if (quantityForSale > 0) ItemStatus.FOR_SALE else ItemStatus.IN_STOCK,
                saleLocation = if (quantityForSale > 0) "Kệ 1" else null,
                expiryDate = expiryDate,
                notes = notes
            )

            val result = if (_uiState.value.editingItem != null) {
                repository.updateImportItem(item.id, item)
            } else {
                repository.insertImportItem(item)
            }

            when (result) {
                is Result.Success -> {
                    hideAddEditDialog()
                    loadImportItems()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateItemStatus(
        itemId: String,
        status: ItemStatus,
        quantityForSale: Int?,
        saleLocation: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = repository.updateItemStatus(
                id = itemId,
                status = status,
                quantityForSale = quantityForSale,
                saleLocation = saleLocation
            )

            when (result) {
                is Result.Success -> {
                    hideStatusDialog()
                    loadImportItems()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun deleteImportItem(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = repository.deleteImportItem(id)
            when (result) {
                is Result.Success -> loadImportItems()
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
