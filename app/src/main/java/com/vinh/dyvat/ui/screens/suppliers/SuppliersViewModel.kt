package com.vinh.dyvat.ui.screens.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.data.repository.ProductRepository
import com.vinh.dyvat.data.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuppliersUiState(
    val suppliers: List<Supplier> = emptyList(),
    val searchQuery: String = "",
    val showInactive: Boolean = false,
    val productCountsBySupplier: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val editingSupplier: Supplier? = null,
    val supplierToDeactivate: Supplier? = null
) {
    val visibleSuppliers: List<Supplier>
        get() = suppliers
            .filter { it.isActive != showInactive }
            .filter {
                val query = searchQuery.trim()
                query.isBlank() ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.phone.orEmpty().contains(query, ignoreCase = true)
            }
}

@HiltViewModel
class SuppliersViewModel @Inject constructor(
    private val repository: SupplierRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuppliersUiState())
    val uiState: StateFlow<SuppliersUiState> = _uiState.asStateFlow()

    init {
        loadSuppliers()
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            repository.getAll(activeOnly = false).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Result.Success -> {
                        val counts = when (val countResult = productRepository.getActiveProductCountsBySupplier()) {
                            is Result.Success -> countResult.data
                            else -> _uiState.value.productCountsBySupplier
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                suppliers = result.data,
                                productCountsBySupplier = counts,
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
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingSupplier = null)
    }

    fun showEditDialog(supplier: Supplier) {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingSupplier = supplier)
    }

    fun hideDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingSupplier = null) }
    }

    fun showDeactivateDialog(supplier: Supplier) {
        _uiState.update { it.copy(supplierToDeactivate = supplier) }
    }

    fun hideDeactivateDialog() {
        _uiState.update { it.copy(supplierToDeactivate = null) }
    }

    fun addSupplier(name: String, phone: String?) {
        val trimmedName = name.trim()
        val normalizedPhone = phone?.trim()?.ifBlank { null }
        validateSupplier(trimmedName, normalizedPhone)?.let { message ->
            _uiState.update { it.copy(error = message) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.insert(trimmedName, normalizedPhone)
            when (result) {
                is Result.Success -> {
                    hideDialog()
                    _uiState.update { it.copy(isSaving = false) }
                    loadSuppliers()
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

    fun updateSupplier(id: String, name: String, phone: String?) {
        val trimmedName = name.trim()
        val normalizedPhone = phone?.trim()?.ifBlank { null }
        validateSupplier(trimmedName, normalizedPhone, currentId = id)?.let { message ->
            _uiState.update { it.copy(error = message) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.update(id, trimmedName, normalizedPhone)
            when (result) {
                is Result.Success -> {
                    hideDialog()
                    _uiState.update { it.copy(isSaving = false) }
                    loadSuppliers()
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

    fun deactivateSupplier(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.setActive(id, false)
            when (result) {
                is Result.Success -> {
                    hideDeactivateDialog()
                    _uiState.update { it.copy(isSaving = false) }
                    loadSuppliers()
                }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun restoreSupplier(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = repository.setActive(id, true)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, showInactive = false) }
                    loadSuppliers()
                }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun validateSupplier(
        name: String,
        phone: String?,
        currentId: String? = null
    ): String? {
        if (name.isBlank()) return "Tên nhà cung cấp không được để trống"

        val duplicatedName = _uiState.value.suppliers.firstOrNull {
            it.id != currentId && it.name.trim().equals(name, ignoreCase = true)
        }
        if (duplicatedName != null) {
            return if (duplicatedName.isActive) {
                "Tên nhà cung cấp này đã tồn tại"
            } else {
                "Tên này đã tồn tại trong mục đã ẩn. Hãy khôi phục nhà cung cấp đó hoặc dùng tên khác."
            }
        }

        if (!phone.isNullOrBlank()) {
            val duplicatedPhone = _uiState.value.suppliers.firstOrNull {
                it.id != currentId && it.phone?.trim() == phone
            }
            if (duplicatedPhone != null) {
                return if (duplicatedPhone.isActive) {
                    "Số điện thoại này đã được dùng cho nhà cung cấp khác"
                } else {
                    "Số điện thoại này thuộc một nhà cung cấp đã ẩn. Hãy khôi phục mục đó hoặc dùng số khác."
                }
            }
        }

        return null
    }
}
