package com.vinh.dyvat.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.Category
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val editingCategory: Category? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            repository.getAllCategories().collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> _uiState.value.copy(isLoading = true, error = null)
                    is Result.Success -> _uiState.value.copy(
                        isLoading = false,
                        categories = result.data,
                        error = null
                    )
                    is Result.Error -> _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingCategory = null)
    }

    fun showEditDialog(category: Category) {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingCategory = category)
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, editingCategory = null)
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.insertCategory(name)
            when (result) {
                is Result.Success -> {
                    hideDialog()
                    loadCategories()
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

    fun updateCategory(id: String, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.updateCategory(id, name)
            when (result) {
                is Result.Success -> {
                    hideDialog()
                    loadCategories()
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

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.deleteCategory(id)
            when (result) {
                is Result.Success -> loadCategories()
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
