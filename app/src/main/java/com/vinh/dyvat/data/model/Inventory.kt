package com.vinh.dyvat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LotStatus {
    @SerialName("in_stock")
    IN_STOCK,
    @SerialName("out_of_stock")
    OUT_OF_STOCK,
    @SerialName("cancelled")
    CANCELLED,
    @SerialName("has_expired_item")
    HAS_EXPIRED_ITEM
}

@Serializable
data class InventoryLotCard(
    @SerialName("purchase_ticket_id")
    val purchaseTicketId: String = "",
    @SerialName("lot_code")
    val lotCode: String = "",
    @SerialName("purchase_date")
    val purchaseDate: String = "",
    @SerialName("lot_status")
    val lotStatus: LotStatus = LotStatus.IN_STOCK,
    @SerialName("total_inventory_value_vnd")
    val totalInventoryValueVnd: Long = 0L,
    @SerialName("total_remaining_quantity")
    val totalRemainingQuantity: Int = 0
)

@Serializable
data class InventoryLotDetail(
    @SerialName("purchase_item_id")
    val purchaseItemId: String = "",
    @SerialName("lot_code")
    val lotCode: String = "",
    @SerialName("purchase_date")
    val purchaseDate: String = "",
    @SerialName("product_code")
    val productCode: String = "",
    @SerialName("product_name")
    val productName: String = "",
    @SerialName("unit_name")
    val unitName: String = "",
    @SerialName("supplier_name")
    val supplierName: String = "",
    @SerialName("expiry_date")
    val expiryDate: String? = null,
    @SerialName("quantity_purchased")
    val quantityPurchased: Int = 0,
    @SerialName("quantity_remaining")
    val quantityRemaining: Int = 0,
    @SerialName("purchase_price_vnd")
    val purchasePriceVnd: Long = 0L,
    @SerialName("remaining_value_vnd")
    val remainingValueVnd: Long = 0L
)
