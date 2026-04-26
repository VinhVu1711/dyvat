package com.vinh.dyvat.ui.screens.imports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vinh.dyvat.data.model.*
import com.vinh.dyvat.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditImportDialog(
    item: ImportItem?,
    categories: List<Category>,
    units: List<UnitModel>,
    suppliers: List<Supplier>,
    onDismiss: () -> Unit,
    onSave: (
        productName: String,
        categoryId: String?,
        unitId: String,
        supplierId: String?,
        totalImportAmount: Double,
        unitPrice: Double,
        totalQuantity: Int,
        quantityForSale: Int,
        expiryDate: String?,
        notes: String?
    ) -> Unit
) {
    var productName by remember { mutableStateOf(item?.productName ?: "") }
    var selectedCategoryId by remember { mutableStateOf(item?.categoryId) }
    var selectedUnitId by remember { mutableStateOf(item?.unitId ?: units.firstOrNull()?.id ?: "") }
    var selectedSupplierId by remember { mutableStateOf(item?.supplierId) }
    var totalImportAmount by remember { mutableStateOf(item?.totalImportAmount?.toString() ?: "") }
    var unitPrice by remember { mutableStateOf(item?.unitPrice?.toString() ?: "") }
    var totalQuantity by remember { mutableStateOf(item?.totalQuantity?.toString() ?: "") }
    var quantityForSale by remember { mutableStateOf(item?.quantityForSale?.toString() ?: "0") }
    var expiryDate by remember { mutableStateOf(item?.expiryDate ?: "") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var supplierExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidDark,
        title = {
            Text(
                text = if (item != null) "Sửa phiếu nhập" else "Thêm phiếu nhập",
                color = TextWhite
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Tên sản phẩm *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors()
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name ?: "Chọn loại",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Loại sản phẩm") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it }
                ) {
                    OutlinedTextField(
                        value = units.find { it.id == selectedUnitId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Đơn vị tính *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    selectedUnitId = unit.id
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = it }
                ) {
                    OutlinedTextField(
                        value = suppliers.find { it.id == selectedSupplierId }?.name ?: "Chọn nhà cung cấp",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nhà cung cấp") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = supplierExpanded,
                        onDismissRequest = { supplierExpanded = false }
                    ) {
                        suppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.name) },
                                onClick = {
                                    selectedSupplierId = supplier.id
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = totalImportAmount,
                    onValueChange = { totalImportAmount = it },
                    label = { Text("Tổng tiền nhập (VNĐ) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors()
                )

                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Giá bán 1 đơn vị (VNĐ) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalQuantity,
                        onValueChange = { totalQuantity = it },
                        label = { Text("SL nhập *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors()
                    )
                    OutlinedTextField(
                        value = quantityForSale,
                        onValueChange = { quantityForSale = it },
                        label = { Text("SL bán") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors()
                    )
                }

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Ngày hết hạn (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        productName,
                        selectedCategoryId,
                        selectedUnitId,
                        selectedSupplierId,
                        totalImportAmount.toDoubleOrNull() ?: 0.0,
                        unitPrice.toDoubleOrNull() ?: 0.0,
                        totalQuantity.toIntOrNull() ?: 0,
                        quantityForSale.toIntOrNull() ?: 0,
                        expiryDate.ifBlank { null },
                        notes.ifBlank { null }
                    )
                },
                enabled = productName.isNotBlank() && selectedUnitId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                Text("Lưu", color = NearBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSilver)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusUpdateDialog(
    item: ImportItemWithDetails,
    onDismiss: () -> Unit,
    onUpdate: (ItemStatus, Int?, String?) -> Unit
) {
    val importItem = item.importItem
    var selectedStatus by remember { mutableStateOf(importItem.status) }
    var quantityForSale by remember { mutableStateOf(importItem.quantityForSale.toString()) }
    var saleLocation by remember { mutableStateOf(importItem.saleLocation ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidDark,
        title = {
            Text(
                text = "Cập nhật trạng thái",
                color = TextWhite
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = importItem.productName,
                    color = TextWhite,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Tổng số lượng: ${importItem.totalQuantity}",
                    color = TextSilver
                )

                Column {
                    Text(
                        text = "Trạng thái:",
                        color = TextSilver,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ItemStatus.entries.forEach { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = {
                                    Text(
                                        when (status) {
                                            ItemStatus.IN_STOCK -> "Trong kho"
                                            ItemStatus.FOR_SALE -> "Đem ra bán"
                                        }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpotifyGreen,
                                    selectedLabelColor = NearBlack,
                                    containerColor = DarkSurface,
                                    labelColor = TextSilver
                                )
                            )
                        }
                    }
                }

                if (selectedStatus == ItemStatus.FOR_SALE) {
                    OutlinedTextField(
                        value = quantityForSale,
                        onValueChange = { quantityForSale = it },
                        label = { Text("Số lượng đem ra bán") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = saleLocation,
                        onValueChange = { saleLocation = it },
                        label = { Text("Vị trí bán (tùy chọn)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )
                }

                val qtyForSale = quantityForSale.toIntOrNull() ?: 0
                val qtyInStock = importItem.totalQuantity - qtyForSale
                Text(
                    text = "Số lượng tồn kho: $qtyInStock",
                    color = if (qtyInStock < 0) NegativeRed else TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdate(
                        selectedStatus,
                        if (selectedStatus == ItemStatus.FOR_SALE) quantityForSale.toIntOrNull() else null,
                        if (selectedStatus == ItemStatus.FOR_SALE) saleLocation.ifBlank { null } else null
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                Text("Cập nhật", color = NearBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextSilver)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedBorderColor = SpotifyGreen,
    unfocusedBorderColor = TextSilver,
    focusedLabelColor = SpotifyGreen,
    unfocusedLabelColor = TextSilver,
    cursorColor = SpotifyGreen,
    focusedContainerColor = NearBlack,
    unfocusedContainerColor = NearBlack
)
