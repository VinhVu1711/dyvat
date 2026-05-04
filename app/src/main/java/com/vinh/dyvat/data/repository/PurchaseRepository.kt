package com.vinh.dyvat.data.repository

import android.util.Log
import com.vinh.dyvat.data.model.PurchaseItem
import com.vinh.dyvat.data.model.PurchaseItemWithDetails
import com.vinh.dyvat.data.model.PurchaseTicket
import com.vinh.dyvat.data.model.PurchaseTicketCard
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.Supplier
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
class PurchaseRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun getTicketCards(
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 0,
        pageSize: Int = 5,
        sortField: PurchaseTicketSortField = PurchaseTicketSortField.PURCHASE_DATE,
        ascending: Boolean = false,
        searchQuery: String = "",
        status: TicketStatus? = null
    ): Flow<Result<List<PurchaseTicketCard>>> = flow {
        emit(Result.Loading)
        try {
            val all = if (startDate != null || status != null) {
                supabaseClient.postgrest[SupabaseViews.V_PURCHASE_TICKET_CARDS]
                    .select {
                        filter {
                            startDate?.let { gte("purchase_date", it) }
                            status?.let { eq("status", it.toDatabaseValue()) }
                        }
                    }
                    .decodeList<PurchaseTicketCard>()
            } else {
                supabaseClient.postgrest[SupabaseViews.V_PURCHASE_TICKET_CARDS]
                    .select()
                    .decodeList<PurchaseTicketCard>()
            }

            val filtered = all
                .filter { card ->
                    endDate == null || card.purchaseDate.toDateOnly() <= endDate
                }
                .filter { card ->
                    searchQuery.isBlank() ||
                            card.code.contains(searchQuery, ignoreCase = true) ||
                            card.id.contains(searchQuery, ignoreCase = true)
                }

            val comparator = when (sortField) {
                PurchaseTicketSortField.PURCHASE_DATE -> compareBy<PurchaseTicketCard> { it.purchaseDate }
                PurchaseTicketSortField.TOTAL_AMOUNT -> compareBy { it.totalPurchaseAmountVnd }
            }.thenBy { it.id }

            val sorted = if (ascending) {
                filtered.sortedWith(comparator)
            } else {
                filtered.sortedWith(comparator.reversed())
            }

            val offset = page * pageSize
            emit(Result.Success(sorted.drop(offset).take(pageSize)))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Loi khi tai phieu nhap", e))
        }
    }

    suspend fun getTicketById(id: String): Result<PurchaseTicket> {
        return try {
            val response = supabaseClient.postgrest[SupabaseTables.PURCHASE_TICKETS]
                .select { filter { eq("id", id) } }
                .decodeSingle<PurchaseTicket>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Loi khi tai phieu nhap", e)
        }
    }

    fun getItemsByTicketId(ticketId: String): Flow<Result<List<PurchaseItemWithDetails>>> = flow {
        emit(Result.Loading)
        try {
            Log.d("PurchaseRepository", "getItemsByTicketId: loading items for ticketId=$ticketId")
            val items = supabaseClient.postgrest[SupabaseTables.PURCHASE_ITEMS]
                .select { filter { eq("purchase_ticket_id", ticketId) } }
                .decodeList<PurchaseItem>()
            Log.d("PurchaseRepository", "getItemsByTicketId: found ${items.size} items from database")

            val products = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .select()
                .decodeList<com.vinh.dyvat.data.model.Product>()
                .associateBy { it.id }

            val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select()
                .decodeList<UnitModel>()
                .associateBy { it.id }

            val suppliers = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .select()
                .decodeList<Supplier>()
                .associateBy { it.id }

            val result = items.map { item ->
                val product = products[item.productId]
                val calculatedLineTotal = if (item.lineTotalVnd > 0) {
                    item.lineTotalVnd
                } else {
                    item.quantityPurchased.toLong() * item.purchasePriceVnd
                }
                val itemWithCalculatedTotal = item.copy(lineTotalVnd = calculatedLineTotal)
                Log.d(
                    "PurchaseRepository",
                    "getItemsByTicketId: item - productId=${item.productId}, qty=${item.quantityPurchased}, price=${item.purchasePriceVnd}, lineTotal=$calculatedLineTotal"
                )
                PurchaseItemWithDetails(
                    item = itemWithCalculatedTotal,
                    productName = product?.name ?: "",
                    productCode = product?.code ?: "",
                    supplierName = suppliers[item.supplierId]?.name ?: "",
                    unitName = units[item.unitId]?.name ?: ""
                )
            }
            Log.d("PurchaseRepository", "getItemsByTicketId: returning ${result.size} items")
            emit(Result.Success(result))
        } catch (e: Exception) {
            Log.e("PurchaseRepository", "getItemsByTicketId: failed - ${e.message}", e)
            emit(Result.Error(e.message ?: "Loi khi tai chi tiet phieu nhap", e))
        }
    }

    suspend fun createTicket(
        purchaseDate: String,
        items: List<PurchaseItemDraft>
    ): Result<String> {
        var createdTicketId: String? = null
        return try {
            Log.d("PurchaseRepository", "createTicket: creating ticket with date=$purchaseDate, items count=${items.size}")
            val ticketId = UUID.randomUUID().toString()
            val ticket = PurchaseTicketInsert(
                id = ticketId,
                purchaseDate = purchaseDate
            )

            supabaseClient.postgrest[SupabaseTables.PURCHASE_TICKETS]
                .insert(ticket)
            createdTicketId = ticketId
            Log.d("PurchaseRepository", "createTicket: ticket created with id=$ticketId")

            for (draft in items) {
                val lineTotalVnd = draft.quantityPurchased.toLong() * draft.purchasePriceVnd
                Log.d(
                    "PurchaseRepository",
                    "createTicket: inserting item - productId=${draft.productId}, qty=${draft.quantityPurchased}, price=${draft.purchasePriceVnd}, lineTotal=$lineTotalVnd"
                )
                val item = PurchaseItemInsert(
                    purchaseTicketId = ticketId,
                    productId = draft.productId,
                    supplierId = draft.supplierId,
                    unitId = draft.unitId,
                    expiryDate = draft.expiryDate,
                    quantityPurchased = draft.quantityPurchased,
                    purchasePriceVnd = draft.purchasePriceVnd
                )
                supabaseClient.postgrest[SupabaseTables.PURCHASE_ITEMS]
                    .insert(item)
                Log.d("PurchaseRepository", "createTicket: item inserted successfully")
            }

            Result.Success(ticketId)
        } catch (e: Exception) {
            Log.e("PurchaseRepository", "createTicket: failed - ${e.message}", e)
            createdTicketId?.let { ticketId ->
                rollbackCreatedTicket(ticketId)
            }
            Result.Error(e.message ?: "Loi khi tao phieu nhap", e)
        }
    }

    private suspend fun rollbackCreatedTicket(ticketId: String) {
        try {
            supabaseClient.postgrest[SupabaseTables.PURCHASE_ITEMS]
                .delete { filter { eq("purchase_ticket_id", ticketId) } }
            supabaseClient.postgrest[SupabaseTables.PURCHASE_TICKETS]
                .delete { filter { eq("id", ticketId) } }
            Log.d("PurchaseRepository", "createTicket: rolled back ticketId=$ticketId after failure")
        } catch (rollbackError: Exception) {
            Log.e(
                "PurchaseRepository",
                "createTicket: rollback failed for ticketId=$ticketId - ${rollbackError.message}",
                rollbackError
            )
        }
    }

    suspend fun cancelTicket(id: String, reason: String?): Result<Unit> {
        return try {
            val statusStr = when (TicketStatus.CANCELLED) {
                TicketStatus.CANCELLED -> "cancelled"
                else -> "cancelled"
            }
            supabaseClient.postgrest[SupabaseTables.PURCHASE_TICKETS]
                .update({
                    set("status", statusStr)
                    set("cancel_reason", reason)
                }) {
                    filter { eq("id", id) }
                }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Loi khi huy phieu nhap", e)
        }
    }
}

private fun String.toDateOnly(): String {
    return split("T")[0]
}

private fun TicketStatus.toDatabaseValue(): String {
    return when (this) {
        TicketStatus.ACTIVE -> "active"
        TicketStatus.CANCELLED -> "cancelled"
    }
}

data class PurchaseItemDraft(
    val productId: String,
    val supplierId: String,
    val unitId: String,
    val expiryDate: String?,
    val quantityPurchased: Int,
    val purchasePriceVnd: Long
)

enum class PurchaseTicketSortField {
    PURCHASE_DATE,
    TOTAL_AMOUNT
}

@Serializable
private data class PurchaseTicketInsert(
    val id: String,
    @SerialName("purchase_date")
    val purchaseDate: String
)

@Serializable
private data class PurchaseItemInsert(
    @SerialName("purchase_ticket_id")
    val purchaseTicketId: String,
    @SerialName("product_id")
    val productId: String,
    @SerialName("supplier_id")
    val supplierId: String,
    @SerialName("unit_id")
    val unitId: String,
    @SerialName("expiry_date")
    val expiryDate: String?,
    @SerialName("quantity_purchased")
    val quantityPurchased: Int,
    @SerialName("purchase_price_vnd")
    val purchasePriceVnd: Long
)
