package com.vinh.dyvat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImportReceipt(
    val id: String = "",
    @SerialName("import_date")
    val importDate: String = "",
    @SerialName("total_amount")
    val totalAmount: Double = 0.0,
    @SerialName("payment_method")
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
enum class PaymentMethod {
    @SerialName("cash")
    CASH,
    @SerialName("transfer")
    TRANSFER
}

@Serializable
data class ImportItem(
    val id: String = "",
    @SerialName("import_receipt_id")
    val importReceiptId: String = "",
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("unit_id")
    val unitId: String = "",
    @SerialName("supplier_id")
    val supplierId: String? = null,
    @SerialName("product_name")
    val productName: String = "",
    @SerialName("total_import_amount")
    val totalImportAmount: Double = 0.0,
    @SerialName("unit_price")
    val unitPrice: Double = 0.0,
    @SerialName("total_quantity")
    val totalQuantity: Int = 0,
    @SerialName("quantity_for_sale")
    val quantityForSale: Int = 0,
    @SerialName("quantity_in_stock")
    val quantityInStock: Int = 0,
    val status: ItemStatus = ItemStatus.IN_STOCK,
    @SerialName("sale_location")
    val saleLocation: String? = null,
    @SerialName("expiry_date")
    val expiryDate: String? = null,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
enum class ItemStatus {
    @SerialName("in_stock")
    IN_STOCK,
    @SerialName("for_sale")
    FOR_SALE
}

data class ImportItemWithDetails(
    val importItem: ImportItem,
    val categoryName: String = "",
    val unitName: String = "",
    val supplierName: String = ""
)
