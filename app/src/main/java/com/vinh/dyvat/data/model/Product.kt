package com.vinh.dyvat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class ProductStatus {
    @SerialName("active")
    ACTIVE,
    @SerialName("discontinued")
    DISCONTINUED
}

@Serializable
data class Product(
    val id: String = "",
    @Transient
    @SerialName("owner_id")
    val ownerId: String = "",
    val code: String = "",
    val name: String = "",
    @SerialName("category_id")
    val categoryId: String = "",
    @SerialName("unit_id")
    val unitId: String = "",
    @SerialName("supplier_id")
    val supplierId: String = "",
    @SerialName("default_purchase_price_vnd")
    val defaultPurchasePriceVnd: Long = 0L,
    @SerialName("default_sale_price_vnd")
    val defaultSalePriceVnd: Long = 0L,
    val status: ProductStatus = ProductStatus.ACTIVE,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

data class ProductWithDetails(
    val product: Product,
    val categoryName: String = "",
    val unitName: String = "",
    val supplierName: String = ""
)

data class ProductFormState(
    val name: String = "",
    val categoryId: String = "",
    val unitId: String = "",
    val supplierId: String = "",
    val defaultPurchasePriceVnd: String = "",
    val defaultSalePriceVnd: String = "",
    val isValid: Boolean = false
)
