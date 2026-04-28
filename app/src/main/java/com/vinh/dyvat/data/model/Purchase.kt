package com.vinh.dyvat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class TicketStatus {
    @SerialName("active")
    ACTIVE,
    @SerialName("cancelled")
    CANCELLED
}

@Serializable
data class PurchaseTicketCard(
    val id: String = "",
    val code: String = "",
    @SerialName("purchase_date")
    val purchaseDate: String = "",
    val status: TicketStatus = TicketStatus.ACTIVE,
    @SerialName("cancelled_at")
    val cancelledAt: String? = null,
    @SerialName("total_purchase_amount_vnd")
    val totalPurchaseAmountVnd: Long = 0L,
    @SerialName("item_count")
    val itemCount: Int = 0
)

@Serializable
data class PurchaseTicket(
    val id: String = "",
    @Transient
    @SerialName("owner_id")
    val ownerId: String = "",
    val code: String = "",
    @SerialName("purchase_date")
    val purchaseDate: String = "",
    val status: TicketStatus = TicketStatus.ACTIVE,
    @SerialName("cancelled_at")
    val cancelledAt: String? = null,
    @SerialName("cancel_reason")
    val cancelReason: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class PurchaseItem(
    val id: String = "",
    @Transient
    @SerialName("owner_id")
    val ownerId: String = "",
    @SerialName("purchase_ticket_id")
    val purchaseTicketId: String = "",
    @SerialName("product_id")
    val productId: String = "",
    @SerialName("supplier_id")
    val supplierId: String = "",
    @SerialName("unit_id")
    val unitId: String = "",
    @SerialName("expiry_date")
    val expiryDate: String? = null,
    @SerialName("quantity_purchased")
    val quantityPurchased: Int = 0,
    @SerialName("quantity_remaining")
    val quantityRemaining: Int = 0,
    @SerialName("purchase_price_vnd")
    val purchasePriceVnd: Long = 0L,
    @SerialName("line_total_vnd")
    val lineTotalVnd: Long = 0L,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

data class PurchaseItemWithDetails(
    val item: PurchaseItem,
    val productName: String = "",
    val productCode: String = "",
    val supplierName: String = "",
    val unitName: String = ""
)

data class PurchaseItemFormState(
    val productId: String = "",
    val supplierId: String = "",
    val unitId: String = "",
    val expiryDate: String = "",
    val quantityPurchased: String = "",
    val purchasePriceVnd: String = "",
    val isValid: Boolean = false
) {
    val lineTotal: Long
        get() {
            val qty = quantityPurchased.toLongOrNull() ?: 0L
            val price = purchasePriceVnd.toLongOrNull() ?: 0L
            return qty * price
        }
}

data class PurchaseTicketFormState(
    val purchaseDate: String = "",
    val items: List<PurchaseItemFormState> = emptyList()
) {
    val totalAmount: Long
        get() = items.filter { it.isValid }.sumOf { it.lineTotal }
}
