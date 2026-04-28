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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var addItemSheetForId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.initForm()
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tao phieu nhap",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lai",
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

                DateSelector(
                    date = formState.purchaseDate,
                    onClick = { showDatePicker = true }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Danh sach san pham",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = { viewModel.addFormItem() },
                        shape = RoundedCornerShape(500.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Them san pham", color = SpotifyGreen)
                    }
                }
            }

            if (formState.items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface),
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
                                text = "Chua co san pham nao",
                                color = TextSilver,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Nhan \"Them san pham\" de bat dau",
                                color = TextSilver,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = formState.items,
                    key = { _, item -> item.id }
                ) { _, item ->
                    PurchaseItemFormRow(
                        item = item,
                        onClick = { addItemSheetForId = item.id }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tong tien",
                                color = TextSilver,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${formState.items.size} san pham",
                                color = TextSilver,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = formState.totalAmount.toVnd(),
                            color = SpotifyGreen,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            formState.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.saveTicket(onNavigateBack) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !formState.isSaving && formState.items.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    shape = RoundedCornerShape(500.dp)
                ) {
                    if (formState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp),
                            color = NearBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = NearBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Luu phieu nhap",
                            color = NearBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(formState.purchaseDate)?.time
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
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                            viewModel.updatePurchaseDate(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Chon", color = SpotifyGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Huy", color = TextSilver)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    addItemSheetForId?.let { itemId ->
        val item = formState.items.find { it.id == itemId }
        if (item != null) {
            AddPurchaseItemSheet(
                item = item,
                availableProducts = formState.availableProducts,
                suppliers = formState.suppliers,
                units = formState.units,
                onProductSelected = { product ->
                    viewModel.updateFormItemProduct(
                        itemId = itemId,
                        productId = product.product.id,
                        productName = product.product.name,
                        unitId = product.product.unitId,
                        unitName = product.unitName
                    )
                },
                onSupplierSelected = { supplier ->
                    viewModel.updateFormItemSupplier(
                        itemId = itemId,
                        supplierId = supplier.id,
                        supplierName = supplier.name
                    )
                },
                onExpiryChanged = { viewModel.updateFormItemExpiry(itemId, it) },
                onQuantityChanged = { viewModel.updateFormItemQuantity(itemId, it) },
                onPriceChanged = { viewModel.updateFormItemPrice(itemId, it) },
                onRemove = {
                    viewModel.removeFormItem(itemId)
                    addItemSheetForId = null
                },
                onDismiss = { addItemSheetForId = null }
            )
        }
    }
}

@Composable
private fun DateSelector(
    date: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidDark)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = SpotifyGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ngay nhap",
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatDisplayDate(date),
                color = TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = "Chon ngay",
            tint = TextSilver,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PurchaseItemFormRow(
    item: PurchaseItemDraftUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName.ifEmpty { "Chon san pham..." },
                    color = if (item.productName.isEmpty()) TextSilver else TextWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.productError != null) {
                    Text(
                        text = item.productError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = buildString {
                            if (item.quantity.isNotEmpty()) append("${item.quantity} ${item.unitName}")
                            if (item.purchasePrice.isNotEmpty()) {
                                if (isNotEmpty()) append(" x ")
                                append(item.purchasePrice.toLongOrNull()?.toVnd() ?: item.purchasePrice)
                            }
                        },
                        color = TextSilver,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (item.lineTotal > 0) {
                Text(
                    text = item.lineTotal.toVnd(),
                    color = SpotifyGreen,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPurchaseItemSheet(
    item: PurchaseItemDraftUi,
    availableProducts: List<ProductWithDetails>,
    suppliers: List<com.vinh.dyvat.data.model.Supplier>,
    units: List<com.vinh.dyvat.data.model.UnitModel>,
    onProductSelected: (ProductWithDetails) -> Unit,
    onSupplierSelected: (com.vinh.dyvat.data.model.Supplier) -> Unit,
    onExpiryChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onPriceChanged: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showExpiryPicker by remember { mutableStateOf(false) }

    val filteredProducts = if (searchQuery.isBlank()) {
        availableProducts
    } else {
        availableProducts.filter {
            it.product.name.contains(searchQuery, ignoreCase = true) ||
                    it.product.code.contains(searchQuery, ignoreCase = true)
        }
    }

    val displayExpiryDate = if (item.expiryDate.isNotEmpty()) {
        try {
            val parts = item.expiryDate.split("-")
            if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else item.expiryDate
        } catch (_: Exception) { item.expiryDate }
    } else ""

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Them san pham",
                    color = TextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xoa",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DyvatSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Tim san pham...",
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Khong co san pham",
                        color = TextSilver,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(min = 80.dp, max = 240.dp)
                ) {
                    itemsIndexed(
                        items = filteredProducts,
                        key = { _, p -> p.product.id }
                    ) { _, product ->
                        val isSelected = product.product.id == item.productId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SpotifyGreen.copy(alpha = 0.1f) else DarkSurface)
                                .clickable {
                                    onProductSelected(product)
                                    scope.launch {
                                        sheetState.hide()
                                    }
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.product.name,
                                    color = if (isSelected) SpotifyGreen else TextWhite,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (product.categoryName.isNotEmpty()) {
                                    Text(
                                        text = product.categoryName,
                                        color = TextSilver,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (product.product.defaultPurchasePriceVnd > 0) {
                                Text(
                                    text = product.product.defaultPurchasePriceVnd.toVnd(),
                                    color = SpotifyGreen,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FormTextField(
                label = "So luong *",
                value = item.quantity,
                onValueChange = onQuantityChanged,
                placeholder = "0",
                error = item.quantityError,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FormTextField(
                label = "Gia nhap (VND) *",
                value = item.purchasePrice,
                onValueChange = onPriceChanged,
                placeholder = "0",
                error = item.priceError,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExpiryDateField(
                displayDate = displayExpiryDate,
                onClick = { showExpiryPicker = true }
            )

            if (item.lineTotal > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Thanh tien: ${item.lineTotal.toVnd()}",
                    color = SpotifyGreen,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Dong", color = TextSilver)
            }
        }
    }

    if (showExpiryPicker) {
        val initialMillis = try {
            if (item.expiryDate.isNotEmpty()) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(item.expiryDate)?.time
                    ?: System.currentTimeMillis()
            } else {
                System.currentTimeMillis()
            }
        } catch (_: Exception) { System.currentTimeMillis() }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showExpiryPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                            onExpiryChanged(dateStr)
                        }
                        showExpiryPicker = false
                    }
                ) {
                    Text("Chon", color = SpotifyGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryPicker = false }) {
                    Text("Huy", color = TextSilver)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ExpiryDateField(
    displayDate: String,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = "Han su dung",
            color = TextSilver,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MidDark)
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = displayDate.ifEmpty { "Chon ngay (neu co)" },
                color = if (displayDate.isEmpty()) TextSilver.copy(alpha = 0.5f) else TextWhite,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = if (error != null) MaterialTheme.colorScheme.error else TextSilver,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(text = placeholder, color = TextSilver.copy(alpha = 0.5f))
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = MidDark,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                cursorColor = SpotifyGreen
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatDisplayDate(dateStr: String): String {
    return try {
        val parts = dateStr.split("T")[0].split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else dateStr
    } catch (_: Exception) {
        dateStr
    }
}


