package com.vinh.dyvat.ui.screens.products

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.ProductImportCommitResult
import com.vinh.dyvat.data.model.ProductImportError
import com.vinh.dyvat.data.model.ProductImportSummary
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.repository.ProductImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProductImportPhase {
    IDLE,
    VALIDATING,
    NEEDS_FIX,
    READY,
    COMMITTING,
    SUCCESS,
    ERROR
}

data class ProductImportUiState(
    val phase: ProductImportPhase = ProductImportPhase.IDLE,
    val fileName: String = "",
    val fileSizeBytes: Long? = null,
    val summary: ProductImportSummary? = null,
    val errors: List<ProductImportError> = emptyList(),
    val importToken: String? = null,
    val commitResult: ProductImportCommitResult? = null,
    val message: String? = null
)

@HiltViewModel
class ProductImportViewModel @Inject constructor(
    private val repository: ProductImportRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductImportUiState())
    val uiState: StateFlow<ProductImportUiState> = _uiState.asStateFlow()

    fun validateFile(uri: Uri, fileName: String, fileSizeBytes: Long?) {
        viewModelScope.launch {
            _uiState.value = ProductImportUiState(
                phase = ProductImportPhase.VALIDATING,
                fileName = fileName,
                fileSizeBytes = fileSizeBytes,
                message = "Đang kiểm tra file..."
            )

            when (val result = repository.validateFile(uri, fileName, fileSizeBytes)) {
                is Result.Success -> {
                    val response = result.data
                    _uiState.value = if (response.success) {
                        if (response.importToken.isNullOrBlank()) {
                            _uiState.value.copy(
                                phase = ProductImportPhase.ERROR,
                                summary = response.summary,
                                errors = emptyList(),
                                importToken = null,
                                message = "Server chưa trả mã import. Vui lòng kiểm tra file lại."
                            )
                        } else {
                            _uiState.value.copy(
                                phase = ProductImportPhase.READY,
                                summary = response.summary,
                                errors = emptyList(),
                                importToken = response.importToken,
                                message = response.message.ifBlank { "File hợp lệ và sẵn sàng import." }
                            )
                        }
                    } else {
                        _uiState.value.copy(
                            phase = if (response.errors.isEmpty()) {
                                ProductImportPhase.ERROR
                            } else {
                                ProductImportPhase.NEEDS_FIX
                            },
                            summary = response.summary,
                            errors = response.errors,
                            importToken = null,
                            message = response.message.ifBlank {
                                "File còn lỗi dữ liệu. Chưa có dữ liệu nào được nhập."
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        phase = ProductImportPhase.ERROR,
                        message = result.message
                    )
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun commitImport() {
        val token = _uiState.value.importToken ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                phase = ProductImportPhase.COMMITTING,
                message = "Đang import sản phẩm..."
            )

            when (val result = repository.commitImport(token)) {
                is Result.Success -> {
                    val response = result.data
                    _uiState.value = if (response.success) {
                        _uiState.value.copy(
                            phase = ProductImportPhase.SUCCESS,
                            commitResult = response.result,
                            message = response.message.ifBlank { "Import sản phẩm thành công." }
                        )
                    } else {
                        _uiState.value.copy(
                            phase = ProductImportPhase.ERROR,
                            message = response.message.ifBlank { "Import thất bại. Chưa có dữ liệu nào được nhập." }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        phase = ProductImportPhase.ERROR,
                        message = result.message
                    )
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun reset() {
        _uiState.value = ProductImportUiState()
    }
}
