package com.vinh.dyvat.ui.screens.sales

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.AvailableLot
import com.vinh.dyvat.data.model.ProductWithDetails
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.SaleItemWithDetails
import com.vinh.dyvat.data.model.SaleTicketStatus
import com.vinh.dyvat.data.repository.ProductRepository
import com.vinh.dyvat.data.repository.SaleItemDraft
import com.vinh.dyvat.data.repository.SaleRepository
import com.vinh.dyvat.data.repository.SaleTicketSortField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject

private const val TAG = "SaleViewModel"
private val SALE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

data class SaleListUiState(
    val tickets: List<SaleTicketCardUi> = emptyList(),
    val filteredTickets: List<SaleTicketCardUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedStatusFilter: SaleStatusFilter = SaleStatusFilter.ALL,
    val searchQuery: String = "",
    val sortOption: SaleSortOption = SaleSortOption.DATE_NEWEST,
    val fromDate: String = "",
    val toDate: String = "",
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 0
)

data class SaleDetailUiState(
    val ticket: SaleTicketUi? = null,
    val items: List<SaleItemWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCancelConfirm: Boolean = false,
    val actionError: String? = null
)

data class SaleFormUiState(
    val isInitialized: Boolean = false,
    val saleDate: String = "",
    val items: List<SaleItemDraftUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val saleDateError: String? = null,
    val availableProducts: List<ProductWithDetails> = emptyList(),
    val isSaving: Boolean = false
) {
    val totalRevenue: Long
        get() = items.filter { it.isValid }.sumOf { it.lineRevenue }
}

data class SaleTicketCardUi(
    val id: String,
    val code: String,
    val saleDate: String,
    val status: SaleTicketStatus,
    val totalSaleAmountVnd: Long,
    val totalCostAmountVnd: Long,
    val profitVnd: Long,
    val itemCount: Int
)

data class SaleTicketUi(
    val id: String,
    val code: String,
    val saleDate: String,
    val status: SaleTicketStatus,
    val cancelledAt: String?,
    val cancelReason: String?
)

data class SaleItemDraftUi(
    val id: Int,
    val productId: String = "",
    val productName: String = "",
    val unitId: String = "",
    val unitName: String = "",
    val purchaseItemId: String = "",
    val lotCode: String = "",
    val expiryDate: String? = null,
    val quantityRemaining: Int = 0,
    val purchasePriceVnd: Long = 0L,
    val quantity: String = "",
    val salePrice: String = "",
    val productError: String? = null,
    val lotError: String? = null,
    val quantityError: String? = null,
    val priceError: String? = null
) {
    val lineRevenue: Long
        get() = (quantity.toLongOrNull() ?: 0L) * (salePrice.toLongOrNull() ?: 0L)

    val isValid: Boolean
        get() = productId.isNotBlank() &&
                purchaseItemId.isNotBlank() &&
                quantity.toIntOrNull()?.let { it > 0 && it <= quantityRemaining } == true &&
                salePrice.toLongOrNull()?.let { it >= 0 } == true
}

enum class SaleStatusFilter {
    ALL, ACTIVE, CANCELLED
}

enum class SaleSortOption(val label: String) {
    DATE_NEWEST("Ngày mới nhất trước"),
    DATE_OLDEST("Ngày cũ nhất trước"),
    AMOUNT_HIGHEST("Tổng tiền cao nhất"),
    AMOUNT_LOWEST("Tổng tiền thấp nhất")
}

@HiltViewModel
class SaleViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(SaleListUiState())
    val listState: StateFlow<SaleListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(SaleDetailUiState())
    val detailState: StateFlow<SaleDetailUiState> = _detailState.asStateFlow()

    private val _formState = MutableStateFlow(SaleFormUiState())
    val formState: StateFlow<SaleFormUiState> = _formState.asStateFlow()

    private val lookupMutex = Mutex()
    private var listLoadJob: Job? = null

    companion object {
        private const val PAGE_SIZE = 5
    }

    init {
        loadTickets()
    }

    private suspend fun <T> Flow<Result<T>>.awaitResult(): Result<T> {
        var latest: Result<T> = Result.Loading
        collect { result ->
            if (result !is Result.Loading) latest = result
        }
        return latest
    }

    fun loadTickets() {
        listLoadJob?.cancel()
        listLoadJob = viewModelScope.launch {
            val state = _listState.value
            _listState.value = state.copy(isLoading = true, isLoadingMore = false, error = null, currentPage = 0)
            saleRepository.getTicketCards(
                startDate = state.fromDate.toApiDateOrNull(),
                endDate = state.toDate.toApiDateOrNull(),
                page = 0,
                pageSize = PAGE_SIZE,
                sortField = state.sortOption.toRepositorySortField(),
                ascending = state.sortOption.isAscending(),
                searchQuery = state.searchQuery,
                status = state.selectedStatusFilter.toSaleStatusOrNull()
            ).collect { result ->
                _listState.value = when (result) {
                    is Result.Loading -> _listState.value.copy(isLoading = true, error = null)
                    is Result.Success -> {
                        val cards = result.data.map {
                            SaleTicketCardUi(
                                id = it.id,
                                code = it.code,
                                saleDate = it.saleDate,
                                status = it.status,
                                totalSaleAmountVnd = it.totalSaleAmountVnd,
                                totalCostAmountVnd = it.totalCostAmountVnd,
                                profitVnd = it.profitVnd,
                                itemCount = it.itemCount
                            )
                        }
                        _listState.value.copy(
                            isLoading = false,
                            tickets = cards,
                            filteredTickets = cards,
                            hasMore = cards.size >= PAGE_SIZE,
                            currentPage = 0,
                            error = null
                        )
                    }
                    is Result.Error -> _listState.value.copy(isLoading = false, error = result.message)
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
            saleRepository.getTicketCards(
                startDate = state.fromDate.toApiDateOrNull(),
                endDate = state.toDate.toApiDateOrNull(),
                page = nextPage,
                pageSize = PAGE_SIZE,
                sortField = state.sortOption.toRepositorySortField(),
                ascending = state.sortOption.isAscending(),
                searchQuery = state.searchQuery,
                status = state.selectedStatusFilter.toSaleStatusOrNull()
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {}
                    is Result.Success -> {
                        val newCards = result.data.map {
                            SaleTicketCardUi(
                                id = it.id,
                                code = it.code,
                                saleDate = it.saleDate,
                                status = it.status,
                                totalSaleAmountVnd = it.totalSaleAmountVnd,
                                totalCostAmountVnd = it.totalCostAmountVnd,
                                profitVnd = it.profitVnd,
                                itemCount = it.itemCount
                            )
                        }
                        val allCards = state.tickets + newCards
                        _listState.value = _listState.value.copy(
                            isLoadingMore = false,
                            tickets = allCards,
                            filteredTickets = allCards,
                            hasMore = newCards.size >= PAGE_SIZE,
                            currentPage = nextPage
                        )
                    }
                    is Result.Error -> _listState.value = _listState.value.copy(
                        isLoadingMore = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun setStatusFilter(filter: SaleStatusFilter) {
        _listState.value = _listState.value.copy(selectedStatusFilter = filter)
        loadTickets()
    }

    fun setSearchQuery(query: String) {
        _listState.value = _listState.value.copy(searchQuery = query)
        loadTickets()
    }

    fun setSortOption(option: SaleSortOption) {
        _listState.value = _listState.value.copy(sortOption = option)
        loadTickets()
    }

    fun setFromDate(date: String) {
        _listState.value = _listState.value.copy(fromDate = date)
        loadTickets()
    }

    fun setToDate(date: String) {
        _listState.value = _listState.value.copy(toDate = date)
        loadTickets()
    }

    fun clearDateFilters() {
        _listState.value = _listState.value.copy(fromDate = "", toDate = "")
        loadTickets()
    }

    fun clearError() {
        _listState.value = _listState.value.copy(error = null)
    }

    fun loadTicketDetail(ticketId: String) {
        viewModelScope.launch {
            _detailState.value = SaleDetailUiState(isLoading = true)
            when (val ticketResult = saleRepository.getTicketById(ticketId)) {
                is Result.Success -> {
                    val t = ticketResult.data
                    val ticket = SaleTicketUi(
                        id = t.id,
                        code = t.code,
                        saleDate = t.saleDate,
                        status = t.status,
                        cancelledAt = t.cancelledAt,
                        cancelReason = t.cancelReason
                    )
                    saleRepository.getItemsByTicketId(ticketId).collect { itemsResult ->
                        _detailState.value = when (itemsResult) {
                            is Result.Loading -> SaleDetailUiState(ticket = ticket, isLoading = true)
                            is Result.Success -> SaleDetailUiState(ticket = ticket, items = itemsResult.data)
                            is Result.Error -> SaleDetailUiState(ticket = ticket, error = itemsResult.message)
                        }
                    }
                }
                is Result.Error -> _detailState.value = SaleDetailUiState(isLoading = false, error = ticketResult.message)
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
            when (val result = saleRepository.cancelTicket(ticketId, reason)) {
                is Result.Success -> {
                    _detailState.value = _detailState.value.copy(showCancelConfirm = false, isLoading = false)
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

    fun initForm() {
        viewModelScope.launch {
            try {
                lookupMutex.withLock {
                    val productsResult = productRepository.getAll(activeOnly = true, pageSize = 1000).awaitResult()
                    val products = (productsResult as? Result.Success)?.data ?: emptyList()
                    _formState.value = SaleFormUiState(
                        isInitialized = true,
                        saleDate = LocalDate.now().format(SALE_DATE_FORMATTER),
                        availableProducts = products
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "initForm failed - ${e.message}", e)
                _formState.value = SaleFormUiState(
                    isInitialized = true,
                    saleDate = LocalDate.now().format(SALE_DATE_FORMATTER),
                    error = e.message
                )
            }
        }
    }

    fun addFormItemWithDetails(
        product: ProductWithDetails,
        lot: AvailableLot,
        quantity: String,
        price: String
    ) {
        val currentItems = _formState.value.items
        val newId = if (currentItems.isEmpty()) 0 else currentItems.maxOf { it.id } + 1
        _formState.value = _formState.value.copy(
            items = currentItems + SaleItemDraftUi(
                id = newId,
                productId = product.product.id,
                productName = product.product.name,
                unitId = product.product.unitId,
                unitName = product.unitName,
                purchaseItemId = lot.purchaseItemId,
                lotCode = lot.lotCode,
                expiryDate = lot.expiryDate,
                quantityRemaining = lot.quantityRemaining,
                purchasePriceVnd = lot.purchasePriceVnd,
                quantity = quantity,
                salePrice = price
            )
        )
    }

    fun updateFormItemWithDetails(
        itemId: Int,
        product: ProductWithDetails,
        lot: AvailableLot,
        quantity: String,
        price: String
    ) {
        _formState.value = _formState.value.copy(
            items = _formState.value.items.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        productId = product.product.id,
                        productName = product.product.name,
                        unitId = product.product.unitId,
                        unitName = product.unitName,
                        purchaseItemId = lot.purchaseItemId,
                        lotCode = lot.lotCode,
                        expiryDate = lot.expiryDate,
                        quantityRemaining = lot.quantityRemaining,
                        purchasePriceVnd = lot.purchasePriceVnd,
                        quantity = quantity,
                        salePrice = price,
                        productError = null,
                        lotError = null,
                        quantityError = null,
                        priceError = null
                    )
                } else {
                    item
                }
            }
        )
    }

    fun removeFormItem(itemId: Int) {
        _formState.value = _formState.value.copy(items = _formState.value.items.filter { it.id != itemId })
    }

    fun updateSaleDate(date: String) {
        _formState.value = _formState.value.copy(saleDate = date, saleDateError = validateSaleDate(date))
    }

    fun validateSaleDateForSave(): Boolean {
        val error = validateSaleDate(_formState.value.saleDate)
        _formState.value = _formState.value.copy(saleDateError = error)
        return error == null
    }

    fun saveTicket(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.isSaving) return

        val saleDateError = validateSaleDate(state.saleDate)
        if (saleDateError != null) {
            _formState.value = state.copy(saleDateError = saleDateError)
            return
        }
        if (state.items.isEmpty()) {
            _formState.value = state.copy(error = "Phải có ít nhất 1 sản phẩm bán")
            return
        }

        var hasErrors = false
        val validatedItems = state.items.map { item ->
            var updated = item
            if (item.productId.isBlank()) {
                updated = updated.copy(productError = "Chọn sản phẩm")
                hasErrors = true
            }
            if (item.purchaseItemId.isBlank()) {
                updated = updated.copy(lotError = "Chọn lô còn hàng")
                hasErrors = true
            }
            val qty = item.quantity.toIntOrNull()
            if (qty == null || qty <= 0) {
                updated = updated.copy(quantityError = "Số lượng phải lớn hơn 0")
                hasErrors = true
            } else if (qty > item.quantityRemaining) {
                updated = updated.copy(quantityError = "Số lượng bán không được lớn hơn tồn kho")
                hasErrors = true
            }
            val price = item.salePrice.toLongOrNull()
            if (price == null || price < 0) {
                updated = updated.copy(priceError = "Giá bán không hợp lệ")
                hasErrors = true
            }
            updated
        }
        val soldByLot = state.items
            .filter { it.purchaseItemId.isNotBlank() }
            .groupBy { it.purchaseItemId }
            .mapValues { (_, items) -> items.sumOf { it.quantity.toIntOrNull() ?: 0 } }
        val overSoldLots = state.items
            .filter { it.purchaseItemId.isNotBlank() }
            .filter { item -> (soldByLot[item.purchaseItemId] ?: 0) > item.quantityRemaining }
            .map { it.purchaseItemId }
            .toSet()
        val validatedWithLotTotals = validatedItems.map { item ->
            if (item.purchaseItemId in overSoldLots) {
                hasErrors = true
                item.copy(quantityError = "Tổng số lượng bán trong phiếu vượt tồn lô")
            } else {
                item
            }
        }
        if (hasErrors) {
            _formState.value = state.copy(items = validatedWithLotTotals)
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSaving = true, error = null)
            val apiDate = state.saleDate.toApiDateOrNull() ?: state.saleDate
            val drafts = state.items.map {
                SaleItemDraft(
                    productId = it.productId,
                    purchaseItemId = it.purchaseItemId,
                    unitId = it.unitId,
                    quantitySold = it.quantity.toInt(),
                    salePriceVnd = it.salePrice.toLong()
                )
            }
            when (val result = saleRepository.createTicket(apiDate, drafts)) {
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

    fun clearFormError() {
        _formState.value = _formState.value.copy(error = null)
    }

    suspend fun fetchAvailableLotsForProduct(productId: String): Result<List<AvailableLot>> {
        return saleRepository.getAvailableLotsForProduct(productId).awaitResult()
    }

    private fun validateSaleDate(date: String): String? {
        if (date.isBlank()) return "Ngày bán hàng là bắt buộc"
        return try {
            val saleDate = LocalDate.parse(date, SALE_DATE_FORMATTER)
            if (saleDate.isAfter(LocalDate.now())) "Ngày bán hàng không được lớn hơn ngày hiện tại" else null
        } catch (_: DateTimeParseException) {
            "Ngày bán hàng không hợp lệ"
        }
    }

    private fun String.toApiDateOrNull(): String? {
        if (isBlank()) return null
        return try {
            val input = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val output = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            output.format(input.parse(this)!!)
        } catch (e: Exception) {
            null
        }
    }

    private fun SaleSortOption.toRepositorySortField(): SaleTicketSortField {
        return when (this) {
            SaleSortOption.DATE_NEWEST,
            SaleSortOption.DATE_OLDEST -> SaleTicketSortField.SALE_DATE
            SaleSortOption.AMOUNT_HIGHEST,
            SaleSortOption.AMOUNT_LOWEST -> SaleTicketSortField.TOTAL_AMOUNT
        }
    }

    private fun SaleSortOption.isAscending(): Boolean {
        return when (this) {
            SaleSortOption.DATE_OLDEST,
            SaleSortOption.AMOUNT_LOWEST -> true
            SaleSortOption.DATE_NEWEST,
            SaleSortOption.AMOUNT_HIGHEST -> false
        }
    }

    private fun SaleStatusFilter.toSaleStatusOrNull(): SaleTicketStatus? {
        return when (this) {
            SaleStatusFilter.ALL -> null
            SaleStatusFilter.ACTIVE -> SaleTicketStatus.ACTIVE
            SaleStatusFilter.CANCELLED -> SaleTicketStatus.CANCELLED
        }
    }
}
