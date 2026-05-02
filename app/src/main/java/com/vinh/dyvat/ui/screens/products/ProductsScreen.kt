package com.vinh.dyvat.ui.screens.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.vinh.dyvat.data.model.ProductWithDetails
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.ui.components.ConfirmDialog
import com.vinh.dyvat.ui.components.EmptyState
import com.vinh.dyvat.ui.components.ErrorState
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.LightBorder
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    @Suppress("UNUSED_PARAMETER") onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToDetail: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToAdd: () -> Unit,
    @Suppress("UNUSED_PARAMETER") showBackButton: Boolean = true,
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

    fun onRefresh() {
        viewModel.loadProducts()
    }

    Scaffold(
        containerColor = NearBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = SpotifyGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Them san pham",
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
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NearBlack)
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = "QUẢN LÝ SẢN PHẨM KINH DOANH",
                    color = TextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Sort button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sắp xếp: ",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box {
                    Text(
                        text = "${uiState.sortOption.label} ▼",
                        color = SpotifyGreen,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { showSortMenu = true }
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
            }

            // Toggle: show discontinued products
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hien thi san pham ngung kinh doanh",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodySmall
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

            Spacer(modifier = Modifier.height(8.dp))

            // Active filter chips
            if (uiState.selectedCategoryId != null || uiState.selectedSupplierId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ActiveFiltersRow(
                    categories = uiState.categories,
                    suppliers = uiState.suppliers,
                    selectedCategoryId = uiState.selectedCategoryId,
                    selectedSupplierId = uiState.selectedSupplierId,
                    onClearCategory = { viewModel.setCategoryFilter(null) },
                    onClearSupplier = { viewModel.setSupplierFilter(null) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            when {
                uiState.error != null && uiState.products.isEmpty() -> ErrorState(
                    message = uiState.error ?: "",
                    onRetry = { viewModel.loadProducts() }
                )
                uiState.isLoading -> LoadingIndicator()
                uiState.filteredProducts.isEmpty() -> EmptyState(
                    icon = Icons.Default.Inventory2,
                    title = if (uiState.searchQuery.isNotEmpty() || uiState.selectedCategoryId != null || uiState.selectedSupplierId != null) {
                        "Khong tim thay san pham"
                    } else {
                        "Chua co san pham nao"
                    },
                    subtitle = if (uiState.searchQuery.isEmpty() && uiState.selectedCategoryId == null && uiState.selectedSupplierId == null) {
                        "Nhan + de them san pham moi"
                    } else {
                        "Thu thay doi tu khoa hoac bo loc"
                    },
                    actionText = if (uiState.searchQuery.isEmpty() && uiState.selectedCategoryId == null && uiState.selectedSupplierId == null) {
                        "Them san pham"
                    } else null,
                    onAction = if (uiState.searchQuery.isEmpty() && uiState.selectedCategoryId == null && uiState.selectedSupplierId == null) {
                        { onNavigateToAdd() }
                    } else null
                )
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { onRefresh() },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                        ) {
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
                                            text = if (uiState.isLoadingMore) "Dang tai..." else "Tai them san pham",
                                            color = SpotifyGreen,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Category filter sheet
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

    // Supplier filter sheet
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

    // Delete confirm dialog
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

    // Resume confirm dialog
    if (uiState.showResumeConfirm && uiState.resumeProductId != null) {
        val productName = uiState.products.find { it.product.id == uiState.resumeProductId }?.product?.name ?: "sản phẩm này"
        ConfirmDialog(
            title = "Kích hoạt sản phẩm",
            message = "San pham \"$productName\" se duoc hien thi tro lai trong danh sach kinh doanh.",
            confirmText = "Kich hoat",
            onDismiss = { viewModel.hideResumeConfirm() },
            onConfirm = {
                viewModel.confirmResumeProduct()
            }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Tìm kiếm sản phẩm...",
                color = TextSilver
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Tìm kiếm",
                tint = TextSilver
            )
        },
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedContainerColor = MidDark,
            unfocusedContainerColor = MidDark,
            cursorColor = SpotifyGreen,
            focusedBorderColor = SpotifyGreen,
            unfocusedBorderColor = MidDark
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) SpotifyGreen.copy(alpha = 0.15f) else MidDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (selectedLabel != null) selectedLabel else label,
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
private fun ActiveFiltersRow(
    categories: List<Category>,
    suppliers: List<Supplier>,
    selectedCategoryId: String?,
    selectedSupplierId: String?,
    onClearCategory: () -> Unit,
    onClearSupplier: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        selectedCategoryId?.let { id ->
            val cat = categories.find { it.id == id }
            FilterChip(
                selected = true,
                onClick = onClearCategory,
                label = { Text(cat?.name ?: "Loại") },
                trailingIcon = {
                    Text(
                        text = "\u2715",
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
            val sup = suppliers.find { it.id == id }
            FilterChip(
                selected = true,
                onClick = onClearSupplier,
                label = { Text(sup?.name ?: "NCC") },
                trailingIcon = {
                    Text(
                        text = "\u2715",
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
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    androidx.compose.material3.ModalBottomSheet(
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

            // All option
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

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
    val isDiscontinued = product.product.status == com.vinh.dyvat.data.model.ProductStatus.DISCONTINUED

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDiscontinued) MidDark.copy(alpha = 0.5f) else DarkSurface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isDiscontinued) {
                Text(
                    text = "NGUNG KINH DOANH",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = "Ma san pham: ${product.product.code.ifEmpty { "—" }}",
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = product.product.name,
                color = TextWhite,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Loai san pham: ${product.categoryName.ifEmpty { "—" }}",
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Nha cung cap: ${product.supplierName.ifEmpty { "—" }}",
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall
            )
            if (isDiscontinued && onResumeClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = onResumeClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = SpotifyGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kinh doanh lai", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
