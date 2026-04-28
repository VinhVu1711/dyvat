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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.vinh.dyvat.ui.components.DyvatSearchBar
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
import com.vinh.dyvat.ui.theme.WarningOrange
import com.vinh.dyvat.ui.components.toVnd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    showBackButton: Boolean = true,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "San pham",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lai",
                                tint = TextWhite
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Loc",
                            tint = if (uiState.selectedCategoryId != null || uiState.selectedSupplierId != null) SpotifyGreen else TextSilver
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NearBlack
                )
            )
        },
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
            DyvatSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = "Tim kiem san pham...",
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (uiState.selectedCategoryId != null || uiState.selectedSupplierId != null) {
                ActiveFiltersRow(
                    categories = uiState.categories,
                    suppliers = uiState.suppliers,
                    selectedCategoryId = uiState.selectedCategoryId,
                    selectedSupplierId = uiState.selectedSupplierId,
                    onClearCategory = { viewModel.setCategoryFilter(null) },
                    onClearSupplier = { viewModel.setSupplierFilter(null) },
                    onClearAll = { viewModel.clearFilters() }
                )
            }

            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorState(
                    message = uiState.error ?: "",
                    onRetry = { viewModel.loadProducts() }
                )
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            vertical = 8.dp
                        )
                    ) {
                        items(
                            items = uiState.filteredProducts,
                            key = { it.product.id }
                        ) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onNavigateToDetail(product.product.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            categories = uiState.categories,
            suppliers = uiState.suppliers,
            selectedCategoryId = uiState.selectedCategoryId,
            selectedSupplierId = uiState.selectedSupplierId,
            onCategorySelected = {
                viewModel.setCategoryFilter(it)
                showFilterSheet = false
            },
            onSupplierSelected = {
                viewModel.setSupplierFilter(it)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (uiState.showDeleteConfirm && uiState.productToDelete != null) {
        ConfirmDialog(
            title = "Xac nhan xoa",
            message = "Ban co chac muon xoa san pham \"${uiState.productToDelete!!.product.name}\"? Hanh dong nay co the anh huong du lieu.",
            confirmText = "Xoa",
            isDestructive = true,
            onDismiss = { viewModel.hideDeleteConfirm() },
            onConfirm = {
                viewModel.hideDeleteConfirm()
            }
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
    onClearSupplier: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        selectedCategoryId?.let { id ->
            val cat = categories.find { it.id == id }
            FilterChip(
                selected = true,
                onClick = onClearCategory,
                label = { Text(cat?.name ?: "Loai") },
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
        if (selectedCategoryId != null || selectedSupplierId != null) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Xoa loc",
                color = SpotifyGreen,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onClearAll() }
                    .padding(4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    categories: List<Category>,
    suppliers: List<Supplier>,
    selectedCategoryId: String?,
    selectedSupplierId: String?,
    onCategorySelected: (String?) -> Unit,
    onSupplierSelected: (String?) -> Unit,
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
                text = "Loc san pham",
                color = TextWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (categories.isNotEmpty()) {
                Text(
                    text = "Loai san pham",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = cat.id == selectedCategoryId,
                            onClick = {
                                onCategorySelected(if (cat.id == selectedCategoryId) null else cat.id)
                            },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpotifyGreen.copy(alpha = 0.2f),
                                selectedLabelColor = SpotifyGreen,
                                containerColor = MidDark,
                                labelColor = TextSilver
                            )
                        )
                    }
                }
            }

            if (suppliers.isNotEmpty()) {
                Text(
                    text = "Nha cung cap",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(suppliers) { sup ->
                        FilterChip(
                            selected = sup.id == selectedSupplierId,
                            onClick = {
                                onSupplierSelected(if (sup.id == selectedSupplierId) null else sup.id)
                            },
                            label = { Text(sup.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpotifyGreen.copy(alpha = 0.2f),
                                selectedLabelColor = SpotifyGreen,
                                containerColor = MidDark,
                                labelColor = TextSilver
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Dong", color = TextSilver)
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: ProductWithDetails,
    onClick: () -> Unit
) {
    val isDiscontinued = product.product.status == ProductStatus.DISCONTINUED

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDiscontinued) DarkCard.copy(alpha = 0.6f) else DarkSurface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDiscontinued) MidDark else SpotifyGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = if (isDiscontinued) TextSilver else SpotifyGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.product.name,
                        color = if (isDiscontinued) TextSilver else TextWhite,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isDiscontinued) {
                        StatusBadge(
                            label = "Ngung KD",
                            type = StatusType.WARNING
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (product.categoryName.isNotEmpty()) {
                        Text(
                            text = product.categoryName,
                            color = TextSilver,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (product.unitName.isNotEmpty()) {
                        Text(
                            text = "\u2022 ${product.unitName}",
                            color = TextSilver,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Gia nhap",
                            color = TextSilver,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = product.product.defaultPurchasePriceVnd.toVnd(),
                            color = TextWhite,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column {
                        Text(
                            text = "Gia ban",
                            color = TextSilver,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = product.product.defaultSalePriceVnd.toVnd(),
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
