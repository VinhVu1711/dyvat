package com.vinh.dyvat.ui.screens.sales

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
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
import com.vinh.dyvat.data.model.AvailableLot
import com.vinh.dyvat.data.model.Product
import com.vinh.dyvat.data.model.ProductWithDetails
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.ui.components.DyvatSearchBar
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import java.time.LocalDate

data class SaleProductData(
    val id: String,
    val name: String,
    val code: String,
    val categoryId: String,
    val categoryName: String,
    val unitId: String,
    val unitName: String,
    val supplierId: String,
    val supplierName: String,
    val defaultSalePriceVnd: Long
) {
    fun toProductWithDetails() = ProductWithDetails(
        product = Product(
            id = id,
            name = name,
            code = code,
            categoryId = categoryId,
            unitId = unitId,
            supplierId = supplierId,
            defaultSalePriceVnd = defaultSalePriceVnd
        ),
        categoryName = categoryName,
        unitName = unitName,
        supplierName = supplierName
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleItemScreen(
    saleDate: String,
    availableProductsData: List<Map<String, Any?>>,
    editingItem: SaleItemDraftUi? = null,
    onProductAdded: (
        productId: String,
        productName: String,
        purchaseItemId: String,
        lotCode: String,
        purchaseDate: String,
        expiryDate: String?,
        quantityRemaining: Int,
        purchasePrice: Long,
        quantity: String,
        price: String
    ) -> Unit,
    onProductEdited: (
        itemId: Int,
        productId: String,
        productName: String,
        purchaseItemId: String,
        lotCode: String,
        purchaseDate: String,
        expiryDate: String?,
        quantityRemaining: Int,
        purchasePrice: Long,
        quantity: String,
        price: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onNavigateBack: () -> Unit,
    viewModel: SaleViewModel = hiltViewModel()
) {
    val isEditMode = editingItem != null
    val availableProducts = remember(availableProductsData) {
        availableProductsData.mapNotNull { map ->
            SaleProductData(
                id = map["id"] as? String ?: return@mapNotNull null,
                name = map["name"] as? String ?: "",
                code = map["code"] as? String ?: "",
                categoryId = map["categoryId"] as? String ?: "",
                categoryName = map["categoryName"] as? String ?: "",
                unitId = map["unitId"] as? String ?: "",
                unitName = map["unitName"] as? String ?: "",
                supplierId = map["supplierId"] as? String ?: "",
                supplierName = map["supplierName"] as? String ?: "",
                defaultSalePriceVnd = (map["defaultSalePriceVnd"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedProductData by remember { mutableStateOf<SaleProductData?>(null) }
    var availableLots by remember { mutableStateOf<List<AvailableLot>>(emptyList()) }
    var isLoadingLots by remember { mutableStateOf(false) }
    var lotsError by remember { mutableStateOf<String?>(null) }
    var selectedLot by remember { mutableStateOf<AvailableLot?>(null) }
    var quantity by remember { mutableStateOf("") }
    var salePrice by remember { mutableStateOf("") }
    var productError by remember { mutableStateOf<String?>(null) }
    var lotError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var expiredLotDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingItem?.id, availableProducts) {
        editingItem?.let { item ->
            selectedProductData = availableProducts.find { it.id == item.productId }
            selectedLot = AvailableLot(
                purchaseItemId = item.purchaseItemId,
                lotCode = item.lotCode,
                purchaseDate = item.purchaseDate,
                expiryDate = item.expiryDate,
                quantityRemaining = item.quantityRemaining,
                purchasePriceVnd = item.purchasePriceVnd
            )
            quantity = item.quantity
            salePrice = item.salePrice
        }
    }

    LaunchedEffect(selectedProductData?.id) {
        val productId = selectedProductData?.id ?: return@LaunchedEffect
        isLoadingLots = true
        lotsError = null
        when (val result = viewModel.fetchAvailableLotsForProduct(productId)) {
            is Result.Success -> {
                val existingLot = selectedLot
                availableLots = result.data
                selectedLot = existingLot?.let { old ->
                    result.data.find { it.purchaseItemId == old.purchaseItemId } ?: old
                }
                isLoadingLots = false
            }
            is Result.Error -> {
                availableLots = emptyList()
                lotsError = result.message
                isLoadingLots = false
            }
            is Result.Loading -> isLoadingLots = true
        }
    }

    val lineRevenue by remember {
        derivedStateOf {
            (quantity.toLongOrNull() ?: 0L) * (salePrice.toLongOrNull() ?: 0L)
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

    fun selectProduct(product: SaleProductData) {
        selectedProductData = product
        selectedLot = null
        availableLots = emptyList()
        productError = null
        lotError = null
        salePrice = product.defaultSalePriceVnd.toString()
    }

    fun validateAll(): Boolean {
        var isValid = true
        val lot = selectedLot
        if (selectedProductData == null) {
            productError = "Vui lòng chọn sản phẩm"
            isValid = false
        } else {
            productError = null
        }
        if (lot == null) {
            lotError = "Vui lòng chọn lô còn hàng"
            isValid = false
        } else {
            lotError = null
        }
        val qty = quantity.toIntOrNull()
        if (qty == null || qty <= 0) {
            quantityError = "Số lượng phải lớn hơn 0"
            isValid = false
        } else if (lot != null && qty > lot.quantityRemaining) {
            quantityError = "Số lượng bán không được lớn hơn tồn kho"
            isValid = false
        } else {
            quantityError = null
        }
        if (lot?.expiryDate?.toDateOnly()?.let { isExpiredDate(it) } == true) {
            lotError = "Lô này đã hết hạn sử dụng, không thể bán"
            isValid = false
        }
        if (salePrice.isBlank() || salePrice.toLongOrNull()?.let { it > 0 } != true) {
            priceError = "Giá bán phải lớn hơn 0"
            isValid = false
        } else {
            priceError = null
        }
        return isValid
    }

    fun submitItem() {
        if (!validateAll()) return
        val product = selectedProductData ?: return
        val lot = selectedLot ?: return
        if (editingItem != null) {
            onProductEdited(
                editingItem.id,
                product.id,
                product.name,
                lot.purchaseItemId,
                lot.lotCode,
                lot.purchaseDate,
                lot.expiryDate,
                lot.quantityRemaining,
                lot.purchasePriceVnd,
                quantity,
                salePrice
            )
        } else {
            onProductAdded(
                product.id,
                product.name,
                lot.purchaseItemId,
                lot.lotCode,
                lot.purchaseDate,
                lot.expiryDate,
                lot.quantityRemaining,
                lot.purchasePriceVnd,
                quantity,
                salePrice
            )
        }
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "SỬA SẢN PHẨM BÁN" else "THÊM SẢN PHẨM BÁN",
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
                productError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (filteredProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Không có sản phẩm", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(filteredProducts) { product ->
                            SaleProductPickRow(
                                product = product,
                                isSelected = selectedProductData?.id == product.id,
                                onClick = { selectProduct(product) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            } else {
                SaleSelectedProductCard(
                    product = selectedProductData!!,
                    onChange = {
                        selectedProductData = null
                        selectedLot = null
                        availableLots = emptyList()
                        searchQuery = ""
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Đơn vị tính", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            DisabledSaleField(value = selectedProductData?.unitName ?: "", placeholder = "Chọn sản phẩm để tự động điền")

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Chọn lô *",
                color = if (lotError != null) MaterialTheme.colorScheme.error else TextSilver,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            when {
                selectedProductData == null -> DisabledSaleField(value = "", placeholder = "Chọn sản phẩm trước")
                isLoadingLots -> Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SpotifyGreen, strokeWidth = 2.dp)
                }
                lotsError != null -> Text(lotsError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                availableLots.isEmpty() -> Text(
                    "Sản phẩm này chưa có lô còn hàng",
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableLots.forEach { lot ->
                        SaleLotPickRow(
                            lot = lot,
                            isSelected = selectedLot?.purchaseItemId == lot.purchaseItemId,
                            onClick = {
                                if (lot.expiryDate?.toDateOnly()?.let { isExpiredDate(it) } == true) {
                                    expiredLotDialogMessage = "Lô ${lot.lotCode.ifEmpty { lot.purchaseItemId.take(8).uppercase() }} đã hết hạn sử dụng, không thể bán."
                                } else {
                                    selectedLot = lot
                                    lotError = null
                                    quantityError = null
                                }
                            }
                        )
                    }
                }
            }
            lotError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Số lượng tồn kho của lô đã chọn", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            DisabledSaleField(
                value = selectedLot?.quantityRemaining?.toString() ?: "",
                placeholder = "Tự hiển thị / Khóa"
            )

            Spacer(modifier = Modifier.height(16.dp))
            SaleNumberField(
                label = "Số lượng bán *",
                value = quantity,
                error = quantityError,
                onValueChange = {
                    quantity = it.filter { c -> c.isDigit() }
                    quantityError = null
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SaleNumberField(
                label = "Giá bán cho 1 đơn vị *",
                value = salePrice,
                error = priceError,
                onValueChange = {
                    salePrice = it.filter { c -> c.isDigit() }
                    priceError = null
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = TextSilver.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tổng giá bán sản phẩm này", color = TextSilver, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = lineRevenue.toVnd(),
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

    expiredLotDialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { expiredLotDialogMessage = null },
            containerColor = DarkCard,
            title = { Text("Không thể chọn lô hết hạn", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text(message, color = TextSilver) },
            confirmButton = {
                TextButton(onClick = { expiredLotDialogMessage = null }) {
                    Text("Đã hiểu", color = SpotifyGreen)
                }
            }
        )
    }
}

@Composable
private fun SaleProductPickRow(product: SaleProductData, isSelected: Boolean, onClick: () -> Unit) {
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
        if (product.defaultSalePriceVnd > 0) {
            Text(
                text = "Giá bán: ${product.defaultSalePriceVnd.toVnd()}",
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = if (isSelected) 24.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun SaleSelectedProductCard(product: SaleProductData, onChange: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onChange),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, color = SpotifyGreen, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Giá bán: ${product.defaultSalePriceVnd.toVnd()}", color = TextSilver, style = MaterialTheme.typography.bodySmall)
            }
            Text("Đổi", color = TextSilver, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SaleLotPickRow(lot: AvailableLot, isSelected: Boolean, onClick: () -> Unit) {
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
                text = lot.lotCode.ifEmpty { lot.purchaseItemId.take(8).uppercase() },
                color = if (isSelected) SpotifyGreen else TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "${lot.expiryDate?.let { "HSD $it" } ?: "Không HSD"} - còn ${lot.quantityRemaining}",
            color = if (lot.expiryDate?.toDateOnly()?.let { isExpiredDate(it) } == true) MaterialTheme.colorScheme.error else TextSilver,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = if (isSelected) 24.dp else 0.dp)
        )
        if (lot.expiryDate?.toDateOnly()?.let { isExpiredDate(it) } == true) {
            Text(
                text = "Đã hết hạn",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = if (isSelected) 24.dp else 0.dp)
            )
        }
        Text(
            text = "Giá vốn: ${lot.purchasePriceVnd.toVnd()}",
            color = TextSilver,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = if (isSelected) 24.dp else 0.dp)
        )
    }
}

@Composable
private fun DisabledSaleField(value: String, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        enabled = false,
        placeholder = { Text(placeholder, color = TextSilver.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = TextWhite,
            disabledBorderColor = MidDark,
            disabledContainerColor = DarkCard
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SaleNumberField(
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

private fun String.toDateOnly(): String = split("T")[0]

private fun isExpiredDate(date: String): Boolean = date < LocalDate.now().toString()
