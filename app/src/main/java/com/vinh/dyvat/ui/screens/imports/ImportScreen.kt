package com.vinh.dyvat.ui.screens.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.data.model.*
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nhập hàng",
                        color = TextWhite
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = SpotifyGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm phiếu nhập",
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
            // Filter tabs
            FilterTabs(
                selectedFilter = uiState.dateFilter,
                onFilterSelected = { viewModel.setDateFilter(it) }
            )

            // Content
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.filteredItems.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    ImportItemsTable(
                        items = uiState.filteredItems,
                        onItemClick = { viewModel.showStatusDialog(it) },
                        onEditClick = { viewModel.showEditDialog(it.importItem) },
                        onDeleteClick = { viewModel.deleteImportItem(it.importItem.id) }
                    )
                }
            }
        }

        // Add/Edit Dialog
        if (uiState.showAddEditDialog) {
            AddEditImportDialog(
                item = uiState.editingItem,
                categories = uiState.categories,
                units = uiState.units,
                suppliers = uiState.suppliers,
                onDismiss = { viewModel.hideAddEditDialog() },
                onSave = { productName, categoryId, unitId, supplierId, totalImportAmount, unitPrice, totalQuantity, quantityForSale, expiryDate, notes ->
                    viewModel.saveImportItem(
                        productName, categoryId, unitId, supplierId,
                        totalImportAmount, unitPrice, totalQuantity, quantityForSale,
                        expiryDate, notes
                    )
                }
            )
        }

        // Status Dialog
        if (uiState.showStatusDialog && uiState.statusItem != null) {
            StatusUpdateDialog(
                item = uiState.statusItem!!,
                onDismiss = { viewModel.hideStatusDialog() },
                onUpdate = { status, quantityForSale, saleLocation ->
                    viewModel.updateItemStatus(
                        itemId = uiState.statusItem!!.importItem.id,
                        status = status,
                        quantityForSale = quantityForSale,
                        saleLocation = saleLocation
                    )
                }
            )
        }
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: DateFilter,
    onFilterSelected: (DateFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = when (filter) {
                            DateFilter.DAY -> "Ngày"
                            DateFilter.MONTH -> "Tháng"
                            DateFilter.YEAR -> "Năm"
                            DateFilter.ALL -> "Tất cả"
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SpotifyGreen,
                    selectedLabelColor = NearBlack,
                    containerColor = MidDark,
                    labelColor = TextSilver
                )
            )
        }
    }
}

@Composable
private fun ImportItemsTable(
    items: List<ImportItemWithDetails>,
    onItemClick: (ImportItemWithDetails) -> Unit,
    onEditClick: (ImportItemWithDetails) -> Unit,
    onDeleteClick: (ImportItemWithDetails) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        item {
            TableHeader()
        }

        items(items) { item ->
            TableRow(
                item = item,
                onClick = { onItemClick(item) },
                onEditClick = { onEditClick(item) },
                onDeleteClick = { onDeleteClick(item) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Tên SP",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.5f)
        )
        Text(
            text = "ĐVT",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tồn",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Bán",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Giá",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.End
        )
        Text(
            text = "TT",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TableRow(
    item: ImportItemWithDetails,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val importItem = item.importItem
    val statusColor = when (importItem.status) {
        ItemStatus.IN_STOCK -> WarningOrange
        ItemStatus.FOR_SALE -> SpotifyGreen
    }
    val statusText = when (importItem.status) {
        ItemStatus.IN_STOCK -> "Kho"
        ItemStatus.FOR_SALE -> "Bán"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MidDark),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = importItem.productName,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.categoryName,
                    color = TextSilver,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = item.unitName,
                color = TextSilver,
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.Center
            )
            Text(
                text = importItem.quantityInStock.toString(),
                color = TextSilver,
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.Center
            )
            Text(
                text = importItem.quantityForSale.toString(),
                color = TextWhite,
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.Center
            )
            Text(
                text = formatCurrency(importItem.unitPrice),
                color = TextWhite,
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.End
            )
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Chưa có sản phẩm nhập hàng.\nNhấn + để thêm mới.",
            color = TextSilver,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return "%,.0fđ".format(amount)
}
