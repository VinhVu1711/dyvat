package com.vinh.dyvat.ui.screens.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
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
import com.vinh.dyvat.data.model.SaleItemWithDetails
import com.vinh.dyvat.data.model.SaleTicketStatus
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
fun SaleDetailScreen(
    ticketId: String,
    onNavigateBack: () -> Unit,
    viewModel: SaleViewModel = hiltViewModel()
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
                        text = "CHI TIẾT PHIẾU BÁN HÀNG",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = TextWhite)
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
                val isCancelled = ticket.status == SaleTicketStatus.CANCELLED
                val totalRevenue = detailState.items.sumOf { it.item.lineRevenueVnd }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item {
                        SaleTicketInfoCard(ticket = ticket, isCancelled = isCancelled, totalRevenue = totalRevenue)
                    }
                    detailState.actionError?.let { error ->
                        item {
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Danh sách sản phẩm đã bán",
                            color = TextWhite,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (detailState.items.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Receipt,
                                title = "Không có sản phẩm",
                                subtitle = "Phiếu này chưa có sản phẩm nào"
                            )
                        }
                    } else {
                        items(items = detailState.items, key = { it.item.id }) { item ->
                            SaleItemCard(item = item)
                        }
                    }

                    item { SaleTotalSection(totalRevenue = totalRevenue) }

                    if (!isCancelled) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { viewModel.showCancelConfirm() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange)
                            ) {
                                Text(
                                    text = "Hủy phiếu bán",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }

    if (detailState.showCancelConfirm) {
        ConfirmDialog(
            title = "Hủy phiếu bán",
            message = "Bạn có chắc muốn hủy phiếu bán này? Hệ thống sẽ hoàn lại số lượng đã bán vào đúng lô ban đầu.",
            confirmText = "Hủy phiếu",
            dismissText = "Đóng",
            isDestructive = true,
            onDismiss = { viewModel.hideCancelConfirm() },
            onConfirm = { viewModel.cancelTicket(ticketId, null) }
        )
    }
}

@Composable
private fun SaleTicketInfoCard(
    ticket: SaleTicketUi,
    isCancelled: Boolean,
    totalRevenue: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Mã phiếu bán hàng: ${ticket.code.ifEmpty { ticket.id.take(8).uppercase() }}",
                color = TextWhite,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ngày bán hàng: ${formatSaleDate(ticket.saleDate)}", color = TextWhite, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tổng tiền bán: ${totalRevenue.toVnd()}",
                color = if (isCancelled) TextSilver else SpotifyGreen,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trạng thái: ", color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                if (isCancelled) {
                    StatusBadge(label = "Đã hủy", type = StatusType.CANCELLED)
                } else {
                    StatusBadge(label = "Đang hoạt động", type = StatusType.ACTIVE)
                }
            }
            if (isCancelled) {
                ticket.cancelledAt?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Thời gian hủy: ${formatSaleDate(it)}", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                }
                ticket.cancelReason?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Lý do hủy: $it", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SaleItemCard(item: SaleItemWithDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Tên sản phẩm: ${item.productName}",
                color = TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Đơn vị tính: ${item.unitName}", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Mã lô: ${item.lotCode}", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            item.expiryDate?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ngày hết hạn: ${formatSaleDate(it)}", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Số lượng bán: ${item.item.quantitySold}", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Giá bán / 1 đơn vị: ${item.item.salePriceVnd.toVnd()}",
                color = TextSilver,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = TextSilver.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tổng giá bán sản phẩm này: ${item.item.lineRevenueVnd.toVnd()}",
                color = SpotifyGreen,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SaleTotalSection(totalRevenue: Long) {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = TextSilver.copy(alpha = 0.3f))
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tổng tiền bán trong phiếu:",
            color = TextWhite,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = totalRevenue.toVnd(),
            color = SpotifyGreen,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
