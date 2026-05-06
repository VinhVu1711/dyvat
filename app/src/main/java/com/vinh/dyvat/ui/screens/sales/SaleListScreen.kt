package com.vinh.dyvat.ui.screens.sales

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.material3.FloatingActionButton
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
import com.vinh.dyvat.data.model.SaleTicketStatus
import com.vinh.dyvat.ui.components.EmptyState
import com.vinh.dyvat.ui.components.ErrorState
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.components.StatusBadge
import com.vinh.dyvat.ui.components.StatusType
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    showBackButton: Boolean = true,
    refreshSignal: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: SaleViewModel = hiltViewModel()
) {
    val uiState by viewModel.listState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()
    val hasDateFilter = uiState.fromDate.isNotBlank() || uiState.toDate.isNotBlank()
    val hasAnyFilter = uiState.searchQuery.isNotBlank() ||
            hasDateFilter ||
            uiState.selectedStatusFilter != SaleStatusFilter.ALL

    LaunchedEffect(refreshSignal) {
        if (refreshSignal) {
            viewModel.loadTickets()
            onRefreshHandled()
        }
    }

    LaunchedEffect(uiState.filteredTickets, uiState.currentPage) {
        if (uiState.currentPage == 0 && uiState.filteredTickets.isNotEmpty()) {
            lazyListState.animateScrollToItem(0)
        }
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "QUẢN LÝ BÁN HÀNG",
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd, containerColor = SpotifyGreen) {
                Icon(Icons.Default.Add, contentDescription = "Tạo phiếu bán", tint = NearBlack)
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
                SaleSearchTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = "Tìm kiếm mã phiếu bán..."
                )

                Box {
                    SaleSortDropdownButton(
                        currentSort = uiState.sortOption,
                        onClick = { showSortMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(DarkCard)
                    ) {
                        SaleSortOption.entries.forEach { option ->
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

                SaleStatusFilterRow(
                    selected = uiState.selectedStatusFilter,
                    onSelect = { viewModel.setStatusFilter(it) }
                )

                Text("Lọc ngày bán", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SaleDateFilterField(
                        label = "Từ ngày",
                        date = uiState.fromDate,
                        onClick = { showFromDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    SaleDateFilterField(
                        label = "Đến ngày",
                        date = uiState.toDate,
                        onClick = { showToDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (hasDateFilter) {
                    TextButton(onClick = { viewModel.clearDateFilters() }, modifier = Modifier.align(Alignment.End)) {
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
                text = "Danh sách phiếu bán hàng",
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
                onRefresh = { viewModel.loadTickets() },
                state = pullToRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp)
                ) {
                    when {
                        uiState.isLoading -> item { LoadingIndicator() }
                        uiState.error != null && uiState.tickets.isEmpty() -> item {
                            ErrorState(message = uiState.error ?: "", onRetry = { viewModel.loadTickets() })
                        }
                        uiState.filteredTickets.isEmpty() -> item {
                            EmptyState(
                                icon = Icons.Default.Receipt,
                                title = if (hasAnyFilter) "Không tìm thấy phiếu bán" else "Chưa có phiếu bán",
                                subtitle = if (hasAnyFilter) {
                                    "Thử thay đổi từ khóa hoặc bộ lọc"
                                } else {
                                    "Nhấn + để tạo phiếu bán hàng"
                                }
                            )
                        }
                        else -> {
                            items(items = uiState.filteredTickets, key = { it.id }) { ticket ->
                                SaleTicketCard(ticket = ticket, onClick = { onNavigateToDetail(ticket.id) })
                            }

                            if (uiState.hasMore) {
                                item {
                                    TextButton(
                                        onClick = { viewModel.loadMore() },
                                        enabled = !uiState.isLoadingMore,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (uiState.isLoadingMore) "Đang tải..." else "Tải thêm phiếu bán",
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

    if (showFromDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = uiState.fromDate.toDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setFromDate(it.toDateString()) }
                    showFromDatePicker = false
                }) { Text("Chọn", color = SpotifyGreen) }
            },
            dismissButton = { TextButton(onClick = { showFromDatePicker = false }) { Text("Hủy", color = TextSilver) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showToDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = uiState.toDate.toDateMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showToDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setToDate(it.toDateString()) }
                    showToDatePicker = false
                }) { Text("Chọn", color = SpotifyGreen) }
            },
            dismissButton = { TextButton(onClick = { showToDatePicker = false }) { Text("Hủy", color = TextSilver) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SaleSearchTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextSilver.copy(alpha = 0.6f)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSilver) },
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
private fun SaleSortDropdownButton(currentSort: SaleSortOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Sắp xếp: ${currentSort.label}", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(4.dp))
        Text("v", color = TextSilver, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SaleStatusFilterRow(selected: SaleStatusFilter, onSelect: (SaleStatusFilter) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SaleStatusFilterChip("Tất cả phiếu", selected == SaleStatusFilter.ALL) { onSelect(SaleStatusFilter.ALL) }
        SaleStatusFilterChip("Đang hoạt động", selected == SaleStatusFilter.ACTIVE) { onSelect(SaleStatusFilter.ACTIVE) }
        SaleStatusFilterChip("Đã hủy", selected == SaleStatusFilter.CANCELLED) { onSelect(SaleStatusFilter.CANCELLED) }
    }
}

@Composable
private fun SaleStatusFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = DarkCard,
            labelColor = TextSilver,
            selectedContainerColor = SpotifyGreen.copy(alpha = 0.2f),
            selectedLabelColor = SpotifyGreen
        )
    )
}

@Composable
private fun SaleDateFilterField(label: String, date: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = TextSilver, style = MaterialTheme.typography.labelSmall)
            Text(
                text = date.ifEmpty { "Chọn ngày" },
                color = if (date.isNotEmpty()) TextWhite else TextSilver.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SaleTicketCard(ticket: SaleTicketCardUi, onClick: () -> Unit) {
    val isCancelled = ticket.status == SaleTicketStatus.CANCELLED
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isCancelled) DarkCard.copy(alpha = 0.6f) else DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Mã phiếu bán hàng: ${ticket.code.ifEmpty { ticket.id.take(8).uppercase() }}",
                color = if (isCancelled) TextSilver else TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ngày bán hàng: ${formatSaleDate(ticket.saleDate)}", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tổng tiền bán: ${ticket.totalSaleAmountVnd.toVnd()}",
                color = if (isCancelled) TextSilver else SpotifyGreen,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trạng thái: ", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                if (isCancelled) {
                    StatusBadge(label = "Đã hủy", type = StatusType.CANCELLED)
                } else {
                    StatusBadge(label = "Đang hoạt động", type = StatusType.ACTIVE)
                }
            }
        }
    }
}

fun formatSaleDate(dateStr: String): String {
    return try {
        val parts = dateStr.split("T")[0].split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else dateStr
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

private fun Long.toDateString(): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(this))
