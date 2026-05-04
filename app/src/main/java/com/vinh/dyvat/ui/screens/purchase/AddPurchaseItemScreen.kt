package com.vinh.dyvat.ui.screens.purchase

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.data.model.Product
import com.vinh.dyvat.data.model.ProductWithDetails
import com.vinh.dyvat.ui.components.DyvatSearchBar
import com.vinh.dyvat.ui.components.toVnd
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

data class ProductData(
    val id: String,
    val name: String,
    val code: String,
    val categoryId: String,
    val categoryName: String,
    val unitId: String,
    val unitName: String,
    val supplierId: String,
    val supplierName: String,
    val defaultPurchasePriceVnd: Long
) {
    fun toProductWithDetails() = ProductWithDetails(
        product = Product(
            id = id,
            name = name,
            code = code,
            categoryId = categoryId,
            unitId = unitId,
            supplierId = supplierId,
            defaultPurchasePriceVnd = defaultPurchasePriceVnd
        ),
        categoryName = categoryName,
        unitName = unitName,
        supplierName = supplierName
    )
}

data class SupplierData(
    val id: String,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseItemScreen(
    purchaseDate: String,
    suppliersData: List<Map<String, Any?>>,
    availableProductsData: List<Map<String, Any?>>,
    editingItem: PurchaseItemDraftUi? = null,
    onProductAdded: (
        productId: String,
        productName: String,
        supplierId: String,
        supplierName: String,
        unitId: String,
        unitName: String,
        quantity: String,
        expiryDate: String,
        price: String
    ) -> Unit,
    onProductEdited: (
        itemId: Int,
        productId: String,
        productName: String,
        supplierId: String,
        supplierName: String,
        unitId: String,
        unitName: String,
        quantity: String,
        expiryDate: String,
        price: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onNavigateBack: () -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    val isEditMode = editingItem != null
    val availableProducts = remember(availableProductsData) {
        availableProductsData.mapNotNull { map ->
            ProductData(
                id = map["id"] as? String ?: return@mapNotNull null,
                name = map["name"] as? String ?: "",
                code = map["code"] as? String ?: "",
                categoryId = map["categoryId"] as? String ?: "",
                categoryName = map["categoryName"] as? String ?: "",
                unitId = map["unitId"] as? String ?: "",
                unitName = map["unitName"] as? String ?: "",
                supplierId = map["supplierId"] as? String ?: "",
                supplierName = map["supplierName"] as? String ?: "",
                defaultPurchasePriceVnd = (map["defaultPurchasePriceVnd"] as? Number)?.toLong() ?: 0L
            )
        }
    }
    val suppliers = remember(suppliersData) {
        suppliersData.mapNotNull { map ->
            SupplierData(
                id = map["id"] as? String ?: return@mapNotNull null,
                name = map["name"] as? String ?: ""
            )
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedProductData by remember { mutableStateOf<ProductData?>(null) }
    var selectedSupplierId by remember { mutableStateOf<String?>(null) }
    var selectedSupplierName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var productError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var expiryDateError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingItem?.id, availableProducts, suppliers) {
        editingItem?.let { item ->
            selectedProductData = availableProducts.find { it.id == item.productId }
            selectedSupplierId = item.supplierId.ifBlank { null }
            selectedSupplierName = item.supplierName.ifBlank {
                suppliers.find { it.id == item.supplierId }?.name ?: ""
            }
            quantity = item.quantity
            expiryDate = item.expiryDate
            purchasePrice = item.purchasePrice
        }
    }

    val lineTotal by remember {
        derivedStateOf {
            val qty = quantity.toLongOrNull() ?: 0L
            val price = purchasePrice.toLongOrNull() ?: 0L
            qty * price
        }
    }
    val filteredProducts = if (searchQuery.isBlank()) {
        availableProducts
    } else {
        availableProducts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    fun selectProduct(product: ProductData) {
        selectedProductData = product
        productError = null
        purchasePrice = product.defaultPurchasePriceVnd.toString()
        if (product.supplierName.isNotEmpty()) {
            selectedSupplierId = product.supplierId
            selectedSupplierName = product.supplierName
        }
    }

    fun validateExpiryDate(expiry: String, purchase: String): String? {
        if (expiry.isBlank()) return "Ngày hết hạn là bắt buộc"
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val expiryDateObj = inputFormat.parse(expiry)
            val purchaseDateObj = inputFormat.parse(purchase)
            if (expiryDateObj != null && purchaseDateObj != null && expiryDateObj <= purchaseDateObj) {
                "Ngày hết hạn phải lớn hơn ngày nhập hàng"
            } else {
                null
            }
        } catch (_: Exception) {
            "Ngày hết hạn không hợp lệ"
        }
    }

    fun validateAll(): Boolean {
        var isValid = true
        if (selectedProductData == null) {
            productError = "Vui lòng chọn sản phẩm"
            isValid = false
        } else {
            productError = null
        }
        if (quantity.isBlank() || quantity.toIntOrNull()?.let { it > 0 } != true) {
            quantityError = "Số lượng phải lớn hơn 0"
            isValid = false
        } else {
            quantityError = null
        }
        expiryDateError = validateExpiryDate(expiryDate, purchaseDate)
        if (expiryDateError != null) isValid = false
        if (purchasePrice.isBlank() || purchasePrice.toLongOrNull()?.let { it >= 0 } != true) {
            priceError = "Giá nhập không hợp lệ"
            isValid = false
        } else {
            priceError = null
        }
        return isValid
    }

    fun submitItem() {
        if (!validateAll()) return
        val product = selectedProductData ?: return
        val supplierId = selectedSupplierId ?: ""
        val submitSupplierName = selectedSupplierName.ifBlank {
            suppliers.find { it.id == supplierId }?.name ?: ""
        }

        if (editingItem != null) {
            onProductEdited(
                editingItem.id,
                product.id,
                product.name,
                supplierId,
                submitSupplierName,
                product.unitId,
                product.unitName,
                quantity,
                expiryDate,
                purchasePrice
            )
        } else {
            onProductAdded(
                product.id,
                product.name,
                supplierId,
                submitSupplierName,
                product.unitId,
                product.unitName,
                quantity,
                expiryDate,
                purchasePrice
            )
        }
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "SỬA SẢN PHẨM NHẬP" else "THÊM SẢN PHẨM NHẬP",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tên sản phẩm *", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))

            if (selectedProductData == null) {
                DyvatSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Tìm sản phẩm...",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                productError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Không có sản phẩm", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(filteredProducts) { product ->
                            ProductPickRow(
                                product = product,
                                isSelected = selectedProductData?.id == product.id,
                                onClick = { selectProduct(product) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            } else {
                SelectedProductCard(
                    product = selectedProductData!!,
                    onChange = {
                        selectedProductData = null
                        selectedSupplierId = null
                        selectedSupplierName = ""
                        searchQuery = ""
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Nhà cung cấp", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            SupplierDropdownSimple(
                suppliers = suppliers,
                selectedSupplierId = selectedSupplierId,
                onSupplierSelected = { supplier ->
                    selectedSupplierId = supplier?.id
                    selectedSupplierName = supplier?.name ?: ""
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Đơn vị tính", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = selectedProductData?.unitName ?: "",
                onValueChange = {},
                enabled = false,
                placeholder = { Text("Chọn sản phẩm để tự động điền", color = TextSilver.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = TextWhite,
                    disabledBorderColor = MidDark,
                    disabledContainerColor = DarkCard
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            NumberField(
                label = "Số lượng nhập *",
                value = quantity,
                error = quantityError,
                onValueChange = {
                    quantity = it.filter { c -> c.isDigit() }
                    quantityError = null
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ngày hết hạn *",
                color = if (expiryDateError != null) MaterialTheme.colorScheme.error else TextSilver,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            DateFieldClickable(
                date = expiryDate,
                onClick = { showDatePicker = true },
                isError = expiryDateError != null
            )
            expiryDateError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            NumberField(
                label = "Giá nhập cho 1 đơn vị *",
                value = purchasePrice,
                error = priceError,
                onValueChange = {
                    purchasePrice = it.filter { c -> c.isDigit() }
                    priceError = null
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = TextSilver.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tổng tiền nhập sản phẩm này", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = lineTotal.toVnd(),
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
                        "Tự tính",
                        color = TextSilver.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hủy", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    onClick = { submitItem() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = NearBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditMode) "Lưu thay đổi" else "Thêm",
                        color = NearBlack,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            expiryDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(millis))
                            expiryDateError = null
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
}

@Composable
private fun ProductPickRow(
    product: ProductData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SpotifyGreen.copy(alpha = 0.1f) else DarkSurface)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = product.name,
                color = if (isSelected) SpotifyGreen else TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        if (product.categoryName.isNotEmpty()) {
            Text(
                text = product.categoryName,
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = if (isSelected) 24.dp else 0.dp)
            )
        }
        if (product.defaultPurchasePriceVnd > 0) {
            Text(
                text = "Giá: ${product.defaultPurchasePriceVnd.toVnd()}",
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = if (isSelected) 24.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun SelectedProductCard(
    product: ProductData,
    onChange: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onChange),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, color = SpotifyGreen, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Giá: ${product.defaultPurchasePriceVnd.toVnd()}", color = TextSilver, style = MaterialTheme.typography.bodySmall)
            }
            Text("Đổi", color = TextSilver, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit
) {
    Text(
        text = label,
        color = if (error != null) MaterialTheme.colorScheme.error else TextSilver,
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("0", color = TextSilver.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = error != null,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedBorderColor = SpotifyGreen,
            unfocusedBorderColor = MidDark,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            cursorColor = SpotifyGreen,
            errorBorderColor = MaterialTheme.colorScheme.error
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun DateFieldClickable(
    date: String,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (date.isNotEmpty()) date else "Chọn ngày",
            color = if (date.isNotEmpty()) TextWhite else TextSilver.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = "Chọn ngày",
            tint = if (isError) MaterialTheme.colorScheme.error else SpotifyGreen,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierDropdownSimple(
    suppliers: List<SupplierData>,
    selectedSupplierId: String?,
    onSupplierSelected: (SupplierData?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = suppliers.find { it.id == selectedSupplierId }?.name ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Chọn nhà cung cấp", color = TextSilver.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            enabled = false,
            trailingIcon = {
                Text("v", color = TextSilver, modifier = Modifier.padding(end = 12.dp))
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = TextWhite,
                disabledBorderColor = MidDark,
                disabledContainerColor = DarkCard
            ),
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (suppliers.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Không có nhà cung cấp", color = TextSilver) },
                    onClick = { expanded = false }
                )
            } else {
                suppliers.forEach { supplier ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = supplier.name,
                                color = if (supplier.id == selectedSupplierId) SpotifyGreen else TextWhite
                            )
                        },
                        onClick = {
                            onSupplierSelected(supplier)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
