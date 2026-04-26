package com.vinh.dyvat.ui.screens.units

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
import com.vinh.dyvat.ui.components.AddEditDialog
import com.vinh.dyvat.ui.components.ItemCard
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.theme.DarkSurface
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

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quản lý đơn vị tính",
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
                uiState.units.isEmpty() -> {
                    EmptyState(
                        message = "Chưa có đơn vị tính nào.\nNhấn + để thêm mới."
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.units) { unit ->
                            ItemCard(
                                name = unit.name,
                                onEdit = { viewModel.showEditDialog(unit) },
                                onDelete = { viewModel.deleteUnit(unit.id) }
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
            val editingUnit = uiState.editingUnit
            AddEditDialog(
                title = if (editingUnit != null) "Sửa đơn vị tính" else "Thêm đơn vị tính",
                label = "Tên đơn vị tính",
                initialValue = editingUnit?.name ?: "",
                onDismiss = { viewModel.hideDialog() },
                onConfirm = { name ->
                    if (editingUnit != null) {
                        viewModel.updateUnit(editingUnit.id, name)
                    } else {
                        viewModel.addUnit(name)
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
