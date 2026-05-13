package com.vinh.dyvat.ui.screens.suppliers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.ui.components.ConfirmDialog
import com.vinh.dyvat.ui.components.DyvatSearchBar
import com.vinh.dyvat.ui.components.EmptyState
import com.vinh.dyvat.ui.components.ErrorState
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.theme.BorderGray
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    onNavigateBack: () -> Unit,
    viewModel: SuppliersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = NearBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Nhà cung cấp", color = TextWhite, fontWeight = FontWeight.Bold)
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = SpotifyGreen,
                contentColor = NearBlack
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm nhà cung cấp")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NearBlack)
                .padding(innerPadding)
        ) {
            SupplierHeader(
                count = uiState.visibleSuppliers.size,
                showInactive = uiState.showInactive,
                searchQuery = uiState.searchQuery,
                onSearchChange = viewModel::setSearchQuery,
                onShowInactiveChange = viewModel::setShowInactive
            )

            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorState(
                    message = uiState.error ?: "Không thể tải nhà cung cấp",
                    onRetry = { viewModel.loadSuppliers() }
                )
                uiState.visibleSuppliers.isEmpty() -> EmptyState(
                    icon = Icons.Default.Business,
                    title = if (uiState.searchQuery.isBlank()) {
                        if (uiState.showInactive) "Chưa có nhà cung cấp đã ẩn" else "Chưa có nhà cung cấp"
                    } else {
                        "Không tìm thấy nhà cung cấp"
                    },
                    subtitle = if (uiState.showInactive) {
                        "Các nhà cung cấp đã ngừng dùng sẽ xuất hiện ở đây để bạn khôi phục khi cần."
                    } else {
                        "Thêm nhà cung cấp để dùng khi tạo sản phẩm và phiếu nhập."
                    }
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 166.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.visibleSuppliers, key = { it.id }) { supplier ->
                        SupplierCard(
                            supplier = supplier,
                            productCount = uiState.productCountsBySupplier[supplier.id] ?: 0,
                            isSaving = uiState.isSaving,
                            onEdit = { viewModel.showEditDialog(supplier) },
                            onDeactivate = { viewModel.showDeactivateDialog(supplier) },
                            onRestore = { viewModel.restoreSupplier(supplier.id) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        SupplierFormDialog(
            supplier = uiState.editingSupplier,
            isSaving = uiState.isSaving,
            onDismiss = { viewModel.hideDialog() },
            onConfirm = { name, phone ->
                val editing = uiState.editingSupplier
                val normalizedPhone = phone.trim().ifBlank { null }
                if (editing != null) {
                    viewModel.updateSupplier(editing.id, name, normalizedPhone)
                } else {
                    viewModel.addSupplier(name, normalizedPhone)
                }
            }
        )
    }

    uiState.supplierToDeactivate?.let { supplier ->
        val productCount = uiState.productCountsBySupplier[supplier.id] ?: 0
        ConfirmDialog(
            title = "Ngừng dùng nhà cung cấp",
            message = buildDeactivateMessage("Nhà cung cấp", supplier.name, productCount),
            confirmText = "Ngừng dùng",
            dismissText = "Hủy",
            onDismiss = { viewModel.hideDeactivateDialog() },
            onConfirm = { viewModel.deactivateSupplier(supplier.id) },
            isDestructive = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierHeader(
    count: Int,
    showInactive: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onShowInactiveChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "$count mục ${if (showInactive) "đã ẩn" else "đang dùng"}",
            color = TextSilver,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        DyvatSearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = "Tìm kiếm nhà cung cấp..."
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showInactive,
                onClick = { onShowInactiveChange(false) },
                label = { Text("Đang dùng") },
                colors = supplierChipColors()
            )
            FilterChip(
                selected = showInactive,
                onClick = { onShowInactiveChange(true) },
                label = { Text("Đã ẩn") },
                colors = supplierChipColors()
            )
        }
    }
}

@Composable
private fun SupplierCard(
    supplier: Supplier,
    productCount: Int,
    isSaving: Boolean,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = supplier.name,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = supplier.phone?.takeIf { it.isNotBlank() } ?: "Chưa có số điện thoại",
                color = TextSilver,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (supplier.isActive) "$productCount sản phẩm" else "Đã ẩn",
                color = TextSilver,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (supplier.isActive) {
                    IconButton(onClick = onEdit, enabled = !isSaving) {
                        Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = TextSilver)
                    }
                    IconButton(onClick = onDeactivate, enabled = !isSaving) {
                        Icon(Icons.Default.Archive, contentDescription = "Ngừng dùng", tint = TextSilver)
                    }
                } else {
                    TextButton(onClick = onRestore, enabled = !isSaving) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = SpotifyGreen)
                        Text("Khôi phục", color = SpotifyGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierFormDialog(
    supplier: Supplier?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember(supplier?.id) { mutableStateOf(supplier?.name.orEmpty()) }
    var phone by remember(supplier?.id) { mutableStateOf(supplier?.phone.orEmpty()) }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text = if (supplier == null) "Thêm nhà cung cấp" else "Sửa nhà cung cấp",
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên nhà cung cấp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = supplierTextFieldColors()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    placeholder = { Text("Không bắt buộc") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = supplierTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trimmedName, phone) },
                enabled = trimmedName.isNotEmpty() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = NearBlack)
            ) {
                Text(if (isSaving) "Đang lưu..." else "Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Hủy", color = TextSilver)
            }
        }
    )
}

private fun buildDeactivateMessage(entityLabel: String, name: String, productCount: Int): String {
    val usageWarning = if (productCount > 0) {
        " Hiện có $productCount sản phẩm đang dùng mục này."
    } else {
        ""
    }
    return "$entityLabel \"$name\" sẽ bị ẩn khỏi danh sách mặc định và dropdown tạo dữ liệu mới.$usageWarning Lịch sử cũ vẫn được giữ nguyên."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun supplierChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = SpotifyGreen,
    selectedLabelColor = NearBlack,
    containerColor = MidDark,
    labelColor = TextSilver
)

@Composable
private fun supplierTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedBorderColor = SpotifyGreen,
    unfocusedBorderColor = BorderGray,
    focusedLabelColor = SpotifyGreen,
    unfocusedLabelColor = TextSilver,
    cursorColor = SpotifyGreen,
    focusedContainerColor = MidDark,
    unfocusedContainerColor = MidDark
)
