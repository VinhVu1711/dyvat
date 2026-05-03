package com.vinh.dyvat.ui.screens.purchase

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.data.model.PurchaseItemWithDetails
import com.vinh.dyvat.data.model.TicketStatus
import com.vinh.dyvat.ui.components.ConfirmDialog
import com.vinh.dyvat.ui.components.EmptyState
import com.vinh.dyvat.ui.components.ErrorState
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.components.StatusBadge
import com.vinh.dyvat.ui.components.StatusType
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import com.vinh.dyvat.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    ticketId: String,
    onNavigateBack: () -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsState()

    LaunchedEffect(ticketId) {
        viewModel.loadTicketDetail(ticketId)
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CHI TIẾT PHIẾU NHẬP HÀNG",
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
        }
    ) { innerPadding ->
        when {
            detailState.isLoading -> LoadingIndicator()
            detailState.error != null -> ErrorState(
                message = detailState.error ?: "",
                onRetry = { viewModel.loadTicketDetail(ticketId) }
            )
            detailState.ticket != null -> {
                val ticket = detailState.ticket!!
                val isCancelled = ticket.status == TicketStatus.CANCELLED
                val totalAmount = detailState.items.sumOf { it.item.lineTotalVnd }

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

                    // Ticket Info Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Mã phiếu nhập hàng
                                Text(
                                    text = "Mã phiếu nhập hàng: ${ticket.code.ifEmpty { ticket.id.take(8).uppercase() }}",
                                    color = TextWhite,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Ngày nhập hàng
                                Text(
                                    text = "Ngày nhập hàng: ${formatDate(ticket.purchaseDate)}",
                                    color = TextWhite,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Tổng tiền nhập
                                Text(
                                    text = "Tổng tiền nhập: ${totalAmount.toVnd()}",
                                    color = SpotifyGreen,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Trạng thái
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Trạng thái: ",
                                        color = TextWhite,
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

                    // Product list header
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Danh sách sản phẩm đã nhập",
                            color = TextWhite,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Product items
                    if (detailState.items.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Receipt,
                                title = "Không có sản phẩm",
                                subtitle = "Phiếu này chưa có sản phẩm nào"
                            )
                        }
                    } else {
                        items(
                            items = detailState.items,
                            key = { it.item.id }
                        ) { item ->
                            PurchaseItemCard(item = item)
                        }
                    }

                    // Total amount section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = TextSilver.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tổng tiền nhập trong phiếu:",
                                color = TextWhite,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = totalAmount.toVnd(),
                                color = SpotifyGreen,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Action buttons
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Edit button (only for active tickets)
                            if (!isCancelled) {
                                OutlinedButton(
                                    onClick = { /* Navigate to edit */ },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = TextWhite
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Chỉnh sửa phiếu nhập",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Cancel button (only for active tickets)
                            if (!isCancelled) {
                                OutlinedButton(
                                    onClick = { viewModel.showCancelConfirm() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = WarningOrange
                                    )
                                ) {
                                    Text(
                                        text = "Hủy phiếu nhập",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Cancel confirmation dialog
    if (detailState.showCancelConfirm) {
        ConfirmDialog(
            title = "Hủy phiếu nhập",
            message = "Bạn có chắc muốn hủy phiếu nhập này? Hành động này có thể ảnh hưởng tồn kho.",
            confirmText = "Hủy phiếu",
            isDestructive = true,
            onDismiss = { viewModel.hideCancelConfirm() },
            onConfirm = {
                viewModel.cancelTicket(ticketId, null)
            }
        )
    }
}

@Composable
private fun PurchaseItemCard(item: PurchaseItemWithDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Tên sản phẩm
            Text(
                text = "Tên sản phẩm: ${item.productName}",
                color = TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Nhà cung cấp
            if (item.supplierName.isNotEmpty()) {
                Text(
                    text = "Nhà cung cấp: ${item.supplierName}",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Đơn vị tính
            Text(
                text = "Đơn vị tính: ${item.unitName}",
                color = TextSilver,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Ngày hết hạn
            if (!item.item.expiryDate.isNullOrEmpty()) {
                Text(
                    text = "Ngày hết hạn: ${formatDate(item.item.expiryDate)}",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Số lượng nhập
            Text(
                text = "Số lượng nhập: ${item.item.quantityPurchased}",
                color = TextSilver,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Giá nhập / 1 đơn vị
            Text(
                text = "Giá nhập / 1 đơn vị: ${item.item.purchasePriceVnd.toVnd()}",
                color = TextSilver,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = TextSilver.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(8.dp))

            // Tổng tiền nhập sản phẩm này
            Text(
                text = "Tổng tiền nhập sản phẩm này: ${item.item.lineTotalVnd.toVnd()}",
                color = SpotifyGreen,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
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
