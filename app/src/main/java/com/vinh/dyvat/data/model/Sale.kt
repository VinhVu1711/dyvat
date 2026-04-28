package com.vinh.dyvat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class SaleTicketStatus {
    @SerialName("active")
    ACTIVE,
    @SerialName("cancelled")
    CANCELLED
}

@Serializable
data class SaleTicketCard(
    val id: String = "",
    val code: String = "",
    @SerialName("sale_date")
    val saleDate: String = "",
    val status: SaleTicketStatus = SaleTicketStatus.ACTIVE,
    @SerialName("cancelled_at")
    val cancelledAt: String? = null,
    @SerialName("total_sale_amount_vnd")
    val totalSaleAmountVnd: Long = 0L,
    @SerialName("total_cost_amount_vnd")
    val totalCostAmountVnd: Long = 0L,
    @SerialName("profit_vnd")
    val profitVnd: Long = 0L,
    @SerialName("item_count")
    val itemCount: Int = 0
)

@Serializable
data class SaleTicket(
    val id: String = "",
    @Transient
    @SerialName("owner_id")
    val ownerId: String = "",
    val code: String = "",
    @SerialName("sale_date")
    val saleDate: String = "",
    val status: SaleTicketStatus = SaleTicketStatus.ACTIVE,
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
data class SaleItem(
    val id: String = "",
    @Transient
    @SerialName("owner_id")
    val ownerId: String = "",
    @SerialName("sale_ticket_id")
    val saleTicketId: String = "",
    @SerialName("product_id")
    val productId: String = "",
    @SerialName("purchase_item_id")
    val purchaseItemId: String = "",
    @SerialName("unit_id")
    val unitId: String = "",
    @SerialName("quantity_sold")
    val quantitySold: Int = 0,
    @SerialName("sale_price_vnd")
    val salePriceVnd: Long = 0L,
    @SerialName("unit_cost_vnd")
    val unitCostVnd: Long = 0L,
    @SerialName("line_revenue_vnd")
    val lineRevenueVnd: Long = 0L,
    @SerialName("line_cost_vnd")
    val lineCostVnd: Long = 0L,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

data class SaleItemWithDetails(
    val item: SaleItem,
    val productName: String = "",
    val productCode: String = "",
    val lotCode: String = "",
    val expiryDate: String? = null,
    val unitName: String = ""
)

data class AvailableLot(
    val purchaseItemId: String = "",
    val lotCode: String = "",
    val expiryDate: String? = null,
    val quantityRemaining: Int = 0,
    val purchasePriceVnd: Long = 0L,
    val displayText: String = ""
) {
    companion object {
        fun fromPurchaseItem(
            pi: PurchaseItem,
            lotCode: String,
            unitName: String
        ): AvailableLot {
            val expText = pi.expiryDate?.let { "HSD $it" } ?: "Không HSD"
            val display = "$lotCode - $expText - còn ${pi.quantityRemaining} $unitName"
            return AvailableLot(
                purchaseItemId = pi.id,
                lotCode = lotCode,
                expiryDate = pi.expiryDate,
                quantityRemaining = pi.quantityRemaining,
                purchasePriceVnd = pi.purchasePriceVnd,
                displayText = display
            )
        }
    }
}

data class SaleItemFormState(
    val productId: String = "",
    val purchaseItemId: String = "",
    val unitId: String = "",
    val unitName: String = "",
    val quantitySold: String = "",
    val salePriceVnd: String = "",
    val availableLots: List<AvailableLot> = emptyList(),
    val selectedLot: AvailableLot? = null,
    val isValid: Boolean = false
) {
    val lineRevenue: Long
        get() {
            val qty = quantitySold.toLongOrNull() ?: 0L
            val price = salePriceVnd.toLongOrNull() ?: 0L
            return qty * price
        }
}

data class SaleTicketFormState(
    val saleDate: String = "",
    val items: List<SaleItemFormState> = emptyList()
) {
    val totalRevenue: Long
        get() = items.filter { it.isValid }.sumOf { it.lineRevenue }
}
