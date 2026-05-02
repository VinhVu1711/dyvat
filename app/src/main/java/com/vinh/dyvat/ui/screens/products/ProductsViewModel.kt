package com.vinh.dyvat.ui.screens.products

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.Category
import com.vinh.dyvat.data.model.Product
import com.vinh.dyvat.data.model.ProductStatus
import com.vinh.dyvat.data.model.ProductWithDetails
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.data.model.UnitModel
import com.vinh.dyvat.data.repository.CategoryRepository
import com.vinh.dyvat.data.repository.ProductRepository
import com.vinh.dyvat.data.repository.ProductSortField
import com.vinh.dyvat.data.repository.SupplierRepository
import com.vinh.dyvat.data.repository.UnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

private const val TAG = "ProductsViewModel"

private data class ProductLookups(
    val categories: List<Category>,
    val units: List<UnitModel>,
    val suppliers: List<Supplier>
)

data class ProductsUiState(
    val products: List<ProductWithDetails> = emptyList(),
    val filteredProducts: List<ProductWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val selectedSupplierId: String? = null,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val showInactive: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val productToDelete: ProductWithDetails? = null,
    val showResumeConfirm: Boolean = false,
    val resumeProductId: String? = null,
    val categories: List<Category> = emptyList(),
    val units: List<UnitModel> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 0
)

enum class SortOption(val label: String) {
    NAME_ASC("Tên sản phẩm A-Z"),
    NAME_DESC("Tên sản phẩm Z-A"),
    CODE_ASC("Mã sản phẩm A-Z"),
    CODE_DESC("Mã sản phẩm Z-A"),
    PRICE_ASC("Giá tăng dần"),
    PRICE_DESC("Giá giảm dần"),
    NEWEST("Mới nhất"),
    OLDEST("Cũ nhất")
}

data class ProductDetailUiState(
    val product: ProductWithDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDiscontinueConfirm: Boolean = false,
    val showReactivateConfirm: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showCannotDeleteDialog: Boolean = false,
    val cannotDeleteMessage: String = "",
    val navigateBack: Boolean = false,
    val actionError: String? = null
)

data class ProductFormUiState(
    val name: String = "",
    val categoryId: String = "",
    val unitId: String = "",
    val supplierId: String = "",
    val purchasePrice: String = "",
    val salePrice: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val categoryError: String? = null,
    val unitError: String? = null,
    val supplierError: String? = null,
    val purchasePriceError: String? = null,
    val salePriceError: String? = null,
    val categories: List<Category> = emptyList(),
    val units: List<UnitModel> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val isEditMode: Boolean = false,
    val editingProductId: String? = null,
    val showErrorDialog: Boolean = false,
    val errorDialogTitle: String = "",
    val errorDialogMessage: String = "",
    val showSuccessDialog: Boolean = false,
    val successDialogMessage: String = "",
    val addMultiple: Boolean = false
)

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val unitRepository: UnitRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    private val _detailUiState = MutableStateFlow(ProductDetailUiState())
    val detailUiState: StateFlow<ProductDetailUiState> = _detailUiState.asStateFlow()

    private val _formUiState = MutableStateFlow(ProductFormUiState())
    val formUiState: StateFlow<ProductFormUiState> = _formUiState.asStateFlow()

    private val lookupMutex = Mutex()
    private var searchDebounceJob: Job? = null

    private val searchDebounceMs = 1250L

    init {
        loadProducts()
        loadLookups()
    }

    private fun loadLookups() {
        viewModelScope.launch {
            lookupMutex.withLock {
                try {
                    val lookups = fetchLookups()

                    _uiState.value = _uiState.value.copy(
                        categories = lookups.categories,
                        units = lookups.units,
                        suppliers = lookups.suppliers
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "loadLookups: failed - ${e.message}", e)
                }
            }
        }
    }

    private suspend fun fetchLookups(): ProductLookups {
        Log.d(TAG, "fetchLookups: fetching categories, units, suppliers")
        val categoriesResult = categoryRepository.getAll().awaitResult()
        val unitsResult = unitRepository.getAll().awaitResult()
        val suppliersResult = supplierRepository.getAll().awaitResult()

        val categoriesData = (categoriesResult as? Result.Success)?.data ?: emptyList()
        val unitsData = (unitsResult as? Result.Success)?.data ?: emptyList()
        val suppliersData = (suppliersResult as? Result.Success)?.data ?: emptyList()

        Log.d(TAG, "fetchLookups: loaded - categories=${categoriesData.size}, units=${unitsData.size}, suppliers=${suppliersData.size}")

        return ProductLookups(
            categories = categoriesData,
            units = unitsData,
            suppliers = suppliersData
        )
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

    companion object {
        private const val PAGE_SIZE = 5
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isLoadingMore = false,
                error = null,
                currentPage = 0
            )
            productRepository.getAll(
                activeOnly = !_uiState.value.showInactive,
                page = 0,
                pageSize = PAGE_SIZE,
                sortField = _uiState.value.sortOption.toRepositorySortField(),
                ascending = _uiState.value.sortOption.isAscending(),
                categoryId = _uiState.value.selectedCategoryId,
                supplierId = _uiState.value.selectedSupplierId
            )
                .collect { result ->
                    _uiState.value = when (result) {
                        is Result.Loading -> _uiState.value.copy(isLoading = true, error = null)
                        is Result.Success -> {
                            val products = result.data
                            _uiState.value.copy(
                                isLoading = false,
                                products = products,
                                filteredProducts = applyFilters(products),
                                hasMore = products.size >= PAGE_SIZE,
                                currentPage = 0,
                                error = null
                            )
                        }
                        is Result.Error -> _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val nextPage = state.currentPage + 1
            productRepository.getAll(
                activeOnly = !_uiState.value.showInactive,
                page = nextPage,
                pageSize = PAGE_SIZE,
                sortField = state.sortOption.toRepositorySortField(),
                ascending = state.sortOption.isAscending(),
                categoryId = state.selectedCategoryId,
                supplierId = state.selectedSupplierId
            )
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {}
                        is Result.Success -> {
                            val newProducts = result.data
                            val allProducts = state.products + newProducts
                            _uiState.value = _uiState.value.copy(
                                isLoadingMore = false,
                                products = allProducts,
                                filteredProducts = applyFilters(allProducts),
                                hasMore = newProducts.size >= PAGE_SIZE,
                                currentPage = nextPage
                            )
                        }
                        is Result.Error -> _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            error = result.message
                        )
                    }
                }
        }
    }

    private suspend fun loadProductsAndWait() {
        Log.d(TAG, "loadProductsAndWait: start")
        productRepository.getAll(
            activeOnly = !_uiState.value.showInactive,
            page = 0,
            pageSize = PAGE_SIZE,
            sortField = _uiState.value.sortOption.toRepositorySortField(),
            ascending = _uiState.value.sortOption.isAscending(),
            categoryId = _uiState.value.selectedCategoryId,
            supplierId = _uiState.value.selectedSupplierId
        ).collect { result ->
            _uiState.value = when (result) {
                is Result.Loading -> _uiState.value.copy(isLoading = true, error = null)
                is Result.Success -> {
                    val products = result.data
                    Log.d(TAG, "loadProductsAndWait: loaded ${products.size} products")
                    _uiState.value.copy(
                        isLoading = false,
                        products = products,
                        filteredProducts = applyFilters(products),
                        hasMore = products.size >= PAGE_SIZE,
                        currentPage = 0,
                        error = null
                    )
                }
                is Result.Error -> {
                    Log.e(TAG, "loadProductsAndWait: error - ${result.message}")
                    _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(searchDebounceMs)
            if (query.isNotBlank()) {
                searchProducts(query)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, filteredProducts = emptyList())
                loadProducts()
            }
        }
    }

    private fun searchProducts(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            productRepository.search(query, activeOnly = !_uiState.value.showInactive).collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> _uiState.value.copy(isLoading = true)
                    is Result.Success -> _uiState.value.copy(
                        isLoading = false,
                        filteredProducts = applyFilters(result.data),
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

    fun setCategoryFilter(categoryId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId
        )
        loadProducts()
    }

    fun setSupplierFilter(supplierId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedSupplierId = supplierId
        )
        loadProducts()
    }

    fun setShowInactive(show: Boolean) {
        _uiState.value = _uiState.value.copy(showInactive = show)
        loadProducts()
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            selectedCategoryId = null,
            selectedSupplierId = null,
            sortOption = SortOption.NAME_ASC,
            filteredProducts = applySorting(_uiState.value.products)
        )
        loadProducts()
    }

    private fun applyFilters(products: List<ProductWithDetails>): List<ProductWithDetails> {
        val state = _uiState.value
        val filtered = products.filter { p ->
            val matchesSearch = state.searchQuery.isBlank() ||
                    p.product.name.contains(state.searchQuery, ignoreCase = true) ||
                    p.product.code.contains(state.searchQuery, ignoreCase = true)
            val matchesCategory = state.selectedCategoryId == null ||
                    p.product.categoryId == state.selectedCategoryId
            val matchesSupplier = state.selectedSupplierId == null ||
                    p.product.supplierId == state.selectedSupplierId
            matchesSearch && matchesCategory && matchesSupplier
        }
        return applySorting(filtered)
    }

    private fun applySorting(products: List<ProductWithDetails>): List<ProductWithDetails> {
        return when (_uiState.value.sortOption) {
            SortOption.NAME_ASC -> products.sortedBy { it.product.name.lowercase() }
            SortOption.NAME_DESC -> products.sortedByDescending { it.product.name.lowercase() }
            SortOption.CODE_ASC -> products.sortedBy { it.product.code.lowercase() }
            SortOption.CODE_DESC -> products.sortedByDescending { it.product.code.lowercase() }
            SortOption.PRICE_ASC -> products.sortedBy { it.product.defaultSalePriceVnd }
            SortOption.PRICE_DESC -> products.sortedByDescending { it.product.defaultSalePriceVnd }
            SortOption.NEWEST -> products.sortedByDescending { it.product.createdAt }
            SortOption.OLDEST -> products.sortedBy { it.product.createdAt }
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.value = _uiState.value.copy(
            sortOption = option,
            filteredProducts = applySorting(_uiState.value.filteredProducts)
        )
        loadProducts()
    }

    private fun SortOption.toRepositorySortField(): ProductSortField {
        return when (this) {
            SortOption.NAME_ASC,
            SortOption.NAME_DESC -> ProductSortField.NAME
            SortOption.CODE_ASC,
            SortOption.CODE_DESC -> ProductSortField.CODE
            SortOption.PRICE_ASC,
            SortOption.PRICE_DESC -> ProductSortField.SALE_PRICE
            SortOption.NEWEST,
            SortOption.OLDEST -> ProductSortField.CREATED_AT
        }
    }

    private fun SortOption.isAscending(): Boolean {
        return when (this) {
            SortOption.NAME_ASC,
            SortOption.CODE_ASC,
            SortOption.PRICE_ASC,
            SortOption.OLDEST -> true
            SortOption.NAME_DESC,
            SortOption.CODE_DESC,
            SortOption.PRICE_DESC,
            SortOption.NEWEST -> false
        }
    }

    fun showDeleteConfirm(product: ProductWithDetails) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            productToDelete = product
        )
    }

    fun hideDeleteConfirm() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = false,
            productToDelete = null
        )
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (productRepository.delete(id)) {
                is Result.Success<*> -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showDeleteConfirm = false,
                        productToDelete = null
                    )
                    loadProducts()
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Xoa san pham that bai"
                )
                is Result.Loading -> {}
            }
        }
    }

    fun requestDeleteProduct(id: String) {
        viewModelScope.launch {
            val relationships = productRepository.checkProductRelationships(id)
            val productName = _uiState.value.products.find { it.product.id == id }?.product?.name
                ?: _detailUiState.value.product?.product?.name
                ?: "sản phẩm này"

            if (relationships.hasPurchaseHistory || relationships.hasSaleHistory) {
                val messages = mutableListOf<String>()
                if (relationships.hasPurchaseHistory) messages.add("đã từng nhập hàng")
                if (relationships.hasSaleHistory) messages.add("đã từng bán hàng")

                _detailUiState.value = _detailUiState.value.copy(
                    showCannotDeleteDialog = true,
                    cannotDeleteMessage = "Sản phẩm \"$productName\" $messages nên không thể xóa. " +
                        "Vui lòng ngừng kinh doanh sản phẩm để bảo toàn dữ liệu."
                )
            } else {
                _detailUiState.value = _detailUiState.value.copy(showDeleteConfirm = true)
            }
        }
    }

    fun hideCannotDeleteDialog() {
        _detailUiState.value = _detailUiState.value.copy(
            showCannotDeleteDialog = false,
            cannotDeleteMessage = ""
        )
    }

    fun performDeleteProduct(id: String) {
        viewModelScope.launch {
            _detailUiState.value = _detailUiState.value.copy(isLoading = true, showDeleteConfirm = false)
            when (productRepository.delete(id)) {
                is Result.Success -> {
                    loadProductsAndWait()
                    _detailUiState.value = _detailUiState.value.copy(
                        isLoading = false,
                        showDeleteConfirm = false,
                        navigateBack = true
                    )
                }
                is Result.Error -> {
                    _detailUiState.value = _detailUiState.value.copy(
                        isLoading = false,
                        actionError = "Xóa sản phẩm thất bại"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun discontinueProduct(id: String) {
        viewModelScope.launch {
            _detailUiState.value = _detailUiState.value.copy(isLoading = true, actionError = null)
            when (val result = productRepository.updateStatus(id, ProductStatus.DISCONTINUED)) {
                is Result.Success -> {
                    _detailUiState.value = _detailUiState.value.copy(
                        isLoading = false,
                        showDiscontinueConfirm = false,
                        product = _detailUiState.value.product?.copy(
                            product = result.data
                        )
                    )
                    loadProducts()
                }
                is Result.Error -> _detailUiState.value = _detailUiState.value.copy(
                    isLoading = false,
                    actionError = result.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun reactivateProduct(id: String) {
        viewModelScope.launch {
            _detailUiState.value = _detailUiState.value.copy(isLoading = true, actionError = null)
            when (val result = productRepository.updateStatus(id, ProductStatus.ACTIVE)) {
                is Result.Success -> {
                    _detailUiState.value = _detailUiState.value.copy(
                        isLoading = false,
                        showReactivateConfirm = false,
                        product = _detailUiState.value.product?.copy(
                            product = result.data
                        )
                    )
                    loadProducts()
                }
                is Result.Error -> _detailUiState.value = _detailUiState.value.copy(
                    isLoading = false,
                    actionError = result.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun requestResumeProduct(id: String) {
        _uiState.value = _uiState.value.copy(showResumeConfirm = true, resumeProductId = id)
    }

    fun hideResumeConfirm() {
        _uiState.value = _uiState.value.copy(showResumeConfirm = false, resumeProductId = null)
    }

    fun confirmResumeProduct() {
        val id = _uiState.value.resumeProductId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, showResumeConfirm = false, resumeProductId = null)
            when (productRepository.updateStatus(id, ProductStatus.ACTIVE)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadProducts()
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Khoi phuc san pham that bai"
                )
                is Result.Loading -> {}
            }
        }
    }

    fun loadProductDetail(id: String) {
        viewModelScope.launch {
            _detailUiState.value = ProductDetailUiState(isLoading = true)
            when (val result = productRepository.getById(id)) {
                is Result.Success -> _detailUiState.value = ProductDetailUiState(
                    product = result.data,
                    isLoading = false
                )
                is Result.Error -> _detailUiState.value = ProductDetailUiState(
                    isLoading = false,
                    error = result.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun showDiscontinueConfirm() {
        _detailUiState.value = _detailUiState.value.copy(showDiscontinueConfirm = true)
    }

    fun hideDiscontinueConfirm() {
        _detailUiState.value = _detailUiState.value.copy(showDiscontinueConfirm = false)
    }

    fun showReactivateConfirm() {
        _detailUiState.value = _detailUiState.value.copy(showReactivateConfirm = true)
    }

    fun hideReactivateConfirm() {
        _detailUiState.value = _detailUiState.value.copy(showReactivateConfirm = false)
    }

    fun hideDetailDeleteConfirm() {
        _detailUiState.value = _detailUiState.value.copy(showDeleteConfirm = false)
    }

    fun clearDetailError() {
        _detailUiState.value = _detailUiState.value.copy(actionError = null)
    }

    // --- Form ---

    fun initFormForAdd() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "initFormForAdd: waiting for loadLookups to complete")
                lookupMutex.withLock {
                    val lookups = if (
                        _uiState.value.categories.isEmpty() &&
                        _uiState.value.units.isEmpty() &&
                        _uiState.value.suppliers.isEmpty()
                    ) {
                        fetchLookups().also {
                            _uiState.value = _uiState.value.copy(
                                categories = it.categories,
                                units = it.units,
                                suppliers = it.suppliers
                            )
                        }
                    } else {
                        ProductLookups(
                            categories = _uiState.value.categories,
                            units = _uiState.value.units,
                            suppliers = _uiState.value.suppliers
                        )
                    }

                    Log.d(TAG, "initFormForAdd: lookup data ready")
                    _formUiState.value = ProductFormUiState(
                        categories = lookups.categories,
                        units = lookups.units,
                        suppliers = lookups.suppliers,
                        isEditMode = false
                    )
                    Log.d(TAG, "initFormForAdd: loaded - categories=${lookups.categories.size}, units=${lookups.units.size}, suppliers=${lookups.suppliers.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "initFormForAdd: failed - ${e.message}", e)
                _formUiState.value = ProductFormUiState(isEditMode = false)
            }
        }
    }

    fun initFormForEdit(productId: String) {
        viewModelScope.launch {
            _formUiState.value = _formUiState.value.copy(isLoading = true)

            when (val result = productRepository.getById(productId)) {
                is Result.Success -> {
                    val p = result.data.product
                    lookupMutex.withLock {
                        val lookups = if (
                            _uiState.value.categories.isEmpty() &&
                            _uiState.value.units.isEmpty() &&
                            _uiState.value.suppliers.isEmpty()
                        ) {
                            fetchLookups().also {
                                _uiState.value = _uiState.value.copy(
                                    categories = it.categories,
                                    units = it.units,
                                    suppliers = it.suppliers
                                )
                            }
                        } else {
                            ProductLookups(
                                categories = _uiState.value.categories,
                                units = _uiState.value.units,
                                suppliers = _uiState.value.suppliers
                            )
                        }

                        _formUiState.value = ProductFormUiState(
                            name = p.name,
                            categoryId = p.categoryId,
                            unitId = p.unitId,
                            supplierId = p.supplierId,
                            purchasePrice = p.defaultPurchasePriceVnd.toString(),
                            salePrice = p.defaultSalePriceVnd.toString(),
                            categories = lookups.categories,
                            units = lookups.units,
                            suppliers = lookups.suppliers,
                            isEditMode = true,
                            editingProductId = productId,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> _formUiState.value = _formUiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun updateFormName(value: String) {
        _formUiState.value = _formUiState.value.copy(
            name = value,
            nameError = null
        )
    }

    fun updateFormCategory(value: String) {
        _formUiState.value = _formUiState.value.copy(
            categoryId = value,
            categoryError = null
        )
    }

    fun updateFormUnit(value: String) {
        _formUiState.value = _formUiState.value.copy(
            unitId = value,
            unitError = null
        )
    }

    fun updateFormSupplier(value: String) {
        _formUiState.value = _formUiState.value.copy(
            supplierId = value,
            supplierError = null
        )
    }

    fun updateFormPurchasePrice(value: String) {
        val cleaned = value.filter { it.isDigit() }
        _formUiState.value = _formUiState.value.copy(
            purchasePrice = cleaned,
            purchasePriceError = null
        )
    }

    fun updateFormSalePrice(value: String) {
        val cleaned = value.filter { it.isDigit() }
        _formUiState.value = _formUiState.value.copy(
            salePrice = cleaned,
            salePriceError = null
        )
    }

    fun dismissErrorDialog() {
        _formUiState.value = _formUiState.value.copy(
            showErrorDialog = false,
            errorDialogTitle = "",
            errorDialogMessage = ""
        )
    }

    fun dismissSuccessDialog() {
        _formUiState.value = _formUiState.value.copy(showSuccessDialog = false)
    }

    fun updateFormAddMultiple(value: Boolean) {
        _formUiState.value = _formUiState.value.copy(addMultiple = value)
    }

    fun saveProduct(onSuccess: () -> Unit) {
        val state = _formUiState.value
        var hasError = false

        if (state.name.isBlank()) {
            _formUiState.value = _formUiState.value.copy(nameError = "Ten san pham khong duoc de trong")
            hasError = true
        }
        if (state.categoryId.isBlank()) {
            _formUiState.value = _formUiState.value.copy(categoryError = "Vui long chon loai san pham")
            hasError = true
        }
        if (state.unitId.isBlank()) {
            _formUiState.value = _formUiState.value.copy(unitError = "Vui long chon don vi tinh")
            hasError = true
        }
        if (state.supplierId.isBlank()) {
            _formUiState.value = _formUiState.value.copy(supplierError = "Vui long chon nha cung cap")
            hasError = true
        }
        if (state.purchasePrice.isBlank() || state.purchasePrice.toLongOrNull() == null || state.purchasePrice.toLong() < 0) {
            _formUiState.value = _formUiState.value.copy(purchasePriceError = "Gia nhap khong hop le")
            hasError = true
        }
        if (state.salePrice.isBlank() || state.salePrice.toLongOrNull() == null || state.salePrice.toLong() < 0) {
            _formUiState.value = _formUiState.value.copy(salePriceError = "Gia ban khong hop le")
            hasError = true
        }

        if (hasError) return

        val purchasePrice = state.purchasePrice.toLong()
        val salePrice = state.salePrice.toLong()

        Log.d(TAG, "saveProduct: name='${state.name}', purchasePrice=$purchasePrice, salePrice=$salePrice, isEditMode=${state.isEditMode}, editingId=${state.editingProductId}")
        Log.d(TAG, "saveProduct: total products in uiState=${_uiState.value.products.size}")

        if (salePrice < purchasePrice) {
            _formUiState.value = _formUiState.value.copy(
                showErrorDialog = true,
                errorDialogTitle = "Gia khong hop le",
                errorDialogMessage = "Gia ban phai lon hon hoac bang gia nhap. Vui long kiem tra lai gia ban."
            )
            return
        }

        val isDuplicate = _uiState.value.products.any {
            it.product.name.equals(state.name.trim(), ignoreCase = true) &&
            (state.isEditMode && state.editingProductId == it.product.id).not()
        }
        Log.d(TAG, "saveProduct: isDuplicate=$isDuplicate")

        if (isDuplicate) {
            _formUiState.value = _formUiState.value.copy(
                showErrorDialog = true,
                errorDialogTitle = "San pham da ton tai",
                errorDialogMessage = "San pham \"${state.name.trim()}\" da co trong he thong. Vui long nhap ten khac hoac chinh sua san pham cu."
            )
            return
        }

        viewModelScope.launch {
            _formUiState.value = _formUiState.value.copy(isLoading = true, error = null)

            val result = if (state.isEditMode && state.editingProductId != null) {
                productRepository.update(
                    id = state.editingProductId,
                    name = state.name,
                    categoryId = state.categoryId,
                    unitId = state.unitId,
                    supplierId = state.supplierId,
                    purchasePrice = purchasePrice,
                    salePrice = salePrice
                )
            } else {
                productRepository.insert(
                    name = state.name,
                    categoryId = state.categoryId,
                    unitId = state.unitId,
                    supplierId = state.supplierId,
                    purchasePrice = purchasePrice,
                    salePrice = salePrice
                )
            }

            Log.d(TAG, "saveProduct: result type = ${result::class.simpleName}")
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "saveProduct: SUCCESS - inserted product=${result.data}")
                    _formUiState.value = _formUiState.value.copy(isLoading = false)
                    loadProductsAndWait()
                    if (!state.addMultiple) {
                        _formUiState.value = _formUiState.value.copy(showSuccessDialog = true)
                    } else {
                        _formUiState.value = _formUiState.value.copy(
                            showSuccessDialog = true,
                            successDialogMessage = "Da them san pham \"${state.name.trim()}\"!"
                        )
                        resetForm()
                    }
                }
                is Result.Error -> {
                    _formUiState.value = _formUiState.value.copy(
                        isLoading = false,
                        showErrorDialog = true,
                        errorDialogTitle = "Lỗi khi lưu sản phẩm",
                        errorDialogMessage = "Không thể lưu sản phẩm. Vui long kiem tra lai thong tin va thu lai."
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearFormError() {
        _formUiState.value = _formUiState.value.copy(error = null)
    }

    fun resetForm() {
        _formUiState.value = ProductFormUiState(
            categories = _formUiState.value.categories,
            units = _formUiState.value.units,
            suppliers = _formUiState.value.suppliers,
            isEditMode = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
