package com.vinh.dyvat.data.repository

import android.util.Log
import com.vinh.dyvat.data.model.AvailableLot
import com.vinh.dyvat.data.model.PurchaseItem
import com.vinh.dyvat.data.model.PurchaseTicket
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.SaleItem
import com.vinh.dyvat.data.model.SaleItemWithDetails
import com.vinh.dyvat.data.model.SaleTicket
import com.vinh.dyvat.data.model.SaleTicketCard
import com.vinh.dyvat.data.model.SaleTicketStatus
import com.vinh.dyvat.data.model.TicketStatus
import com.vinh.dyvat.data.model.UnitModel
import com.vinh.dyvat.data.remote.SupabaseTables
import com.vinh.dyvat.data.remote.SupabaseViews
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun getTicketCards(
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 0,
        pageSize: Int = 5,
        sortField: SaleTicketSortField = SaleTicketSortField.SALE_DATE,
        ascending: Boolean = false,
        searchQuery: String = "",
        status: SaleTicketStatus? = null
    ): Flow<Result<List<SaleTicketCard>>> = flow {
        emit(Result.Loading)
        try {
            val all = if (startDate != null || status != null) {
                supabaseClient.postgrest[SupabaseViews.V_SALE_TICKET_CARDS]
                    .select {
                        filter {
                            startDate?.let { gte("sale_date", it) }
                            status?.let { eq("status", it.toDatabaseValue()) }
                        }
                    }
                    .decodeList<SaleTicketCard>()
            } else {
                supabaseClient.postgrest[SupabaseViews.V_SALE_TICKET_CARDS]
                    .select()
                    .decodeList<SaleTicketCard>()
            }

            val filtered = all
                .filter { card -> endDate == null || card.saleDate.toDateOnly() <= endDate }
                .filter { card ->
                    searchQuery.isBlank() ||
                            card.code.contains(searchQuery, ignoreCase = true) ||
                            card.id.contains(searchQuery, ignoreCase = true)
                }

            val comparator = when (sortField) {
                SaleTicketSortField.SALE_DATE -> compareBy<SaleTicketCard> { it.saleDate }
                SaleTicketSortField.TOTAL_AMOUNT -> compareBy { it.totalSaleAmountVnd }
            }.thenBy { it.id }
            val sorted = if (ascending) filtered.sortedWith(comparator) else filtered.sortedWith(comparator.reversed())
            val offset = page * pageSize

            emit(Result.Success(sorted.drop(offset).take(pageSize)))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Loi khi tai phieu ban", e))
        }
    }

    suspend fun getTicketById(id: String): Result<SaleTicket> {
        return try {
            val response = supabaseClient.postgrest[SupabaseTables.SALE_TICKETS]
                .select { filter { eq("id", id) } }
                .decodeSingle<SaleTicket>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Loi khi tai phieu ban", e)
        }
    }

    fun getItemsByTicketId(ticketId: String): Flow<Result<List<SaleItemWithDetails>>> = flow {
        emit(Result.Loading)
        try {
            val items = supabaseClient.postgrest[SupabaseTables.SALE_ITEMS]
                .select { filter { eq("sale_ticket_id", ticketId) } }
                .decodeList<SaleItem>()

            val products = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .select()
                .decodeList<com.vinh.dyvat.data.model.Product>()
                .associateBy { it.id }

            val purchaseItems = supabaseClient.postgrest[SupabaseTables.PURCHASE_ITEMS]
                .select()
                .decodeList<PurchaseItem>()
                .associateBy { it.id }

            val purchaseTickets = supabaseClient.postgrest[SupabaseTables.PURCHASE_TICKETS]
                .select()
                .decodeList<PurchaseTicket>()
                .associateBy { it.id }

            val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select()
                .decodeList<UnitModel>()
                .associateBy { it.id }

            val result = items.map { item ->
                val product = products[item.productId]
                val lot = purchaseItems[item.purchaseItemId]
                val ticket = lot?.let { purchaseTickets[it.purchaseTicketId] }
                val calculatedRevenue = if (item.lineRevenueVnd > 0) {
                    item.lineRevenueVnd
                } else {
                    item.quantitySold.toLong() * item.salePriceVnd
                }
                SaleItemWithDetails(
                    item = item.copy(lineRevenueVnd = calculatedRevenue),
                    productName = product?.name ?: "",
                    productCode = product?.code ?: "",
                    lotCode = ticket?.code ?: "",
                    expiryDate = lot?.expiryDate,
                    unitName = units[item.unitId]?.name ?: ""
                )
            }
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Loi khi tai chi tiet phieu ban", e))
        }
    }

    fun getAvailableLotsForProduct(productId: String): Flow<Result<List<AvailableLot>>> = flow {
        emit(Result.Loading)
        try {
            val items = supabaseClient.postgrest[SupabaseTables.PURCHASE_ITEMS]
                .select { filter { eq("product_id", productId) } }
                .decodeList<PurchaseItem>()
                .filter { it.quantityRemaining > 0 }

            val purchaseTickets = supabaseClient.postgrest[SupabaseTables.PURCHASE_TICKETS]
                .select()
                .decodeList<PurchaseTicket>()
                .associateBy { it.id }

            val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select()
                .decodeList<UnitModel>()
                .associateBy { it.id }

            val result = items.mapNotNull { item ->
                val ticket = purchaseTickets[item.purchaseTicketId] ?: return@mapNotNull null
                if (ticket.status != TicketStatus.ACTIVE) return@mapNotNull null
                val unitName = units[item.unitId]?.name ?: ""
                AvailableLot.fromPurchaseItem(item, ticket.code, unitName)
            }.sortedWith(compareBy<AvailableLot> { it.expiryDate ?: "9999-12-31" }.thenBy { it.lotCode })

            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Loi khi tai lo hang", e))
        }
    }

    suspend fun createTicket(
        saleDate: String,
        items: List<SaleItemDraft>
    ): Result<String> {
        var createdTicketId: String? = null
        return try {
            Log.d("SaleRepository", "createTicket: creating ticket date=$saleDate, items=${items.size}")
            val ticketId = UUID.randomUUID().toString()
            val ticket = SaleTicketInsert(id = ticketId, saleDate = saleDate)

            supabaseClient.postgrest[SupabaseTables.SALE_TICKETS].insert(ticket)
            createdTicketId = ticketId

            for (draft in items) {
                val item = SaleItemInsert(
                    saleTicketId = ticketId,
                    productId = draft.productId,
                    purchaseItemId = draft.purchaseItemId,
                    unitId = draft.unitId,
                    quantitySold = draft.quantitySold,
                    salePriceVnd = draft.salePriceVnd
                )
                supabaseClient.postgrest[SupabaseTables.SALE_ITEMS].insert(item)
            }

            Result.Success(ticketId)
        } catch (e: Exception) {
            Log.e("SaleRepository", "createTicket: failed - ${e.message}", e)
            createdTicketId?.let { rollbackCreatedTicket(it) }
            Result.Error(e.message ?: "Loi khi tao phieu ban", e)
        }
    }

    private suspend fun rollbackCreatedTicket(ticketId: String) {
        try {
            supabaseClient.postgrest[SupabaseTables.SALE_ITEMS]
                .delete { filter { eq("sale_ticket_id", ticketId) } }
            supabaseClient.postgrest[SupabaseTables.SALE_TICKETS]
                .delete { filter { eq("id", ticketId) } }
            Log.d("SaleRepository", "createTicket: rolled back ticketId=$ticketId")
        } catch (rollbackError: Exception) {
            Log.e("SaleRepository", "createTicket: rollback failed - ${rollbackError.message}", rollbackError)
        }
    }

    suspend fun cancelTicket(id: String, reason: String?): Result<Unit> {
        return try {
            supabaseClient.postgrest[SupabaseTables.SALE_TICKETS]
                .update({
                    set("status", "cancelled")
                    set("cancel_reason", reason)
                }) {
                    filter { eq("id", id) }
                }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Loi khi huy phieu ban", e)
        }
    }
}

data class SaleItemDraft(
    val productId: String,
    val purchaseItemId: String,
    val unitId: String,
    val quantitySold: Int,
    val salePriceVnd: Long
)

enum class SaleTicketSortField {
    SALE_DATE,
    TOTAL_AMOUNT
}

private fun String.toDateOnly(): String = split("T")[0]

private fun SaleTicketStatus.toDatabaseValue(): String {
    return when (this) {
        SaleTicketStatus.ACTIVE -> "active"
        SaleTicketStatus.CANCELLED -> "cancelled"
    }
}

@Serializable
private data class SaleTicketInsert(
    val id: String,
    @SerialName("sale_date")
    val saleDate: String
)

@Serializable
private data class SaleItemInsert(
    @SerialName("sale_ticket_id")
    val saleTicketId: String,
    @SerialName("product_id")
    val productId: String,
    @SerialName("purchase_item_id")
    val purchaseItemId: String,
    @SerialName("unit_id")
    val unitId: String,
    @SerialName("quantity_sold")
    val quantitySold: Int,
    @SerialName("sale_price_vnd")
    val salePriceVnd: Long
)
