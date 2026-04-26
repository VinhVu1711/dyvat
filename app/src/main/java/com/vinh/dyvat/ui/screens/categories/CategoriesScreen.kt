package com.vinh.dyvat.ui.screens.categories

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.ui.components.AddEditDialog
import com.vinh.dyvat.ui.components.ConfirmDeleteDialog
import com.vinh.dyvat.ui.components.ItemCard
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quản lý loại sản phẩm",
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
                uiState.categories.isEmpty() -> {
                    EmptyState(
                        message = "Chưa có loại sản phẩm nào.\nNhấn + để thêm mới."
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.categories) { category ->
                            ItemCard(
                                name = category.name,
                                onEdit = { viewModel.showEditDialog(category) },
                                onDelete = { viewModel.deleteCategory(category.id) }
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
            val editingCategory = uiState.editingCategory
            AddEditDialog(
                title = if (editingCategory != null) "Sửa loại sản phẩm" else "Thêm loại sản phẩm",
                label = "Tên loại sản phẩm",
                initialValue = editingCategory?.name ?: "",
                onDismiss = { viewModel.hideDialog() },
                onConfirm = { name ->
                    if (editingCategory != null) {
                        viewModel.updateCategory(editingCategory.id, name)
                    } else {
                        viewModel.addCategory(name)
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
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = message,
                color = TextSilver,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
