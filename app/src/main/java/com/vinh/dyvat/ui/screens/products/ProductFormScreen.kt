package com.vinh.dyvat.ui.screens.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.ui.components.ErrorDialog
import com.vinh.dyvat.ui.components.FormDropdownSelector
import com.vinh.dyvat.ui.components.LoadingIndicator
import com.vinh.dyvat.ui.components.SuccessDialog
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: String? = null,
    onNavigateBack: () -> Unit,
    onProductSaved: () -> Unit = onNavigateBack,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val formState by viewModel.formUiState.collectAsState()
    val isEditMode = productId != null

    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.initFormForEdit(productId)
        } else {
            viewModel.initFormForAdd()
        }
    }

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "SỬA SẢN PHẨM" else "THÊM SẢN PHẨM",
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
        if (formState.isLoading && isEditMode) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                FormTextField(
                    label = "Tên sản phẩm *",
                    value = formState.name,
                    onValueChange = { viewModel.updateFormName(it) },
                    placeholder = "Nhập tên sản phẩm",
                    error = formState.nameError,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = TextSilver
                        )
                    }
                )

                FormDropdownSelector(
                    label = "Loại sản phẩm *",
                    items = formState.categories,
                    selectedItem = formState.categories.find { it.id == formState.categoryId },
                    onItemSelected = { viewModel.updateFormCategory(it.id) },
                    itemToString = { it.name },
                    itemToId = { it.id },
                    placeholder = "Chọn loại sản phẩm",
                    isRequired = true,
                    errorMessage = formState.categoryError
                )

                FormDropdownSelector(
                    label = "Đơn vị tính *",
                    items = formState.units,
                    selectedItem = formState.units.find { it.id == formState.unitId },
                    onItemSelected = { viewModel.updateFormUnit(it.id) },
                    itemToString = { it.name },
                    itemToId = { it.id },
                    placeholder = "Chọn đơn vị tính",
                    isRequired = true,
                    errorMessage = formState.unitError
                )

                FormDropdownSelector(
                    label = "Nhà cung cấp *",
                    items = formState.suppliers,
                    selectedItem = formState.suppliers.find { it.id == formState.supplierId },
                    onItemSelected = { viewModel.updateFormSupplier(it.id) },
                    itemToString = { it.name },
                    itemToId = { it.id },
                    placeholder = "Chọn nhà cung cấp",
                    isRequired = true,
                    errorMessage = formState.supplierError
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormTextField(
                        label = "Giá nhập (VND) *",
                        value = formState.purchasePrice,
                        onValueChange = { viewModel.updateFormPurchasePrice(it) },
                        placeholder = "0",
                        error = formState.purchasePriceError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = TextSilver
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FormTextField(
                        label = "Giá bán (VND) *",
                        value = formState.salePrice,
                        onValueChange = { viewModel.updateFormSalePrice(it) },
                        placeholder = "0",
                        error = formState.salePriceError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = SpotifyGreen
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                formState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                HorizontalDivider(color = TextSilver.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = formState.addMultiple,
                        onCheckedChange = { viewModel.updateFormAddMultiple(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SpotifyGreen,
                            uncheckedColor = TextSilver,
                            checkmarkColor = NearBlack
                        )
                    )
                    Text(
                        text = "Thêm hàng loạt",
                        color = TextSilver,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = { viewModel.saveProduct(onProductSaved) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !formState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        disabledContainerColor = SpotifyGreen.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (formState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp),
                            color = NearBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isEditMode) "Lưu thay đổi" else "Lưu sản phẩm",
                            color = NearBlack,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (formState.showSuccessDialog) {
            SuccessDialog(
                title = if (isEditMode) "Cập nhật sản phẩm thành công" else "Thêm sản phẩm thành công",
                message = if (formState.successDialogMessage.isNotEmpty()) {
                    formState.successDialogMessage
                } else {
                    "Sản phẩm \"${formState.name.trim()}\" đã được lưu vào hệ thống."
                },
                onDismiss = {
                    viewModel.dismissSuccessDialog()
                    onProductSaved()
                }
            )
        }

        if (formState.showErrorDialog) {
            ErrorDialog(
                title = formState.errorDialogTitle,
                message = formState.errorDialogMessage,
                onDismiss = { viewModel.dismissErrorDialog() }
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
    singleLine: Boolean,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
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
                Text(
                    text = placeholder,
                    color = TextSilver.copy(alpha = 0.5f)
                )
            },
            singleLine = singleLine,
            leadingIcon = leadingIcon,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = MidDark,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard,
                cursorColor = SpotifyGreen
            ),
            isError = error != null,
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
