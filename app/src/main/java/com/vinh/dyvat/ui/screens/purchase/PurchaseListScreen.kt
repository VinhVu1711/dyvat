package com.vinh.dyvat.ui.screens.purchase

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.vinh.dyvat.data.model.TicketStatus
import com.vinh.dyvat.ui.components.DyvatTopBar
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
fun PurchaseListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    showBackButton: Boolean = true,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    val listState by viewModel.listState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            if (showBackButton) {
                TopAppBar(
                    title = {
                        Text(
                            text = "QUẢN LÝ NHẬP HÀNG",
                            color = TextWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = TextWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NearBlack)
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "QUẢN LÝ NHẬP HÀNG",
                            color = TextWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NearBlack)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Search bar
            item {
                SearchTextField(
                    value = listState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = "Tìm kiếm mã phiếu nhập..."
                )
            }

            // Sort dropdown
            item {
                Box {
                    SortDropdownButton(
                        currentSort = listState.sortOption,
                        onClick = { showSortMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.entries.forEach { option ->
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
            }

            // Date filter label
            item {
                Text(
                    text = "Lọc ngày nhập",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Date filter row
            item {
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
            }

            // List header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Danh sách phiếu nhập hàng",
                    color = TextWhite,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Content
            when {
                listState.isLoading -> {
                    item {
                        LoadingIndicator()
                    }
                }
                listState.error != null -> {
                    item {
                        ErrorState(
                            message = listState.error ?: "",
                            onRetry = { viewModel.loadTickets() }
                        )
                    }
                }
                listState.filteredTickets.isEmpty() -> {
                    item {
                        EmptyState(
                            icon = Icons.Default.Receipt,
                            title = "Chưa có phiếu nhập",
                            subtitle = "Tạo phiếu nhập hàng để bắt đầu"
                        )
                    }
                }
                else -> {
                    items(
                        items = listState.filteredTickets,
                        key = { it.id }
                    ) { ticket ->
                        PurchaseTicketCard(
                            ticket = ticket,
                            onClick = { onNavigateToDetail(ticket.id) }
                        )
                    }
                }
            }

            // Add button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AddTicketButton(onClick = onNavigateToAdd)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // From date picker
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

    // To date picker
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
            focusedContainerColor = DarkCard,
            unfocusedContainerColor = DarkCard,
            cursorColor = SpotifyGreen
        ),
        shape = RoundedCornerShape(12.dp)
    )
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
            text = "▼",
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
private fun PurchaseTicketCard(
    ticket: PurchaseTicketCardUi,
    onClick: () -> Unit
) {
    val isCancelled = ticket.status == TicketStatus.CANCELLED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled) DarkCard.copy(alpha = 0.6f) else DarkCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Mã phiếu nhập
            Text(
                text = "Mã phiếu nhập hàng: ${ticket.code.ifEmpty { ticket.id.take(8).uppercase() }}",
                color = if (isCancelled) TextSilver else TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ngày nhập hàng
            Text(
                text = "Ngày nhập hàng: ${formatDate(ticket.purchaseDate)}",
                color = TextSilver,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tổng tiền nhập
            Text(
                text = "Tổng tiền nhập: ${ticket.totalAmountVnd.toVnd()}",
                color = if (isCancelled) TextSilver else SpotifyGreen,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Trạng thái
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trạng thái: ",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isCancelled) {
                    StatusBadge(
                        label = "Đã hủy",
                        type = StatusType.CANCELLED
                    )
                } else {
                    StatusBadge(
                        label = "Đang hoạt động",
                        type = StatusType.ACTIVE
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTicketButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = NearBlack
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Tạo phiếu nhập hàng",
            color = NearBlack,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val parts = dateStr.split("T")[0].split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else dateStr
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

enum class SortOption(val label: String) {
    DATE_NEWEST("Ngày mới nhất trước"),
    DATE_OLDEST("Ngày cũ nhất trước"),
    AMOUNT_HIGHEST("Tổng tiền cao nhất"),
    AMOUNT_LOWEST("Tổng tiền thấp nhất")
}
