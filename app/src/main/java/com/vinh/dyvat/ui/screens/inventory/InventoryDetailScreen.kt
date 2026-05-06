package com.vinh.dyvat.ui.screens.inventory

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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.vinh.dyvat.data.model.InventoryLotDetail
import com.vinh.dyvat.data.model.LotStatus
import com.vinh.dyvat.ui.components.EmptyState
import com.vinh.dyvat.ui.components.ErrorState
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.components.StatusBadge
import com.vinh.dyvat.ui.components.StatusType
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    ticketId: String,
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsState()

    LaunchedEffect(ticketId) {
        viewModel.loadLotDetail(ticketId)
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CHI TIẾT LÔ HÀNG",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại danh sách kho",
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
                onRetry = { viewModel.loadLotDetail(ticketId) }
            )
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        LotSummaryCard(
                            lotCode = detailState.lotCode.ifEmpty { ticketId.take(8).uppercase() },
                            purchaseDate = detailState.purchaseDate,
                            lotStatus = detailState.lotStatus,
                            totalValue = detailState.totalValue
                        )
                    }

                    item {
                        Text(
                            text = "Danh sách sản phẩm tồn kho trong lô",
                            color = TextWhite,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (detailState.products.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Inventory2,
                                title = "Không có sản phẩm tồn kho",
                                subtitle = "Lô này hiện không còn sản phẩm nào trong kho"
                            )
                        }
                    } else {
                        items(
                            items = detailState.products,
                            key = { it.purchaseItemId }
                        ) { product ->
                            InventoryProductCard(product = product)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LotSummaryCard(
    lotCode: String,
    purchaseDate: String,
    lotStatus: LotStatus,
    totalValue: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailTextRow(label = "Mã lô nhập", value = lotCode)
            DetailTextRow(label = "Ngày nhập hàng", value = formatDate(purchaseDate))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trạng thái lô: ",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                StatusBadge(
                    label = lotStatus.label(),
                    type = lotStatus.statusType()
                )
            }

            DetailTextRow(
                label = "Tổng giá trị tồn kho của lô",
                value = totalValue.toVnd(),
                valueColor = SpotifyGreen,
                valueWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InventoryProductCard(product: InventoryLotDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MidDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailTextRow(
                label = "Tên sản phẩm",
                value = product.productName,
                valueColor = TextWhite,
                valueWeight = FontWeight.SemiBold
            )
            if (product.productCode.isNotBlank()) {
                DetailTextRow(label = "Mã sản phẩm", value = product.productCode)
            }
            DetailTextRow(label = "Đơn vị tính", value = product.unitName)
            DetailTextRow(label = "Nhà cung cấp", value = product.supplierName.ifBlank { "Không có" })
            DetailTextRow(
                label = "Ngày hết hạn",
                value = product.expiryDate?.takeIf { it.isNotBlank() }?.let(::formatDate) ?: "Không có"
            )
            DetailTextRow(label = "Số lượng tồn kho", value = product.quantityRemaining.toString())
            DetailTextRow(label = "Giá nhập / 1 đơn vị", value = product.purchasePriceVnd.toVnd())
            DetailTextRow(
                label = "Tổng giá trị còn trong kho",
                value = product.remainingValueVnd.toVnd(),
                valueColor = SpotifyGreen,
                valueWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DetailTextRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextWhite,
    valueWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            color = TextSilver,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.9f)
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueWeight,
            modifier = Modifier
                .weight(1.1f)
                .padding(start = 8.dp)
        )
    }
}

private fun LotStatus.label(): String {
    return when (this) {
        LotStatus.IN_STOCK -> "Còn hàng"
        LotStatus.OUT_OF_STOCK -> "Hết hàng"
        LotStatus.CANCELLED -> "Đã hủy"
        LotStatus.HAS_EXPIRED_ITEM -> "Có hàng hết hạn"
    }
}

private fun LotStatus.statusType(): StatusType {
    return when (this) {
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
