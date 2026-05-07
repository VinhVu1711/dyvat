package com.vinh.dyvat.ui.screens.units

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Straighten
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.data.model.UnitModel
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
fun UnitsScreen(
    onNavigateBack: () -> Unit,
    viewModel: UnitsViewModel = hiltViewModel()
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
                    Text("Đơn vị tính", color = TextWhite, fontWeight = FontWeight.Bold)
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
                Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm đơn vị tính")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NearBlack)
                .padding(innerPadding)
        ) {
            UnitHeader(
                count = uiState.visibleUnits.size,
                showInactive = uiState.showInactive,
                searchQuery = uiState.searchQuery,
                onSearchChange = viewModel::setSearchQuery,
                onShowInactiveChange = viewModel::setShowInactive
            )

            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorState(
                    message = uiState.error ?: "Không thể tải đơn vị tính",
                    onRetry = { viewModel.loadUnits() }
                )
                uiState.visibleUnits.isEmpty() -> EmptyState(
                    icon = Icons.Default.Straighten,
                    title = if (uiState.searchQuery.isBlank()) {
                        if (uiState.showInactive) "Chưa có đơn vị đã ẩn" else "Chưa có đơn vị tính"
                    } else {
                        "Không tìm thấy đơn vị tính"
                    },
                    subtitle = if (uiState.showInactive) {
                        "Các đơn vị đã ngừng dùng sẽ xuất hiện ở đây để bạn khôi phục khi cần."
                    } else {
                        "Thêm đơn vị như hộp, chai, lon để dùng khi tạo sản phẩm."
                    }
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 142.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.visibleUnits, key = { it.id }) { unit ->
                        UnitCard(
                            unit = unit,
                            isSaving = uiState.isSaving,
                            onEdit = { viewModel.showEditDialog(unit) },
                            onDeactivate = { viewModel.showDeactivateDialog(unit) },
                            onRestore = { viewModel.restoreUnit(unit.id) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        UnitFormDialog(
            unit = uiState.editingUnit,
            isSaving = uiState.isSaving,
            onDismiss = { viewModel.hideDialog() },
            onConfirm = { name ->
                val editing = uiState.editingUnit
                if (editing != null) viewModel.updateUnit(editing.id, name) else viewModel.addUnit(name)
            }
        )
    }

    uiState.unitToDeactivate?.let { unit ->
        ConfirmDialog(
            title = "Ngừng dùng đơn vị tính",
            message = "Đơn vị \"${unit.name}\" sẽ bị ẩn khỏi danh sách mặc định và dropdown tạo dữ liệu mới. Lịch sử cũ vẫn được giữ nguyên.",
            confirmText = "Ngừng dùng",
            dismissText = "Hủy",
            onDismiss = { viewModel.hideDeactivateDialog() },
            onConfirm = { viewModel.deactivateUnit(unit.id) },
            isDestructive = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitHeader(
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
            placeholder = "Tìm kiếm đơn vị tính..."
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showInactive,
                onClick = { onShowInactiveChange(false) },
                label = { Text("Đang dùng") },
                colors = unitChipColors()
            )
            FilterChip(
                selected = showInactive,
                onClick = { onShowInactiveChange(true) },
                label = { Text("Đã ẩn") },
                colors = unitChipColors()
            )
        }
    }
}

@Composable
private fun UnitCard(
    unit: UnitModel,
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
                imageVector = Icons.Default.Straighten,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = unit.name,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (unit.isActive) "Đang dùng" else "Đã ẩn",
                color = TextSilver,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (unit.isActive) {
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
private fun UnitFormDialog(
    unit: UnitModel?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(unit?.id) { mutableStateOf(unit?.name.orEmpty()) }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text = if (unit == null) "Thêm đơn vị tính" else "Sửa đơn vị tính",
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên đơn vị tính") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
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
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trimmedName) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun unitChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = SpotifyGreen,
    selectedLabelColor = NearBlack,
    containerColor = MidDark,
    labelColor = TextSilver
)
