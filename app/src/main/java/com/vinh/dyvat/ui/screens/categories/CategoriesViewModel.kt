package com.vinh.dyvat.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.Category
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.repository.CategoryRepository
import com.vinh.dyvat.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val showInactive: Boolean = false,
    val productCountsByCategory: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val editingCategory: Category? = null,
    val categoryToDeactivate: Category? = null
) {
    val visibleCategories: List<Category>
        get() = categories
            .filter { it.isActive != showInactive }
            .filter {
                searchQuery.isBlank() ||
                    it.name.contains(searchQuery.trim(), ignoreCase = true)
            }
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CategoryRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            repository.getAll(activeOnly = false).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Result.Success -> {
                        val counts = when (val countResult = productRepository.getActiveProductCountsByCategory()) {
                            is Result.Success -> countResult.data
                            else -> _uiState.value.productCountsByCategory
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                categories = result.data,
                                productCountsByCategory = counts,
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
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingCategory = null)
    }

    fun showEditDialog(category: Category) {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingCategory = category)
    }

    fun hideDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingCategory = null) }
    }

    fun showDeactivateDialog(category: Category) {
        _uiState.update { it.copy(categoryToDeactivate = category) }
    }

    fun hideDeactivateDialog() {
        _uiState.update { it.copy(categoryToDeactivate = null) }
    }

    fun addCategory(name: String) {
        val trimmedName = name.trim()
        validateCategoryName(trimmedName)?.let { message ->
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
                    loadCategories()
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

    fun updateCategory(id: String, name: String) {
        val trimmedName = name.trim()
        validateCategoryName(trimmedName, currentId = id)?.let { message ->
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
                    loadCategories()
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

    fun deactivateCategory(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = repository.setActive(id, false)
            when (result) {
                is Result.Success -> {
                    hideDeactivateDialog()
                    _uiState.update { it.copy(isSaving = false) }
                    loadCategories()
                }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun restoreCategory(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = repository.setActive(id, true)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, showInactive = false) }
                    loadCategories()
                }
                is Result.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun validateCategoryName(name: String, currentId: String? = null): String? {
        if (name.isBlank()) return "Tên loại sản phẩm không được để trống"
        val duplicated = _uiState.value.categories.firstOrNull {
            it.id != currentId && it.name.trim().equals(name, ignoreCase = true)
        } ?: return null
        return if (duplicated.isActive) {
            "Tên loại sản phẩm này đã tồn tại"
        } else {
            "Tên này đã tồn tại trong mục đã ẩn. Hãy khôi phục loại sản phẩm đó hoặc dùng tên khác."
        }
    }
}
