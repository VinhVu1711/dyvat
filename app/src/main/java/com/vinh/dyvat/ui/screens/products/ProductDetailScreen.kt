package com.vinh.dyvat.ui.screens.products

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.data.model.ProductStatus
import com.vinh.dyvat.ui.components.ConfirmDialog
import com.vinh.dyvat.ui.components.ErrorState
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.components.StatusBadge
import com.vinh.dyvat.ui.components.StatusType
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.NegativeRed
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import com.vinh.dyvat.ui.theme.WarningOrange
import com.vinh.dyvat.ui.components.toVnd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onProductDeleted: () -> Unit = onNavigateBack,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailUiState.collectAsState()

    LaunchedEffect(productId) {
        viewModel.loadProductDetail(productId)
    }

    LaunchedEffect(detailState.navigateBack) {
        if (detailState.navigateBack) {
            onProductDeleted()
        }
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chi tiet san pham",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lai",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    if (detailState.product != null) {
                        IconButton(onClick = { onNavigateToEdit(productId) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Sua san pham",
                                tint = SpotifyGreen
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NearBlack
                )
            )
        }
    ) { innerPadding ->
        when {
            detailState.isLoading -> LoadingIndicator()
            detailState.error != null -> ErrorState(
                message = detailState.error ?: "",
                onRetry = { viewModel.loadProductDetail(productId) }
            )
            detailState.product != null -> {
                val product = detailState.product!!
                val isDiscontinued = product.product.status == ProductStatus.DISCONTINUED

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDiscontinued) MidDark else SpotifyGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = if (isDiscontinued) TextSilver else SpotifyGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.product.name,
                                color = TextWhite,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (product.categoryName.isNotEmpty()) {
                                    StatusBadge(
                                        label = product.categoryName,
                                        type = StatusType.INFO
                                    )
                                }
                                if (isDiscontinued) {
                                    StatusBadge(
                                        label = "Ngung kinh doanh",
                                        type = StatusType.WARNING
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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
                            Text(
                                text = "Thong tin gia",
                                color = TextSilver,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Gia nhap",
                                        color = TextSilver,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = product.product.defaultPurchasePriceVnd.toVnd(),
                                        color = TextWhite,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Gia ban",
                                        color = TextSilver,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = product.product.defaultSalePriceVnd.toVnd(),
                                        color = SpotifyGreen,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            Text(
                                text = "Thong tin chi tiet",
                                color = TextSilver,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (product.categoryName.isNotEmpty()) {
                                DetailRow(
                                    icon = Icons.Default.Category,
                                    label = "Loai san pham",
                                    value = product.categoryName
                                )
                            }

                            if (product.unitName.isNotEmpty()) {
                                DetailRow(
                                    icon = Icons.Default.Straighten,
                                    label = "Don vi tinh",
                                    value = product.unitName
                                )
                            }

                            if (product.supplierName.isNotEmpty()) {
                                DetailRow(
                                    icon = Icons.Default.LocalShipping,
                                    label = "Nha cung cap",
                                    value = product.supplierName
                                )
                            }

                            if (product.product.code.isNotEmpty()) {
                                DetailRow(
                                    icon = Icons.Default.Inventory2,
                                    label = "Ma san pham",
                                    value = product.product.code.ifEmpty { "-" }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isDiscontinued) {
                        Button(
                            onClick = { viewModel.showReactivateConfirm() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyGreen
                            ),
                            shape = RoundedCornerShape(500.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = NearBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kich hoat lai san pham",
                                color = NearBlack,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.showDiscontinueConfirm() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(500.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WarningOrange
                            )
                        ) {
                            Text(
                                text = "Ngung kinh doanh",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.requestDeleteProduct(productId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(500.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NegativeRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Xoa vinh vien",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (detailState.showDiscontinueConfirm) {
        ConfirmDialog(
            title = "Ngung kinh doanh",
            message = "San pham se bi an khoi danh sach tao phieu. Ban van co the kich hoat lai sau.",
            confirmText = "Ngung KD",
            isDestructive = false,
            onDismiss = { viewModel.hideDiscontinueConfirm() },
            onConfirm = {
                viewModel.discontinueProduct(productId)
                viewModel.hideDiscontinueConfirm()
            }
        )
    }

    if (detailState.showReactivateConfirm) {
        ConfirmDialog(
            title = "Kich hoat san pham",
            message = "San pham se duoc hien thi tro lai trong danh sach.",
            confirmText = "Kich hoat",
            onDismiss = { viewModel.hideReactivateConfirm() },
            onConfirm = {
                viewModel.reactivateProduct(productId)
                viewModel.hideReactivateConfirm()
            }
        )
    }

    if (detailState.showCannotDeleteDialog) {
        com.vinh.dyvat.ui.components.ErrorDialog(
            title = "Khong the xoa san pham",
            message = detailState.cannotDeleteMessage,
            onDismiss = { viewModel.hideCannotDeleteDialog() }
        )
    }

    if (detailState.showDeleteConfirm) {
        ConfirmDialog(
            title = "Xac nhan xoa",
            message = "Ban co chan muon xoa san pham \"${detailState.product?.product?.name}\"? Hanh dong nay khong the hoan tac.",
            confirmText = "Xoa",
            isDestructive = true,
            onDismiss = { viewModel.hideDetailDeleteConfirm() },
            onConfirm = { viewModel.performDeleteProduct(productId) }
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSilver,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                color = TextWhite,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
