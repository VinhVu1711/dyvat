package com.vinh.dyvat.ui.screens.products

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.vinh.dyvat.data.model.ProductStatus
import com.vinh.dyvat.data.model.ProductWithDetails
import com.vinh.dyvat.ui.components.ConfirmDialog
import com.vinh.dyvat.ui.components.ErrorDialog
import com.vinh.dyvat.ui.components.ErrorState
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.components.StatusBadge
import com.vinh.dyvat.ui.components.StatusType
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.NegativeRed
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import com.vinh.dyvat.ui.theme.WarningOrange

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
                        text = "CHI TIẾT SẢN PHẨM",
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
                actions = {
                    if (detailState.product != null) {
                        IconButton(onClick = { onNavigateToEdit(productId) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Sửa sản phẩm",
                                tint = SpotifyGreen
                            )
                        }
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
                onRetry = { viewModel.loadProductDetail(productId) }
            )
            detailState.product != null -> {
                val product = detailState.product!!
                val isDiscontinued = product.product.status == ProductStatus.DISCONTINUED

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

                    item {
                        ProductInfoCard(
                            product = product,
                            isDiscontinued = isDiscontinued
                        )
                    }

                    item {
                        Text(
                            text = "Thông tin chi tiết",
                            color = TextWhite,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        ProductDetailCard(product = product)
                    }

                    item {
                        ProductActions(
                            isDiscontinued = isDiscontinued,
                            onReactivate = { viewModel.showReactivateConfirm() },
                            onDiscontinue = { viewModel.showDiscontinueConfirm() },
                            onDelete = { viewModel.requestDeleteProduct(productId) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (detailState.showDiscontinueConfirm) {
        ConfirmDialog(
            title = "Ngừng kinh doanh",
            message = "Sản phẩm sẽ bị ẩn khỏi danh sách tạo phiếu. Bạn vẫn có thể kích hoạt lại sau.",
            confirmText = "Ngừng kinh doanh",
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
            title = "Kích hoạt sản phẩm",
            message = "Sản phẩm sẽ được hiển thị trở lại trong danh sách.",
            confirmText = "Kích hoạt",
            onDismiss = { viewModel.hideReactivateConfirm() },
            onConfirm = {
                viewModel.reactivateProduct(productId)
                viewModel.hideReactivateConfirm()
            }
        )
    }

    if (detailState.showCannotDeleteDialog) {
        ErrorDialog(
            title = "Không thể xóa sản phẩm",
            message = detailState.cannotDeleteMessage,
            onDismiss = { viewModel.hideCannotDeleteDialog() }
        )
    }

    if (detailState.showDeleteConfirm) {
        ConfirmDialog(
            title = "Xác nhận xóa",
            message = "Bạn có chắc muốn xóa sản phẩm \"${detailState.product?.product?.name}\"? Hành động này không thể hoàn tác.",
            confirmText = "Xóa",
            isDestructive = true,
            onDismiss = { viewModel.hideDetailDeleteConfirm() },
            onConfirm = { viewModel.performDeleteProduct(productId) }
        )
    }
}

@Composable
private fun ProductInfoCard(
    product: ProductWithDetails,
    isDiscontinued: Boolean
) {
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
                text = "Mã sản phẩm: ${product.product.code.ifEmpty { "-" }}",
                color = TextWhite,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tên sản phẩm: ${product.product.name}",
                color = TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Trạng thái: ",
                    color = TextWhite,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isDiscontinued) {
                    StatusBadge(
                        label = "Ngừng kinh doanh",
                        type = StatusType.WARNING
                    )
                } else {
                    StatusBadge(
                        label = "Đang kinh doanh",
                        type = StatusType.ACTIVE
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = TextSilver.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Giá nhập / 1 đơn vị",
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
                        text = "Giá bán / 1 đơn vị",
                        color = TextSilver,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = product.product.defaultSalePriceVnd.toVnd(),
                        color = if (isDiscontinued) TextSilver else SpotifyGreen,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailCard(product: ProductWithDetails) {
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
            DetailRow(
                icon = Icons.Default.Category,
                label = "Loại sản phẩm",
                value = product.categoryName.ifEmpty { "-" }
            )
            DetailRow(
                icon = Icons.Default.Straighten,
                label = "Đơn vị tính",
                value = product.unitName.ifEmpty { "-" }
            )
            DetailRow(
                icon = Icons.Default.LocalShipping,
                label = "Nhà cung cấp",
                value = product.supplierName.ifEmpty { "-" }
            )
            DetailRow(
                icon = Icons.Default.Inventory2,
                label = "Mã sản phẩm",
                value = product.product.code.ifEmpty { "-" }
            )
        }
    }
}

@Composable
private fun ProductActions(
    isDiscontinued: Boolean,
    onReactivate: () -> Unit,
    onDiscontinue: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isDiscontinued) {
            Button(
                onClick = onReactivate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = NearBlack,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kích hoạt lại sản phẩm",
                    color = NearBlack,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            OutlinedButton(
                onClick = onDiscontinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange)
            ) {
                Text(
                    text = "Ngừng kinh doanh",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NegativeRed)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Xóa vĩnh viễn",
                fontWeight = FontWeight.SemiBold
            )
        }
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
