package com.vinh.dyvat.ui.screens.inventory

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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    showBackButton: Boolean = true,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val listState by viewModel.listState.collectAsState()

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kho",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lai",
                                tint = TextWhite
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowOutOfStock() }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Loc",
                            tint = if (listState.showOutOfStock) SpotifyGreen else TextSilver
                        )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = listState.showOutOfStock,
                    onClick = { viewModel.toggleShowOutOfStock() },
                    label = {
                        Text(
                            text = if (listState.showOutOfStock) "Dang hien thi het hang" else "An het hang"
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpotifyGreen.copy(alpha = 0.2f),
                        selectedLabelColor = SpotifyGreen,
                        containerColor = MidDark,
                        labelColor = TextSilver
                    )
                )
            }

            when {
                listState.isLoading -> LoadingIndicator()
                listState.error != null -> ErrorState(
                    message = listState.error ?: "",
                    onRetry = { viewModel.loadLots() }
                )
                listState.lots.isEmpty() -> EmptyState(
                    icon = Icons.Default.Inventory2,
                    title = "Kho trong",
                    subtitle = "Chua co lo hang nao trong kho"
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
                            items = listState.lots,
                            key = { it.purchaseTicketId }
                        ) { lot ->
                            InventoryLotCard(
                                lot = lot,
                                onClick = { onNavigateToDetail(lot.purchaseTicketId) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryLotCard(
    lot: InventoryLotCard,
    onClick: () -> Unit
) {
    val isOutOfStock = lot.lotStatus == LotStatus.OUT_OF_STOCK

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOutOfStock) DarkCard.copy(alpha = 0.6f) else DarkSurface)
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
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lot.lotCode.ifEmpty { "Lo #${lot.purchaseTicketId.take(8)}" },
                        color = if (isOutOfStock) TextSilver else TextWhite,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatusBadge(
                        label = when (lot.lotStatus) {
                            LotStatus.IN_STOCK -> "Con hang"
                            LotStatus.OUT_OF_STOCK -> "Het hang"
                            LotStatus.CANCELLED -> "Da huy"
                            LotStatus.HAS_EXPIRED_ITEM -> "Co het han"
                        },
                        type = when (lot.lotStatus) {
                            LotStatus.IN_STOCK -> StatusType.IN_STOCK
                            LotStatus.OUT_OF_STOCK -> StatusType.OUT_OF_STOCK
                            LotStatus.CANCELLED -> StatusType.CANCELLED
                            LotStatus.HAS_EXPIRED_ITEM -> StatusType.WARNING
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatDate(lot.purchaseDate),
                    color = TextSilver,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${lot.totalRemainingQuantity} san pham",
                        color = TextSilver,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = lot.totalInventoryValueVnd.toVnd(),
                        color = if (isOutOfStock) TextSilver else SpotifyGreen,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
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
