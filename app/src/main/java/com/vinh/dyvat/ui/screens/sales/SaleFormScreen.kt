package com.vinh.dyvat.ui.screens.sales

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
import androidx.compose.material3.SelectableDates
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
import androidx.navigation.NavController
import com.vinh.dyvat.data.model.AvailableLot
import com.vinh.dyvat.ui.components.ConfirmDialog
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.navigation.Screen
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleFormScreen(
    onNavigateBack: () -> Unit,
    onTicketSaved: () -> Unit = onNavigateBack,
    navController: NavController,
    viewModel: SaleViewModel
) {
    val formState by viewModel.formState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<SaleItemDraftUi?>(null) }
    val itemResultVersion by navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow("sale_item_result_version", 0L)
        ?.collectAsState()
        ?: remember { mutableStateOf(0L) }

    LaunchedEffect(itemResultVersion, formState.availableProducts) {
        if (itemResultVersion == 0L) return@LaunchedEffect

        navController.currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
            val editedItemId = savedStateHandle.get<Int>("edited_sale_item_id")
            val addedProductId = savedStateHandle.get<String>("added_sale_product_id")
            val productId = savedStateHandle.get<String>("edited_sale_product_id") ?: addedProductId
            if (productId == null) return@let

            val purchaseItemId = savedStateHandle.get<String>("edited_sale_purchase_item_id")
                ?: savedStateHandle.get<String>("added_sale_purchase_item_id")
                ?: ""
            val lotCode = savedStateHandle.get<String>("edited_sale_lot_code")
                ?: savedStateHandle.get<String>("added_sale_lot_code")
                ?: ""
            val expiryDate = savedStateHandle.get<String>("edited_sale_expiry_date")
                ?: savedStateHandle.get<String>("added_sale_expiry_date")
            val quantityRemaining = savedStateHandle.get<Int>("edited_sale_quantity_remaining")
                ?: savedStateHandle.get<Int>("added_sale_quantity_remaining")
                ?: 0
            val purchasePrice = savedStateHandle.get<Long>("edited_sale_purchase_price")
                ?: savedStateHandle.get<Long>("added_sale_purchase_price")
                ?: 0L
            val quantity = savedStateHandle.get<String>("edited_sale_quantity")
                ?: savedStateHandle.get<String>("added_sale_quantity")
                ?: ""
            val price = savedStateHandle.get<String>("edited_sale_price")
                ?: savedStateHandle.get<String>("added_sale_price")
                ?: ""
            val product = formState.availableProducts.find { it.product.id == productId }
            if (product == null) {
                Log.w("SaleFormScreen", "Item result pending: product not found, productId=$productId")
                return@let
            }
            val lot = AvailableLot(
                purchaseItemId = purchaseItemId,
                lotCode = lotCode,
                expiryDate = expiryDate,
                quantityRemaining = quantityRemaining,
                purchasePriceVnd = purchasePrice
            )

            if (editedItemId != null) {
                viewModel.updateFormItemWithDetails(editedItemId, product, lot, quantity, price)
            } else {
                viewModel.addFormItemWithDetails(product, lot, quantity, price)
            }

            clearSaleItemSavedState(savedStateHandle)
        }
    }

    LaunchedEffect(formState.isInitialized) {
        if (!formState.isInitialized && formState.availableProducts.isEmpty()) {
            viewModel.initForm()
        }
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TẠO PHIẾU BÁN",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = TextWhite)
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
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Column {
                    Text("Ngày bán hàng *", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    SaleDateField(date = formState.saleDate, onClick = { showDatePicker = true })
                    formState.saleDateError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TextSilver.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Danh sách sản phẩm bán",
                    color = TextWhite,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (formState.items.isEmpty()) {
                item {
                    EmptySaleItemList(
                        onAddClick = { navController.navigate(Screen.AddSaleItem.createRoute(formState.saleDate)) }
                    )
                }
            } else {
                itemsIndexed(items = formState.items, key = { _, item -> item.id }) { _, item ->
                    SaleDraftItemCard(
                        item = item,
                        onEdit = { navController.navigate(Screen.EditSaleItem.createRoute(item.id, formState.saleDate)) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                AddSaleItemButton(onClick = { navController.navigate(Screen.AddSaleItem.createRoute(formState.saleDate)) })
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TextSilver.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Tổng tiền bán tất cả sản phẩm", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.totalRevenue.toVnd(),
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

            formState.error?.let { error ->
                item {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SaleSaveButton(
                    onClick = {
                        if (viewModel.validateSaleDateForSave()) {
                            showSaveConfirmDialog = true
                        }
                    },
                    isEnabled = !formState.isSaving && formState.items.isNotEmpty(),
                    isLoading = formState.isSaving
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(formState.saleDate)?.time
                    ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.updateSaleDate(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)))
                    }
                    showDatePicker = false
                }) { Text("Chọn", color = SpotifyGreen) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy", color = TextSilver) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    itemToDelete?.let { item ->
        ConfirmDialog(
            title = "Xác nhận xóa sản phẩm bán",
            message = "Bạn có muốn xóa \"${item.productName}\" khỏi phiếu bán này không?",
            confirmText = "Xóa",
            dismissText = "Hủy",
            isDestructive = true,
            onDismiss = { itemToDelete = null },
            onConfirm = {
                viewModel.removeFormItem(item.id)
                itemToDelete = null
            }
        )
    }

    if (showSaveConfirmDialog) {
        ConfirmDialog(
            title = "Xác nhận lưu phiếu bán",
            message = "Bạn có muốn lưu phiếu bán hàng này không?",
            confirmText = "Lưu phiếu bán",
            onDismiss = { showSaveConfirmDialog = false },
            onConfirm = {
                showSaveConfirmDialog = false
                if (!formState.isSaving) {
                    viewModel.saveTicket { onTicketSaved() }
                }
            }
        )
    }
}

private fun clearSaleItemSavedState(savedStateHandle: androidx.lifecycle.SavedStateHandle) {
    savedStateHandle.remove<Long>("sale_item_result_version")
    listOf(
        "added_sale_product_id",
        "added_sale_product_name",
        "added_sale_purchase_item_id",
        "added_sale_lot_code",
        "added_sale_expiry_date",
        "added_sale_quantity",
        "added_sale_price",
        "edited_sale_product_id",
        "edited_sale_product_name",
        "edited_sale_purchase_item_id",
        "edited_sale_lot_code",
        "edited_sale_expiry_date",
        "edited_sale_quantity",
        "edited_sale_price"
    ).forEach { savedStateHandle.remove<String>(it) }
    savedStateHandle.remove<Int>("edited_sale_item_id")
    savedStateHandle.remove<Int>("added_sale_quantity_remaining")
    savedStateHandle.remove<Int>("edited_sale_quantity_remaining")
    savedStateHandle.remove<Long>("added_sale_purchase_price")
    savedStateHandle.remove<Long>("edited_sale_purchase_price")
}

@Composable
private fun SaleDateField(date: String, onClick: () -> Unit) {
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
            text = date.ifEmpty { "Chọn ngày" },
            color = if (date.isNotEmpty()) TextWhite else TextSilver.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.CalendarToday, contentDescription = "Chọn ngày", tint = SpotifyGreen, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptySaleItemList(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = TextSilver, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Chưa có sản phẩm nào", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Bấm \"Thêm sản phẩm bán\" để bắt đầu",
                    color = TextSilver.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SaleDraftItemCard(
    item: SaleItemDraftUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = item.productName.ifEmpty { "Chọn sản phẩm..." },
                color = if (item.productName.isEmpty()) TextSilver else TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (item.productName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Đơn vị tính: ${item.unitName}", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Mã lô: ${item.lotCode}", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                item.expiryDate?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ngày hết hạn: $it", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tồn lô: ${item.quantityRemaining}", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Số lượng bán: ${item.quantity}",
                    color = if (item.quantityError != null) MaterialTheme.colorScheme.error else TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                item.quantityError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Giá bán / 1 đơn vị: ${item.salePrice.toLongOrNull()?.toVnd() ?: "0"}",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TextSilver.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tổng giá bán sản phẩm này: ${item.lineRevenue.toVnd()}",
                    color = SpotifyGreen,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Text("Sửa", color = SpotifyGreen, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Xóa", color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSaleItemButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Thêm sản phẩm bán", color = SpotifyGreen, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SaleSaveButton(onClick: () -> Unit, isEnabled: Boolean, isLoading: Boolean) {
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
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NearBlack, strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = NearBlack)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lưu phiếu bán", color = NearBlack, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
