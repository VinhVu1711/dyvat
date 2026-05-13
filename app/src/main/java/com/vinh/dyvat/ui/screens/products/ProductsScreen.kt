package com.vinh.dyvat.ui.screens.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.data.model.Category
import com.vinh.dyvat.data.model.ProductStatus
import com.vinh.dyvat.data.model.ProductWithDetails
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.ui.components.ConfirmDialog
import com.vinh.dyvat.ui.components.EmptyState
import com.vinh.dyvat.ui.components.ErrorState
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.components.StatusBadge
import com.vinh.dyvat.ui.components.StatusType
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToImport: () -> Unit,
    showBackButton: Boolean = true,
    refreshSignal: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCategorySheet by remember { mutableStateOf(false) }
    var showSupplierSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val hasAnyFilter = uiState.searchQuery.isNotBlank() ||
        uiState.selectedCategoryId != null ||
        uiState.selectedSupplierId != null

    LaunchedEffect(refreshSignal) {
        if (refreshSignal) {
            viewModel.loadProducts()
            onRefreshHandled()
        }
    }

    LaunchedEffect(uiState.filteredProducts, uiState.currentPage) {
        if (uiState.currentPage == 0 && uiState.filteredProducts.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "QUẢN LÝ SẢN PHẨM KINH DOANH",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = TextWhite
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToImport) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import sản phẩm",
                            tint = SpotifyGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NearBlack)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = SpotifyGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm sản phẩm",
                    tint = NearBlack
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NearBlack)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SearchTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = "Tìm kiếm sản phẩm..."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterDropdownButton(
                        label = "Lọc theo loại sản phẩm",
                        selectedLabel = uiState.categories.find { it.id == uiState.selectedCategoryId }?.name,
                        isActive = uiState.selectedCategoryId != null,
                        onClick = { showCategorySheet = true },
                        modifier = Modifier.weight(1f)
                    )
                    FilterDropdownButton(
                        label = "Lọc theo nhà cung cấp",
                        selectedLabel = uiState.suppliers.find { it.id == uiState.selectedSupplierId }?.name,
                        isActive = uiState.selectedSupplierId != null,
                        onClick = { showSupplierSheet = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box {
                    SortDropdownButton(
                        currentSort = uiState.sortOption,
                        onClick = { showSortMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(DarkCard)
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (option == uiState.sortOption) SpotifyGreen else TextWhite
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hiển thị sản phẩm ngừng kinh doanh",
                        color = TextSilver,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = uiState.showInactive,
                        onCheckedChange = { viewModel.setShowInactive(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SpotifyGreen,
                            checkedTrackColor = SpotifyGreen.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextSilver,
                            uncheckedTrackColor = MidDark
                        )
                    )
                }

                if (uiState.selectedCategoryId != null || uiState.selectedSupplierId != null) {
                    ActiveFiltersRow(
                        categories = uiState.categories,
                        suppliers = uiState.suppliers,
                        selectedCategoryId = uiState.selectedCategoryId,
                        selectedSupplierId = uiState.selectedSupplierId,
                        onClearCategory = { viewModel.setCategoryFilter(null) },
                        onClearSupplier = { viewModel.setSupplierFilter(null) }
                    )
                }
            }

            Text(
                text = "Danh sách sản phẩm",
                color = TextWhite,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NearBlack)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.loadProducts() },
                state = pullToRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 88.dp
                    )
                ) {
                    when {
                        uiState.isLoading -> {
                            item {
                                LoadingIndicator()
                            }
                        }
                        uiState.error != null && uiState.products.isEmpty() -> {
                            item {
                                ErrorState(
                                    message = uiState.error ?: "",
                                    onRetry = { viewModel.loadProducts() }
                                )
                            }
                        }
                        uiState.filteredProducts.isEmpty() -> {
                            item {
                                EmptyState(
                                    icon = Icons.Default.Inventory2,
                                    title = if (hasAnyFilter) {
                                        "Không tìm thấy sản phẩm"
                                    } else {
                                        "Chưa có sản phẩm nào"
                                    },
                                    subtitle = if (hasAnyFilter) {
                                        "Thử thay đổi từ khóa hoặc bộ lọc"
                                    } else {
                                        "Nhấn + để thêm sản phẩm mới"
                                    },
                                    actionText = if (!hasAnyFilter) "Thêm sản phẩm" else null,
                                    onAction = if (!hasAnyFilter) onNavigateToAdd else null
                                )
                            }
                        }
                        else -> {
                            items(
                                items = uiState.filteredProducts,
                                key = { it.product.id }
                            ) { product ->
                                ProductCard(
                                    product = product,
                                    onClick = { onNavigateToDetail(product.product.id) },
                                    onResumeClick = { viewModel.requestResumeProduct(product.product.id) }
                                )
                            }

                            if (uiState.hasMore) {
                                item {
                                    TextButton(
                                        onClick = { viewModel.loadMore() },
                                        enabled = !uiState.isLoadingMore,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (uiState.isLoadingMore) "Đang tải..." else "Tải thêm sản phẩm",
                                            color = SpotifyGreen,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategorySheet) {
        FilterSheet(
            title = "Lọc theo loại sản phẩm",
            items = uiState.categories.map { it.id to it.name },
            selectedId = uiState.selectedCategoryId,
            onSelect = {
                viewModel.setCategoryFilter(it)
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showSupplierSheet) {
        FilterSheet(
            title = "Lọc theo nhà cung cấp",
            items = uiState.suppliers.map { it.id to it.name },
            selectedId = uiState.selectedSupplierId,
            onSelect = {
                viewModel.setSupplierFilter(it)
                showSupplierSheet = false
            },
            onDismiss = { showSupplierSheet = false }
        )
    }

    if (uiState.showDeleteConfirm && uiState.productToDelete != null) {
        ConfirmDialog(
            title = "Xác nhận xóa",
            message = "Bạn có chắc muốn xóa sản phẩm \"${uiState.productToDelete!!.product.name}\"? Hành động này không thể hoàn tác.",
            confirmText = "Xóa",
            isDestructive = true,
            onDismiss = { viewModel.hideDeleteConfirm() },
            onConfirm = {
                viewModel.deleteProduct(uiState.productToDelete!!.product.id)
            }
        )
    }

    if (uiState.showResumeConfirm && uiState.resumeProductId != null) {
        val productName = uiState.products.find { it.product.id == uiState.resumeProductId }?.product?.name
            ?: "sản phẩm này"
        ConfirmDialog(
            title = "Kích hoạt sản phẩm",
            message = "Sản phẩm \"$productName\" sẽ được hiển thị trở lại trong danh sách kinh doanh.",
            confirmText = "Kích hoạt",
            onDismiss = { viewModel.hideResumeConfirm() },
            onConfirm = { viewModel.confirmResumeProduct() }
        )
    }
}

@Composable
private fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = TextSilver.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSilver
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedBorderColor = SpotifyGreen,
            unfocusedBorderColor = MidDark,
            focusedContainerColor = DarkCard,
            unfocusedContainerColor = DarkCard,
            cursorColor = SpotifyGreen
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun FilterDropdownButton(
    label: String,
    selectedLabel: String?,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) SpotifyGreen.copy(alpha = 0.15f) else DarkCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = if (isActive) SpotifyGreen else TextSilver,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedLabel ?: label,
                color = if (isActive) SpotifyGreen else TextSilver,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = if (isActive) SpotifyGreen else TextSilver,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SortDropdownButton(
    currentSort: SortOption,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sắp xếp: ${currentSort.label}",
            color = TextSilver,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "v",
            color = TextSilver,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ActiveFiltersRow(
    categories: List<Category>,
    suppliers: List<Supplier>,
    selectedCategoryId: String?,
    selectedSupplierId: String?,
    onClearCategory: () -> Unit,
    onClearSupplier: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        selectedCategoryId?.let { id ->
            val category = categories.find { it.id == id }
            FilterChip(
                selected = true,
                onClick = onClearCategory,
                label = { Text(category?.name ?: "Loại sản phẩm") },
                trailingIcon = {
                    Text(
                        text = "x",
                        color = TextWhite,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SpotifyGreen.copy(alpha = 0.2f),
                    selectedLabelColor = SpotifyGreen
                )
            )
        }

        selectedSupplierId?.let { id ->
            val supplier = suppliers.find { it.id == id }
            FilterChip(
                selected = true,
                onClick = onClearSupplier,
                label = { Text(supplier?.name ?: "Nhà cung cấp") },
                trailingIcon = {
                    Text(
                        text = "x",
                        color = TextWhite,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SpotifyGreen.copy(alpha = 0.2f),
                    selectedLabelColor = SpotifyGreen
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    title: String,
    items: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                color = TextWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedId == null) SpotifyGreen.copy(alpha = 0.15f) else MidDark)
                    .clickable { onSelect(null) }
                    .padding(12.dp)
            ) {
                Text(
                    text = "Tất cả",
                    color = if (selectedId == null) SpotifyGreen else TextWhite,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { (id, name) ->
                    FilterChip(
                        selected = id == selectedId,
                        onClick = { onSelect(id) },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpotifyGreen.copy(alpha = 0.2f),
                            selectedLabelColor = SpotifyGreen,
                            containerColor = MidDark,
                            labelColor = TextSilver
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Đóng", color = TextSilver)
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: ProductWithDetails,
    onClick: () -> Unit,
    onResumeClick: (() -> Unit)? = null
) {
    val isDiscontinued = product.product.status == ProductStatus.DISCONTINUED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isDiscontinued) DarkCard.copy(alpha = 0.6f) else DarkSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mã sản phẩm: ${product.product.code.ifEmpty { "-" }}",
                        color = TextSilver,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tên sản phẩm: ${product.product.name}",
                        color = if (isDiscontinued) TextSilver else TextWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isDiscontinued) {
                    StatusBadge(
                        label = "Ngừng kinh doanh",
                        type = StatusType.WARNING
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Loại sản phẩm: ${product.categoryName.ifEmpty { "-" }}",
                color = TextSilver,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Nhà cung cấp: ${product.supplierName.ifEmpty { "-" }}",
                color = TextSilver,
                style = MaterialTheme.typography.bodyMedium
            )

            if (isDiscontinued && onResumeClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onResumeClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kinh doanh lại",
                        color = SpotifyGreen,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
