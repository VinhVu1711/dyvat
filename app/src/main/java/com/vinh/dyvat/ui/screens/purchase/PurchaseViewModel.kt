package com.vinh.dyvat.ui.screens.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.Category
import com.vinh.dyvat.data.model.ProductWithDetails
import com.vinh.dyvat.data.model.PurchaseItemWithDetails
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.data.model.TicketStatus
import com.vinh.dyvat.data.model.UnitModel
import com.vinh.dyvat.data.repository.CategoryRepository
import com.vinh.dyvat.data.repository.ProductRepository
import com.vinh.dyvat.data.repository.PurchaseItemDraft
import com.vinh.dyvat.data.repository.PurchaseRepository
import com.vinh.dyvat.data.repository.SupplierRepository
import com.vinh.dyvat.data.repository.UnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseListUiState(
    val tickets: List<PurchaseTicketCardUi> = emptyList(),
    val filteredTickets: List<PurchaseTicketCardUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedStatusFilter: TicketStatusFilter = TicketStatusFilter.ALL
)

data class PurchaseDetailUiState(
    val ticket: PurchaseTicketUi? = null,
    val items: List<PurchaseItemWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCancelConfirm: Boolean = false,
    val actionError: String? = null
)

data class PurchaseFormUiState(
    val purchaseDate: String = "",
    val items: List<PurchaseItemDraftUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<Int, String> = emptyMap(),
    val availableProducts: List<ProductWithDetails> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val units: List<UnitModel> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false
) {
    val totalAmount: Long
        get() = items.filter { it.isValid }.sumOf { it.lineTotal }

    val isValid: Boolean
        get() = items.isNotEmpty() && items.all { it.isValid }
}

data class PurchaseTicketCardUi(
    val id: String,
    val code: String,
    val purchaseDate: String,
    val status: TicketStatus,
    val totalAmountVnd: Long,
    val itemCount: Int
)

data class PurchaseTicketUi(
    val id: String,
    val code: String,
    val purchaseDate: String,
    val status: TicketStatus,
    val cancelledAt: String?,
    val cancelReason: String?
)

data class PurchaseItemDraftUi(
    val id: Int,
    val productId: String = "",
    val productName: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val unitId: String = "",
    val unitName: String = "",
    val expiryDate: String = "",
    val quantity: String = "",
    val purchasePrice: String = "",
    val productError: String? = null,
    val quantityError: String? = null,
    val priceError: String? = null
) {
    val lineTotal: Long
        get() {
            val qty = quantity.toLongOrNull() ?: 0L
            val price = purchasePrice.toLongOrNull() ?: 0L
            return qty * price
        }

    val isValid: Boolean
        get() = productId.isNotBlank() &&
                quantity.toIntOrNull()?.let { it > 0 } == true &&
                purchasePrice.toLongOrNull()?.let { it >= 0 } == true
}

enum class TicketStatusFilter {
    ALL, ACTIVE, CANCELLED
}

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val unitRepository: UnitRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(PurchaseListUiState())
    val listState: StateFlow<PurchaseListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(PurchaseDetailUiState())
    val detailState: StateFlow<PurchaseDetailUiState> = _detailState.asStateFlow()

    private val _formState = MutableStateFlow(PurchaseFormUiState())
    val formState: StateFlow<PurchaseFormUiState> = _formState.asStateFlow()

    init {
        loadTickets()
    }

    // --- List ---

    fun loadTickets() {
        viewModelScope.launch {
            purchaseRepository.getTicketCards().collect { result ->
                _listState.value = when (result) {
                    is Result.Loading -> _listState.value.copy(isLoading = true, error = null)
                    is Result.Success -> {
                        val cards = result.data.map { card ->
                            PurchaseTicketCardUi(
                                id = card.id,
                                code = card.code,
                                purchaseDate = card.purchaseDate,
                                status = card.status,
                                totalAmountVnd = card.totalPurchaseAmountVnd,
                                itemCount = card.itemCount
                            )
                        }
                        _listState.value.copy(
                            isLoading = false,
                            tickets = cards,
                            filteredTickets = applyStatusFilter(cards),
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

    fun setStatusFilter(filter: TicketStatusFilter) {
        _listState.value = _listState.value.copy(
            selectedStatusFilter = filter,
            filteredTickets = applyStatusFilter(_listState.value.tickets)
        )
    }

    private fun applyStatusFilter(tickets: List<PurchaseTicketCardUi>): List<PurchaseTicketCardUi> {
        return when (_listState.value.selectedStatusFilter) {
            TicketStatusFilter.ALL -> tickets
            TicketStatusFilter.ACTIVE -> tickets.filter { it.status == com.vinh.dyvat.data.model.TicketStatus.ACTIVE }
            TicketStatusFilter.CANCELLED -> tickets.filter { it.status == com.vinh.dyvat.data.model.TicketStatus.CANCELLED }
        }
    }

    // --- Detail ---

    fun loadTicketDetail(ticketId: String) {
        viewModelScope.launch {
            _detailState.value = PurchaseDetailUiState(isLoading = true)

            when (val ticketResult = purchaseRepository.getTicketById(ticketId)) {
                is Result.Success -> {
                    val t = ticketResult.data
                    val ticket = PurchaseTicketUi(
                        id = t.id,
                        code = t.code,
                        purchaseDate = t.purchaseDate,
                        status = t.status,
                        cancelledAt = t.cancelledAt,
                        cancelReason = t.cancelReason
                    )

                    purchaseRepository.getItemsByTicketId(ticketId).collect { itemsResult ->
                        _detailState.value = when (itemsResult) {
                            is Result.Loading -> PurchaseDetailUiState(ticket = ticket, isLoading = true)
                            is Result.Success -> PurchaseDetailUiState(
                                ticket = ticket,
                                items = itemsResult.data,
                                isLoading = false
                            )
                            is Result.Error -> PurchaseDetailUiState(
                                ticket = ticket,
                                isLoading = false,
                                error = itemsResult.message
                            )
                        }
                    }
                }
                is Result.Error -> _detailState.value = PurchaseDetailUiState(
                    isLoading = false,
                    error = ticketResult.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun showCancelConfirm() {
        _detailState.value = _detailState.value.copy(showCancelConfirm = true)
    }

    fun hideCancelConfirm() {
        _detailState.value = _detailState.value.copy(showCancelConfirm = false)
    }

    fun cancelTicket(ticketId: String, reason: String?) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, actionError = null)
            when (val result = purchaseRepository.cancelTicket(ticketId, reason)) {
                is Result.Success -> {
                    _detailState.value = _detailState.value.copy(
                        showCancelConfirm = false,
                        isLoading = false
                    )
                    loadTicketDetail(ticketId)
                    loadTickets()
                }
                is Result.Error -> _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    actionError = result.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun clearDetailError() {
        _detailState.value = _detailState.value.copy(actionError = null)
    }

    // --- Form ---

    fun initForm() {
        viewModelScope.launch {
            _formState.value = PurchaseFormUiState(
                purchaseDate = java.time.LocalDate.now().toString()
            )

            val products = productRepository.getAll(activeOnly = true).first()
            val suppliers = supplierRepository.getAll().first()
            val units = unitRepository.getAll().first()
            val categories = categoryRepository.getAll().first()

            _formState.value = _formState.value.copy(
                availableProducts = (products as? Result.Success)?.data ?: emptyList(),
                suppliers = (suppliers as? Result.Success)?.data ?: emptyList(),
                units = (units as? Result.Success)?.data ?: emptyList(),
                categories = (categories as? Result.Success)?.data ?: emptyList()
            )
        }
    }

    fun addFormItem() {
        val currentItems = _formState.value.items
        val newId = if (currentItems.isEmpty()) 0 else (currentItems.maxOf { it.id } + 1)
        _formState.value = _formState.value.copy(
            items = currentItems + PurchaseItemDraftUi(id = newId),
            validationErrors = _formState.value.validationErrors - newId
        )
    }

    fun removeFormItem(itemId: Int) {
        _formState.value = _formState.value.copy(
            items = _formState.value.items.filter { it.id != itemId }
        )
    }

    fun updateFormItemProduct(itemId: Int, productId: String, productName: String, unitId: String, unitName: String) {
        _formState.value = _formState.value.copy(
            items = _formState.value.items.map { item ->
                if (item.id == itemId) item.copy(
                    productId = productId,
                    productName = productName,
                    unitId = unitId,
                    unitName = unitName,
                    productError = null
                ) else item
            }
        )
    }

    fun updateFormItemSupplier(itemId: Int, supplierId: String, supplierName: String) {
        _formState.value = _formState.value.copy(
            items = _formState.value.items.map { item ->
                if (item.id == itemId) item.copy(
                    supplierId = supplierId,
                    supplierName = supplierName
                ) else item
            }
        )
    }

    fun updateFormItemExpiry(itemId: Int, expiryDate: String) {
        _formState.value = _formState.value.copy(
            items = _formState.value.items.map { item ->
                if (item.id == itemId) item.copy(expiryDate = expiryDate) else item
            }
        )
    }

    fun updateFormItemQuantity(itemId: Int, quantity: String) {
        val cleaned = quantity.filter { it.isDigit() }
        _formState.value = _formState.value.copy(
            items = _formState.value.items.map { item ->
                if (item.id == itemId) item.copy(
                    quantity = cleaned,
                    quantityError = null
                ) else item
            }
        )
    }

    fun updateFormItemPrice(itemId: Int, price: String) {
        val cleaned = price.filter { it.isDigit() }
        _formState.value = _formState.value.copy(
            items = _formState.value.items.map { item ->
                if (item.id == itemId) item.copy(
                    purchasePrice = cleaned,
                    priceError = null
                ) else item
            }
        )
    }

    fun saveTicket(onSuccess: () -> Unit) {
        val state = _formState.value
        var hasErrors = false
        val errors = mutableMapOf<Int, String>()

        val validatedItems = state.items.map { item ->
            var updatedItem = item
            if (item.productId.isBlank()) {
                updatedItem = updatedItem.copy(productError = "Chon san pham")
                errors[item.id] = "Chon san pham"
                hasErrors = true
            }
            val qty = item.quantity.toIntOrNull()
            if (qty == null || qty <= 0) {
                updatedItem = updatedItem.copy(quantityError = "So luong khong hop le")
                errors[item.id] = "So luong khong hop le"
                hasErrors = true
            }
            val price = item.purchasePrice.toLongOrNull()
            if (price == null || price < 0) {
                updatedItem = updatedItem.copy(priceError = "Gia khong hop le")
                errors[item.id] = "Gia khong hop le"
                hasErrors = true
            }
            updatedItem
        }

        if (hasErrors) {
            _formState.value = state.copy(
                items = validatedItems,
                validationErrors = errors
            )
            return
        }

        if (state.items.isEmpty()) {
            _formState.value = state.copy(error = "Phai co it nhat 1 san pham")
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSaving = true, error = null)

            val drafts = state.items.map { item ->
                PurchaseItemDraft(
                    productId = item.productId,
                    supplierId = item.supplierId,
                    unitId = item.unitId,
                    expiryDate = item.expiryDate.ifBlank { null },
                    quantityPurchased = item.quantity.toInt(),
                    purchasePriceVnd = item.purchasePrice.toLong()
                )
            }

            when (val result = purchaseRepository.createTicket(state.purchaseDate, drafts)) {
                is Result.Success -> {
                    _formState.value = _formState.value.copy(isSaving = false)
                    loadTickets()
                    onSuccess()
                }
                is Result.Error -> _formState.value = _formState.value.copy(
                    isSaving = false,
                    error = result.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun updatePurchaseDate(date: String) {
        _formState.value = _formState.value.copy(purchaseDate = date)
    }

    fun clearFormError() {
        _formState.value = _formState.value.copy(error = null)
    }

    fun clearError() {
        _listState.value = _listState.value.copy(error = null)
    }
}
