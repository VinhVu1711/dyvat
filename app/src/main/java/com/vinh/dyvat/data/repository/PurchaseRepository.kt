package com.vinh.dyvat.data.repository

import android.util.Log
import com.vinh.dyvat.data.model.PurchaseItem
import com.vinh.dyvat.data.model.PurchaseItemWithDetails
import com.vinh.dyvat.data.model.PurchaseTicket
import com.vinh.dyvat.data.model.PurchaseTicketCard
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.TicketStatus
import com.vinh.dyvat.data.model.UnitModel
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.data.remote.SupabaseTables
import com.vinh.dyvat.data.remote.SupabaseViews
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun getTicketCards(
        startDate: String? = null,
        endDate: String? = null
    ): Flow<Result<List<PurchaseTicketCard>>> = flow {
        emit(Result.Loading)
        try {
            var query = supabaseClient.postgrest[SupabaseViews.V_PURCHASE_TICKET_CARDS]
                .select()

            if (startDate != null) {
                query = supabaseClient.postgrest[SupabaseViews.V_PURCHASE_TICKET_CARDS]
                    .select { filter { gte("purchase_date", startDate) } }
            }

            val all = if (startDate != null) {
                supabaseClient.postgrest[SupabaseViews.V_PURCHASE_TICKET_CARDS]
                    .select { filter { gte("purchase_date", startDate) } }
                    .decodeList<PurchaseTicketCard>()
            } else {
                supabaseClient.postgrest[SupabaseViews.V_PURCHASE_TICKET_CARDS]
                    .select()
                    .decodeList<PurchaseTicketCard>()
            }

            val filtered = if (endDate != null) {
                all.filter { it.purchaseDate <= endDate }
            } else {
                all
            }

            emit(Result.Success(filtered))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải phiếu nhập", e))
        }
    }

    suspend fun getTicketById(id: String): Result<PurchaseTicket> {
        return try {
            val response = supabaseClient.postgrest[SupabaseTables.PURCHASE_TICKETS]
                .select { filter { eq("id", id) } }
                .decodeSingle<PurchaseTicket>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi tải phiếu nhập", e)
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
                .select().decodeList<com.vinh.dyvat.data.model.Product>()
                .associateBy { it.id }

            val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select().decodeList<UnitModel>()
                .associateBy { it.id }

            val suppliers = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .select().decodeList<Supplier>()
                .associateBy { it.id }

            val result = items.map { item ->
                val product = products[item.productId]
                // Calculate lineTotalVnd if not stored correctly in database
                val calculatedLineTotal = if (item.lineTotalVnd > 0) {
                    item.lineTotalVnd
                } else {
                    item.quantityPurchased.toLong() * item.purchasePriceVnd
                }
                val itemWithCalculatedTotal = item.copy(lineTotalVnd = calculatedLineTotal)
                Log.d("PurchaseRepository", "getItemsByTicketId: item - productId=${item.productId}, qty=${item.quantityPurchased}, price=${item.purchasePriceVnd}, lineTotal=$calculatedLineTotal")
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
            emit(Result.Error(e.message ?: "Lỗi khi tải chi tiết phiếu nhập", e))
        }
    }

    suspend fun createTicket(
        purchaseDate: String,
        items: List<PurchaseItemDraft>
    ): Result<String> {
        return try {
            Log.d("PurchaseRepository", "createTicket: creating ticket with date=$purchaseDate, items count=${items.size}")
            val ticket = PurchaseTicket(purchaseDate = purchaseDate)
            val insertedTicket = supabaseClient.postgrest[SupabaseTables.PURCHASE_TICKETS]
                .insert(ticket)
                .decodeSingle<PurchaseTicket>()
            Log.d("PurchaseRepository", "createTicket: ticket created with id=${insertedTicket.id}")

            for (draft in items) {
                val lineTotalVnd = draft.quantityPurchased.toLong() * draft.purchasePriceVnd
                Log.d("PurchaseRepository", "createTicket: inserting item - productId=${draft.productId}, qty=${draft.quantityPurchased}, price=${draft.purchasePriceVnd}, lineTotal=$lineTotalVnd")
                val item = PurchaseItem(
                    purchaseTicketId = insertedTicket.id,
                    productId = draft.productId,
                    supplierId = draft.supplierId,
                    unitId = draft.unitId,
                    expiryDate = draft.expiryDate,
                    quantityPurchased = draft.quantityPurchased,
                    purchasePriceVnd = draft.purchasePriceVnd,
                    lineTotalVnd = lineTotalVnd
                )
                supabaseClient.postgrest[SupabaseTables.PURCHASE_ITEMS]
                    .insert(item)
                Log.d("PurchaseRepository", "createTicket: item inserted successfully")
            }

            Result.Success(insertedTicket.id)
        } catch (e: Exception) {
            Log.e("PurchaseRepository", "createTicket: failed - ${e.message}", e)
            Result.Error(e.message ?: "Lỗi khi tạo phiếu nhập", e)
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
            Result.Error(e.message ?: "Lỗi khi hủy phiếu nhập", e)
        }
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
