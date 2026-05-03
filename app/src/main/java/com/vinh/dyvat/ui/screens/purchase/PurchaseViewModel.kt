package com.vinh.dyvat.ui.screens.purchase

import android.util.Log
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import javax.inject.Inject

private const val TAG = "PurchaseViewModel"

data class PurchaseListUiState(
    val tickets: List<PurchaseTicketCardUi> = emptyList(),
    val filteredTickets: List<PurchaseTicketCardUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedStatusFilter: TicketStatusFilter = TicketStatusFilter.ALL,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_NEWEST,
    val fromDate: String = "",
    val toDate: String = ""
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
    val isInitialized: Boolean = false,
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
    val priceError: String? = null,
    val expiryDateError: String? = null
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
                purchasePrice.toLongOrNull()?.let { it >= 0 } == true &&
                expiryDate.isNotBlank()
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

    private val lookupMutex = Mutex()

    init {
        loadTickets()
    }

    private suspend fun <T> Flow<Result<T>>.awaitResult(): Result<T> {
        var latest: Result<T> = Result.Loading
        collect { result ->
            if (result !is Result.Loading) {
                latest = result
            }
        }
        return latest
    }

    private suspend fun fetchLookups(): PurchaseLookups {
        Log.d(TAG, "fetchLookups: fetching categories, units, suppliers")
        val categoriesResult = categoryRepository.getAll().awaitResult()
        val suppliersResult = supplierRepository.getAll().awaitResult()
        val unitsResult = unitRepository.getAll().awaitResult()
        // Fetch all products without pagination
        val productsResult = productRepository.getAll(activeOnly = true, pageSize = 1000).awaitResult()

        val categoriesData = (categoriesResult as? Result.Success)?.data ?: emptyList()
        val suppliersData = (suppliersResult as? Result.Success)?.data ?: emptyList()
        val unitsData = (unitsResult as? Result.Success)?.data ?: emptyList()
        val productsData = (productsResult as? Result.Success)?.data ?: emptyList()

        Log.d(TAG, "fetchLookups: loaded - categories=${categoriesData.size}, suppliers=${suppliersData.size}, units=${unitsData.size}, products=${productsData.size}")

        return PurchaseLookups(
            categories = categoriesData,
            suppliers = suppliersData,
            units = unitsData,
            products = productsData
        )
    }

    private data class PurchaseLookups(
        val categories: List<Category>,
        val suppliers: List<Supplier>,
        val units: List<UnitModel>,
        val products: List<ProductWithDetails>
    )

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
            filteredTickets = applyFilters(_listState.value.tickets)
        )
    }

    fun setSearchQuery(query: String) {
        _listState.value = _listState.value.copy(
            searchQuery = query,
            filteredTickets = applyFilters(_listState.value.tickets)
        )
    }

    fun setSortOption(option: SortOption) {
        _listState.value = _listState.value.copy(
            sortOption = option,
            filteredTickets = applyFilters(_listState.value.tickets)
        )
    }

    fun setFromDate(date: String) {
        _listState.value = _listState.value.copy(
            fromDate = date,
            filteredTickets = applyFilters(_listState.value.tickets)
        )
    }

    fun setToDate(date: String) {
        _listState.value = _listState.value.copy(
            toDate = date,
            filteredTickets = applyFilters(_listState.value.tickets)
        )
    }

    private fun applyFilters(tickets: List<PurchaseTicketCardUi>): List<PurchaseTicketCardUi> {
        val state = _listState.value
        var result = tickets

        // Apply status filter
        result = when (state.selectedStatusFilter) {
            TicketStatusFilter.ALL -> result
            TicketStatusFilter.ACTIVE -> result.filter { it.status == com.vinh.dyvat.data.model.TicketStatus.ACTIVE }
            TicketStatusFilter.CANCELLED -> result.filter { it.status == com.vinh.dyvat.data.model.TicketStatus.CANCELLED }
        }

        // Apply search query
        if (state.searchQuery.isNotBlank()) {
            result = result.filter { ticket ->
                ticket.code.contains(state.searchQuery, ignoreCase = true) ||
                ticket.id.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // Apply date filter
        if (state.fromDate.isNotBlank()) {
            result = result.filter { ticket ->
                ticket.purchaseDate >= state.fromDate
            }
        }
        if (state.toDate.isNotBlank()) {
            result = result.filter { ticket ->
                ticket.purchaseDate <= state.toDate
            }
        }

        // Apply sorting
        result = when (state.sortOption) {
            SortOption.DATE_NEWEST -> result.sortedByDescending { it.purchaseDate }
            SortOption.DATE_OLDEST -> result.sortedBy { it.purchaseDate }
            SortOption.AMOUNT_HIGHEST -> result.sortedByDescending { it.totalAmountVnd }
            SortOption.AMOUNT_LOWEST -> result.sortedBy { it.totalAmountVnd }
        }

        return result
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
            try {
                Log.d(TAG, "initForm: waiting for fetchLookups to complete")
                lookupMutex.withLock {
                    val lookups = fetchLookups()
                    Log.d(TAG, "initForm: lookup data ready - products=${lookups.products.size}, suppliers=${lookups.suppliers.size}, units=${lookups.units.size}, categories=${lookups.categories.size}")
                    _formState.value = PurchaseFormUiState(
                        isInitialized = true,
                        purchaseDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        availableProducts = lookups.products,
                        suppliers = lookups.suppliers,
                        units = lookups.units,
                        categories = lookups.categories
                    )
                    Log.d(TAG, "initForm: state initialized with isInitialized=true, items count = ${_formState.value.items.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "initForm: failed - ${e.message}", e)
                _formState.value = PurchaseFormUiState(
                    isInitialized = true,
                    purchaseDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                )
            }
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

    fun addFormItemWithDetails(
        product: ProductWithDetails,
        supplier: Supplier?,
        quantity: String,
        expiryDate: String,
        price: String
    ) {
        Log.d(TAG, "addFormItemWithDetails: START - product=${product.product.name}, qty=$quantity, expiry=$expiryDate, price=$price")
        val currentItems = _formState.value.items
        Log.d(TAG, "addFormItemWithDetails: current items count = ${currentItems.size}")
        val newId = if (currentItems.isEmpty()) 0 else (currentItems.maxOf { it.id } + 1)
        val newItem = PurchaseItemDraftUi(
            id = newId,
            productId = product.product.id,
            productName = product.product.name,
            supplierId = supplier?.id ?: "",
            supplierName = supplier?.name ?: "",
            unitId = product.product.unitId,
            unitName = product.unitName,
            expiryDate = expiryDate,
            quantity = quantity,
            purchasePrice = price
        )
        Log.d(TAG, "addFormItemWithDetails: new item created with id=$newId, lineTotal=${newItem.lineTotal}")
        _formState.value = _formState.value.copy(
            items = currentItems + newItem
        )
        Log.d(TAG, "addFormItemWithDetails: COMPLETE - items count after add = ${_formState.value.items.size}, totalAmount=${_formState.value.totalAmount}")
    }

    fun removeFormItem(itemId: Int) {
        Log.d(TAG, "removeFormItem: itemId=$itemId, items before = ${_formState.value.items.size}")
        _formState.value = _formState.value.copy(
            items = _formState.value.items.filter { it.id != itemId }
        )
        Log.d(TAG, "removeFormItem: items after = ${_formState.value.items.size}")
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

        // Guard: prevent double submit
        if (state.isSaving) {
            Log.d(TAG, "saveTicket: already saving, ignoring duplicate call")
            return
        }

        // Guard: check empty items
        if (state.items.isEmpty()) {
            _formState.value = state.copy(error = "Phải có ít nhất 1 sản phẩm")
            return
        }

        // Validate all items
        var hasErrors = false
        val validatedItems = state.items.map { item ->
            var updatedItem = item
            var itemHasError = false

            if (item.productId.isBlank()) {
                updatedItem = updatedItem.copy(productError = "Chọn sản phẩm")
                hasErrors = true
                itemHasError = true
            } else {
                updatedItem = updatedItem.copy(productError = null)
            }

            val qty = item.quantity.toIntOrNull()
            if (qty == null || qty <= 0) {
                updatedItem = updatedItem.copy(quantityError = "Số lượng phải lớn hơn 0")
                hasErrors = true
                itemHasError = true
            } else {
                updatedItem = updatedItem.copy(quantityError = null)
            }

            val price = item.purchasePrice.toLongOrNull()
            if (price == null || price < 0) {
                updatedItem = updatedItem.copy(priceError = "Giá không hợp lệ")
                hasErrors = true
                itemHasError = true
            } else {
                updatedItem = updatedItem.copy(priceError = null)
            }

            // Validate expiry date
            if (item.expiryDate.isBlank()) {
                updatedItem = updatedItem.copy(expiryDateError = "Ngày hết hạn là bắt buộc")
                hasErrors = true
                itemHasError = true
            } else {
                // Check if expiry date is greater than purchase date
                val expiryError = validateExpiryDate(item.expiryDate, state.purchaseDate)
                if (expiryError != null) {
                    updatedItem = updatedItem.copy(expiryDateError = expiryError)
                    hasErrors = true
                    itemHasError = true
                } else {
                    updatedItem = updatedItem.copy(expiryDateError = null)
                }
            }

            updatedItem
        }

        if (hasErrors) {
            Log.w(TAG, "validateAndSaveTicket: validation failed")
            _formState.value = state.copy(items = validatedItems)
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSaving = true, error = null)

            // Convert date from dd/MM/yyyy to yyyy-MM-dd for API
            val apiDate = try {
                val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.US)
                val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = inputFormat.parse(state.purchaseDate)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                Log.e(TAG, "saveTicket: date parse error - ${e.message}")
                state.purchaseDate
            }

            val drafts = state.items.map { item ->
                Log.d(TAG, "saveTicket: mapping item - productId=${item.productId}, qty=${item.quantity}, price=${item.purchasePrice}")
                PurchaseItemDraft(
                    productId = item.productId,
                    supplierId = item.supplierId,
                    unitId = item.unitId,
                    expiryDate = item.expiryDate,
                    quantityPurchased = item.quantity.toInt(),
                    purchasePriceVnd = item.purchasePrice.toLong()
                )
            }

            Log.d(TAG, "saveTicket: creating ticket with ${drafts.size} items, date=$apiDate")

            when (val result = purchaseRepository.createTicket(apiDate, drafts)) {
                is Result.Success -> {
                    Log.d(TAG, "saveTicket: SUCCESS - ticketId=${result.data}")
                    _formState.value = _formState.value.copy(isSaving = false)
                    loadTickets()
                    onSuccess()
                }
                is Result.Error -> {
                    Log.e(TAG, "saveTicket: ERROR - ${result.message}")
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun validateExpiryDate(expiryDate: String, purchaseDate: String): String? {
        if (expiryDate.isBlank()) {
            return "Ngày hết hạn là bắt buộc"
        }
        return try {
            val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val expiryDateObj = inputFormat.parse(expiryDate)
            val purchaseDateObj = inputFormat.parse(purchaseDate)
            if (expiryDateObj != null && purchaseDateObj != null) {
                if (expiryDateObj <= purchaseDateObj) {
                    return "Ngày hết hạn phải lớn hơn ngày nhập hàng"
                }
            }
            null
        } catch (_: Exception) {
            "Ngày hết hạn không hợp lệ"
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
