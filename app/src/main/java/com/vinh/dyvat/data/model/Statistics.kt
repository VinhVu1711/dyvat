package com.vinh.dyvat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class DailySummary(
    @Transient
    @SerialName("owner_id")
    val ownerId: String = "",
    @SerialName("business_date")
    val businessDate: String = "",
    @SerialName("total_purchase_vnd")
    val totalPurchaseVnd: Long = 0L,
    @SerialName("total_sale_vnd")
    val totalSaleVnd: Long = 0L,
    @SerialName("total_cost_vnd")
    val totalCostVnd: Long = 0L,
    @SerialName("profit_vnd")
    val profitVnd: Long = 0L,
    @SerialName("purchase_ticket_count")
    val purchaseTicketCount: Int = 0,
    @SerialName("sale_ticket_count")
    val saleTicketCount: Int = 0
)

data class MonthlySummary(
    val year: Int,
    val month: Int,
    val totalPurchaseVnd: Long = 0L,
    val totalSaleVnd: Long = 0L,
    val totalCostVnd: Long = 0L,
    val profitVnd: Long = 0L,
    val purchaseTicketCount: Int = 0,
    val saleTicketCount: Int = 0,
    val dailyData: List<DailySummary> = emptyList()
)

data class YearlySummary(
    val year: Int,
    val totalPurchaseVnd: Long = 0L,
    val totalSaleVnd: Long = 0L,
    val totalCostVnd: Long = 0L,
    val profitVnd: Long = 0L,
    val purchaseTicketCount: Int = 0,
    val saleTicketCount: Int = 0,
    val monthlyData: List<MonthlySummary> = emptyList()
)
