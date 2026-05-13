package com.vinh.dyvat.ui.screens.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.UnitModel
import com.vinh.dyvat.data.repository.ProductRepository
import com.vinh.dyvat.data.repository.UnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnitsUiState(
    val units: List<UnitModel> = emptyList(),
    val searchQuery: String = "",
    val showInactive: Boolean = false,
    val productCountsByUnit: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val editingUnit: UnitModel? = null,
    val unitToDeactivate: UnitModel? = null
) {
    val visibleUnits: List<UnitModel>
        get() = units
            .filter { it.isActive != showInactive }
            .filter {
                searchQuery.isBlank() ||
                    it.name.contains(searchQuery.trim(), ignoreCase = true)
            }
}

@HiltViewModel
class UnitsViewModel @Inject constructor(
    private val repository: UnitRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitsUiState())
    val uiState: StateFlow<UnitsUiState> = _uiState.asStateFlow()

    init {
        loadUnits()
    }

    fun loadUnits() {
        viewModelScope.launch {
            repository.getAll(activeOnly = false).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Result.Success -> {
                        val counts = when (val countResult = productRepository.getActiveProductCountsByUnit()) {
                            is Result.Success -> countResult.data
                            else -> _uiState.value.productCountsByUnit
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                units = result.data,
                                productCountsByUnit = counts,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setShowInactive(showInactive: Boolean) {
        _uiState.update { it.copy(showInactive = showInactive) }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingUnit = null)
    }

    fun showEditDialog(unit: UnitModel) {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingUnit = unit)
    }

    fun hideDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingUnit = null) }
    }

    fun showDeactivateDialog(unit: UnitModel) {
        _uiState.update { it.copy(unitToDeactivate = unit) }
    }

    fun hideDeactivateDialog() {
        _uiState.update { it.copy(unitToDeactivate = null) }
    }

    fun addUnit(name: String) {
        val trimmedName = name.trim()
        validateUnitName(trimmedName)?.let { message ->
            _uiState.update { it.copy(error = message) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.insert(trimmedName)
            when (result) {
                is Result.Success -> {
                    hideDialog()
                    _uiState.update { it.copy(isSaving = false) }
                    loadUnits()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isSaving = false,
                        error = result.message
                    ) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateUnit(id: String, name: String) {
        val trimmedName = name.trim()
        validateUnitName(trimmedName, currentId = id)?.let { message ->
            _uiState.update { it.copy(error = message) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.update(id, trimmedName)
            when (result) {
                is Result.Success -> {
                    hideDialog()
                    _uiState.update { it.copy(isSaving = false) }
                    loadUnits()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isSaving = false,
                        error = result.message
                    ) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun deactivateUnit(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.setActive(id, false)
            when (result) {
                is Result.Success -> {
                    hideDeactivateDialog()
                    _uiState.update { it.copy(isSaving = false) }
                    loadUnits()
                }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun restoreUnit(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = repository.setActive(id, true)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, showInactive = false) }
                    loadUnits()
                }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun validateUnitName(name: String, currentId: String? = null): String? {
        if (name.isBlank()) return "Tên đơn vị tính không được để trống"
        val duplicated = _uiState.value.units.firstOrNull {
            it.id != currentId && it.name.trim().equals(name, ignoreCase = true)
        } ?: return null
        return if (duplicated.isActive) {
            "Tên đơn vị tính này đã tồn tại"
        } else {
            "Tên này đã tồn tại trong mục đã ẩn. Hãy khôi phục đơn vị tính đó hoặc dùng tên khác."
        }
    }
}
