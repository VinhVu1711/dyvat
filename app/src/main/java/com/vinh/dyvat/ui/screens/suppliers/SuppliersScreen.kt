package com.vinh.dyvat.ui.screens.suppliers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.ui.components.AddEditDialogWithTwoFields
import com.vinh.dyvat.ui.components.ItemCard
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.theme.DarkSurface
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

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quản lý nhà cung cấp",
                        color = TextWhite
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = SpotifyGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm",
                    tint = NearBlack
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.suppliers.isEmpty() -> {
                    EmptyState(
                        message = "Chưa có nhà cung cấp nào.\nNhấn + để thêm mới."
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.suppliers) { supplier ->
                            ItemCard(
                                name = supplier.name,
                                subtitle = supplier.phone,
                                onEdit = { viewModel.showEditDialog(supplier) },
                                onDelete = { viewModel.deleteSupplier(supplier.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        if (uiState.showAddDialog) {
            val editingSupplier = uiState.editingSupplier
            AddEditDialogWithTwoFields(
                title = if (editingSupplier != null) "Sửa nhà cung cấp" else "Thêm nhà cung cấp",
                label1 = "Tên nhà cung cấp",
                label2 = "Số điện thoại",
                initialValue1 = editingSupplier?.name ?: "",
                initialValue2 = editingSupplier?.phone ?: "",
                onDismiss = { viewModel.hideDialog() },
                onConfirm = { name, phone ->
                    if (editingSupplier != null) {
                        viewModel.updateSupplier(editingSupplier.id, name, phone.ifBlank { null })
                    } else {
                        viewModel.addSupplier(name, phone.ifBlank { null })
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = TextSilver,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
