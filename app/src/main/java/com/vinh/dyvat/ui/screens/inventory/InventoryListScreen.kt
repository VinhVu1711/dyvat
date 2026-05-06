package com.vinh.dyvat.ui.screens.inventory

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.vinh.dyvat.data.model.InventoryLotCard
import com.vinh.dyvat.data.model.LotStatus
import com.vinh.dyvat.ui.components.EmptyState
import com.vinh.dyvat.ui.components.ErrorState
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.components.StatusBadge
import com.vinh.dyvat.ui.components.StatusType
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import com.vinh.dyvat.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    showBackButton: Boolean = true,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val listState by viewModel.listState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()
    val hasDateFilter = listState.fromDate.isNotBlank() || listState.toDate.isNotBlank()
    val hasAnyFilter = listState.searchQuery.isNotBlank() || hasDateFilter || listState.showOutOfStock

    LaunchedEffect(listState.lots, listState.currentPage) {
        if (listState.currentPage == 0 && listState.lots.isNotEmpty()) {
            lazyListState.animateScrollToItem(0)
        }
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "QUẢN LÝ KHO",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NearBlack)
            )
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
                    value = listState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = "Tìm kiếm mã lô nhập..."
                )

                Box {
                    SortDropdownButton(
                        currentSort = listState.sortOption,
                        onClick = { showSortMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(DarkCard)
                    ) {
                        InventorySortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (option == listState.sortOption) SpotifyGreen else TextWhite
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

                FilterChip(
                    selected = listState.showOutOfStock,
                    onClick = { viewModel.toggleShowOutOfStock() },
                    label = {
                        Text(
                            text = if (listState.showOutOfStock) {
                                "Đang hiển thị cả lô hết hàng"
                            } else {
                                "Chỉ hiển thị lô còn hàng"
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpotifyGreen.copy(alpha = 0.2f),
                        selectedLabelColor = SpotifyGreen,
                        containerColor = MidDark,
                        labelColor = TextSilver
                    )
                )

                Text(
                    text = "Lọc ngày nhập",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateFilterField(
                        label = "Từ ngày",
                        date = listState.fromDate,
                        onClick = { showFromDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    DateFilterField(
                        label = "Đến ngày",
                        date = listState.toDate,
                        onClick = { showToDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (hasDateFilter) {
                    TextButton(
                        onClick = { viewModel.clearDateFilters() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Hủy lọc ngày",
                            color = SpotifyGreen,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = "Danh sách lô hàng trong kho",
                color = TextWhite,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NearBlack)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            PullToRefreshBox(
                isRefreshing = listState.isLoading,
                onRefresh = { viewModel.loadLots() },
                state = pullToRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                listState.isLoading -> LoadingIndicator()
                listState.error != null -> ErrorState(
                    message = listState.error ?: "",
                    onRetry = { viewModel.loadLots() }
                )
                listState.lots.isEmpty() -> EmptyState(
                    icon = Icons.Default.Inventory2,
                    title = if (hasAnyFilter) "Không tìm thấy lô hàng" else "Kho trống",
                    subtitle = if (hasAnyFilter) {
                        "Thử thay đổi mã lô hoặc bộ lọc"
                    } else {
                        "Chưa có lô hàng nào trong kho"
                    }
                )
                else -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        item {
                            if (false) {
                                Text(
                                text = "Danh sách lô hàng trong kho",
                                color = TextWhite,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        items(
                            items = listState.lots,
                            key = { it.purchaseTicketId }
                        ) { lot ->
                            InventoryLotCard(
                                lot = lot,
                                onClick = { onNavigateToDetail(lot.purchaseTicketId) }
                            )
                        }

                        if (listState.hasMore) {
                            item {
                                TextButton(
                                    onClick = { viewModel.loadMore() },
                                    enabled = !listState.isLoadingMore,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (listState.isLoadingMore) {
                                            "Đang tải..."
                                        } else {
                                            "Tải thêm lô hàng"
                                        },
                                        color = SpotifyGreen,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Ghi chú: Mặc định chỉ hiển thị lô còn hàng theo ngày nhập cũ nhất. Lô hết hàng không hiển thị mặc định nhưng vẫn lưu để đối chiếu lịch sử nhập, bán và thống kê.",
                                color = TextSilver,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                            )
                        }
                    }
                }
                }
            }
        }
    }

    if (showFromDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = listState.fromDate.toDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.setFromDate(millis.toDateString())
                        }
                        showFromDatePicker = false
                    }
                ) {
                    Text("Chọn", color = SpotifyGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFromDatePicker = false }) {
                    Text("Hủy", color = TextSilver)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showToDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = listState.toDate.toDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showToDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.setToDate(millis.toDateString())
                        }
                        showToDatePicker = false
                    }
                ) {
                    Text("Chọn", color = SpotifyGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showToDatePicker = false }) {
                    Text("Hủy", color = TextSilver)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
            focusedContainerColor = MidDark,
            unfocusedContainerColor = MidDark,
            cursorColor = SpotifyGreen
        ),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SortDropdownButton(
    currentSort: InventorySortOption,
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
            text = "▾",
            color = TextSilver,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DateFilterField(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = SpotifyGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                color = TextSilver,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = if (date.isNotEmpty()) date else "Chọn ngày",
                color = if (date.isNotEmpty()) TextWhite else TextSilver.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InventoryLotCard(
    lot: InventoryLotCard,
    onClick: () -> Unit
) {
    val isOutOfStock = lot.lotStatus == LotStatus.OUT_OF_STOCK

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) DarkCard.copy(alpha = 0.6f) else DarkSurface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (lot.lotStatus) {
                                LotStatus.IN_STOCK -> SpotifyGreen.copy(alpha = 0.15f)
                                LotStatus.OUT_OF_STOCK -> MidDark
                                LotStatus.CANCELLED -> MidDark
                                LotStatus.HAS_EXPIRED_ITEM -> WarningOrange.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = when (lot.lotStatus) {
                            LotStatus.IN_STOCK -> SpotifyGreen
                            LotStatus.OUT_OF_STOCK -> TextSilver
                            LotStatus.CANCELLED -> TextSilver
                            LotStatus.HAS_EXPIRED_ITEM -> WarningOrange
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mã lô nhập: ${lot.lotCode.ifEmpty { lot.purchaseTicketId.take(8).uppercase() }}",
                        color = if (isOutOfStock) TextSilver else TextWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ngày nhập hàng: ${formatDate(lot.purchaseDate)}",
                        color = TextSilver,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Tổng giá trị tồn kho: ${lot.totalInventoryValueVnd.toVnd()}",
                color = if (isOutOfStock) TextSilver else SpotifyGreen,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Trạng thái lô: ",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                StatusBadge(
                    label = lot.statusLabel(),
                    type = lot.statusType()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ngày hết hạn gần nhất: ${lot.nearestExpiryDate?.let { formatDate(it) } ?: "Không có"}",
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Số lượng còn: ${lot.totalRemainingQuantity}",
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun InventoryLotCard.statusLabel(): String {
    return when (lotStatus) {
        LotStatus.IN_STOCK -> "Còn hàng"
        LotStatus.OUT_OF_STOCK -> "Hết hàng"
        LotStatus.CANCELLED -> "Đã hủy"
        LotStatus.HAS_EXPIRED_ITEM -> "Có hàng hết hạn"
    }
}

private fun InventoryLotCard.statusType(): StatusType {
    return when (lotStatus) {
        LotStatus.IN_STOCK -> StatusType.IN_STOCK
        LotStatus.OUT_OF_STOCK -> StatusType.OUT_OF_STOCK
        LotStatus.CANCELLED -> StatusType.CANCELLED
        LotStatus.HAS_EXPIRED_ITEM -> StatusType.WARNING
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val parts = dateStr.split("T")[0].split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            dateStr
        }
    } catch (_: Exception) {
        dateStr
    }
}

private fun String.toDateMillisOrNull(): Long? {
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(this)?.time
    } catch (_: Exception) {
        null
    }
}

private fun Long.toDateString(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(this))
}
