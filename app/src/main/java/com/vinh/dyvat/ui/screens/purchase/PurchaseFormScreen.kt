package com.vinh.dyvat.ui.screens.purchase

import android.util.Log
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vinh.dyvat.data.model.ProductWithDetails
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.ui.components.ConfirmDialog
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.navigation.Screen
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFormScreen(
    onNavigateBack: () -> Unit,
    navController: NavController,
    viewModel: PurchaseViewModel
) {
    val formState by viewModel.formState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }

    // Handle result from AddPurchaseItemScreen via SavedStateHandle
    LaunchedEffect(Unit) {
        Log.d("PurchaseFormScreen", "LaunchedEffect(Unit): checking savedStateHandle")
        navController.currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
            val productId = savedStateHandle.get<String>("added_product_id")
            if (productId != null) {
                Log.d("PurchaseFormScreen", "LaunchedEffect: found productId=$productId in savedStateHandle")
                val productName = savedStateHandle.get<String>("added_product_name") ?: ""
                val supplierId = savedStateHandle.get<String>("added_supplier_id") ?: ""
                val supplierName = savedStateHandle.get<String>("added_supplier_name") ?: ""
                val unitId = savedStateHandle.get<String>("added_unit_id") ?: ""
                val unitName = savedStateHandle.get<String>("added_unit_name") ?: ""
                val quantity = savedStateHandle.get<String>("added_quantity") ?: ""
                val expiryDate = savedStateHandle.get<String>("added_expiry_date") ?: ""
                val price = savedStateHandle.get<String>("added_price") ?: ""

                Log.d("PurchaseFormScreen", "LaunchedEffect: availableProducts count=${formState.availableProducts.size}, trying to find productId=$productId")

                // Find the actual ProductWithDetails from availableProducts
                val productWithDetails = formState.availableProducts.find { it.product.id == productId }
                val supplier = formState.suppliers.find { it.id == supplierId }

                if (productWithDetails != null) {
                    Log.d("PurchaseFormScreen", "LaunchedEffect: found product=${productWithDetails.product.name}, calling addFormItemWithDetails")
                    viewModel.addFormItemWithDetails(productWithDetails, supplier, quantity, expiryDate, price)
                    Log.d("PurchaseFormScreen", "LaunchedEffect: addFormItemWithDetails completed, items count=${formState.items.size}")
                } else {
                    Log.w("PurchaseFormScreen", "LaunchedEffect: product not found in availableProducts! productId=$productId")
                }

                // Clear saved state
                savedStateHandle.remove<String>("added_product_id")
                savedStateHandle.remove<String>("added_product_name")
                savedStateHandle.remove<String>("added_supplier_id")
                savedStateHandle.remove<String>("added_supplier_name")
                savedStateHandle.remove<String>("added_unit_id")
                savedStateHandle.remove<String>("added_unit_name")
                savedStateHandle.remove<String>("added_quantity")
                savedStateHandle.remove<String>("added_expiry_date")
                savedStateHandle.remove<String>("added_price")
                Log.d("PurchaseFormScreen", "LaunchedEffect: savedStateHandle cleared")
            } else {
                Log.d("PurchaseFormScreen", "LaunchedEffect: no productId in savedStateHandle")
            }
        }
    }

    // Only call initForm when not yet initialized (fix race condition)
    LaunchedEffect(formState.isInitialized) {
        Log.d("PurchaseFormScreen", "LaunchedEffect(isInitialized): isInitialized=${formState.isInitialized}, availableProducts=${formState.availableProducts.size}")
        if (!formState.isInitialized && formState.availableProducts.isEmpty()) {
            Log.d("PurchaseFormScreen", "LaunchedEffect(isInitialized): calling initForm()")
            viewModel.initForm()
        } else {
            Log.d("PurchaseFormScreen", "LaunchedEffect(isInitialized): skipping initForm() - already initialized or products available")
        }
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TẠO PHIẾU NHẬP",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NearBlack)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Ngày nhập hàng
            item {
                Column {
                    Text(
                        text = "Ngày nhập hàng *",
                        color = TextSilver,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DateField(
                        date = formState.purchaseDate,
                        onClick = { showDatePicker = true }
                    )
                }
            }

            // Danh sách sản phẩm header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TextSilver.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Danh sách sản phẩm nhập",
                    color = TextWhite,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Product items
            if (formState.items.isEmpty()) {
                item {
                    EmptyProductList(
                        onAddClick = {
                            navController.navigate(Screen.AddPurchaseItem.createRoute(formState.purchaseDate))
                        }
                    )
                }
            } else {
                itemsIndexed(
                    items = formState.items,
                    key = { _, item -> item.id }
                ) { _, item ->
                    PurchaseItemCard(
                        item = item,
                        onClick = { /* TODO: Edit item */ }
                    )
                }
            }

            // Add product button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AddProductButton(
                    onClick = {
                        navController.navigate(Screen.AddPurchaseItem.createRoute(formState.purchaseDate))
                    }
                )
            }

            // Tổng tiền
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TextSilver.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Text(
                        text = "Tổng tiền nhập tất cả sản phẩm",
                        color = TextSilver,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = formState.totalAmount.toVnd(),
                        onValueChange = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = SpotifyGreen,
                            disabledBorderColor = SpotifyGreen.copy(alpha = 0.5f),
                            disabledContainerColor = DarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Text(
                                text = "Tự tính / Khóa",
                                color = TextSilver.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )
                }
            }

            // Error message
            formState.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Save button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SaveButton(
                    onClick = { showSaveConfirmDialog = true },
                    isEnabled = !formState.isSaving && formState.items.isNotEmpty(),
                    isLoading = formState.isSaving
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(formState.purchaseDate)?.time
                    ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(millis))
                            viewModel.updatePurchaseDate(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Chọn", color = SpotifyGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy", color = TextSilver)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Save confirm dialog
    if (showSaveConfirmDialog) {
        ConfirmDialog(
            title = "Xác nhận lưu phiếu nhập",
            message = "Bạn có muốn lưu phiếu nhập hàng này không?",
            confirmText = "Lưu phiếu nhập",
            onDismiss = { showSaveConfirmDialog = false },
            onConfirm = {
                showSaveConfirmDialog = false
                // Only call saveTicket if not already saving
                if (!formState.isSaving) {
                    viewModel.saveTicket {
                        onNavigateBack()
                    }
                }
            }
        )
    }
}

@Composable
private fun DateField(
    date: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (date.isNotEmpty()) date else "Chọn ngày",
            color = if (date.isNotEmpty()) TextWhite else TextSilver.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = "Chọn ngày",
            tint = SpotifyGreen,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyProductList(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = TextSilver,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Chưa có sản phẩm nào",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bấm \"Thêm sản phẩm nhập\" để bắt đầu",
                    color = TextSilver.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PurchaseItemCard(
    item: PurchaseItemDraftUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Tên sản phẩm
            Text(
                text = item.productName.ifEmpty { "Chọn sản phẩm..." },
                color = if (item.productName.isEmpty()) TextSilver else TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (item.productName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                // Nhà cung cấp
                if (item.supplierName.isNotEmpty()) {
                    Text(
                        text = "Nhà cung cấp: ${item.supplierName}",
                        color = TextSilver,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Đơn vị tính
                Text(
                    text = "Đơn vị tính: ${item.unitName}",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Ngày hết hạn
                if (item.expiryDate.isNotEmpty()) {
                    Text(
                        text = "Ngày hết hạn: ${item.expiryDate}",
                        color = if (item.expiryDateError != null) MaterialTheme.colorScheme.error else TextSilver,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    item.expiryDateError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else if (item.expiryDateError != null) {
                    Text(
                        text = "Ngày hết hạn: (trống)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = item.expiryDateError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Số lượng nhập
                Text(
                    text = "Số lượng nhập: ${item.quantity}",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Giá nhập / 1 đơn vị
                Text(
                    text = "Giá nhập / 1 đơn vị: ${item.purchasePrice.toLongOrNull()?.toVnd() ?: "0"}",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TextSilver.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                // Tổng tiền nhập sản phẩm này
                Text(
                    text = "Tổng tiền nhập sản phẩm này: ${item.lineTotal.toVnd()}",
                    color = SpotifyGreen,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AddProductButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = SpotifyGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Thêm sản phẩm nhập",
            color = SpotifyGreen,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SaveButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    isLoading: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = SpotifyGreen,
            disabledContainerColor = SpotifyGreen.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = NearBlack,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = NearBlack
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Lưu phiếu nhập",
                color = NearBlack,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
