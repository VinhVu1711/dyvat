package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.AvailableLot
import com.vinh.dyvat.data.model.PurchaseItem
import com.vinh.dyvat.data.model.PurchaseTicket
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.SaleItem
import com.vinh.dyvat.data.model.SaleItemWithDetails
import com.vinh.dyvat.data.model.SaleTicket
import com.vinh.dyvat.data.model.SaleTicketCard
import com.vinh.dyvat.data.model.SaleTicketStatus
import com.vinh.dyvat.data.model.UnitModel
import com.vinh.dyvat.data.remote.SupabaseTables
import com.vinh.dyvat.data.remote.SupabaseViews
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun getTicketCards(
        startDate: String? = null,
        endDate: String? = null
    ): Flow<Result<List<SaleTicketCard>>> = flow {
        emit(Result.Loading)
        try {
            val all = supabaseClient.postgrest[SupabaseViews.V_SALE_TICKET_CARDS]
                .select()
                .decodeList<SaleTicketCard>()

            var filtered = all
            if (startDate != null) {
                filtered = filtered.filter { it.saleDate >= startDate }
            }
            if (endDate != null) {
                filtered = filtered.filter { it.saleDate <= endDate }
            }

            emit(Result.Success(filtered))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải phiếu bán", e))
        }
    }

    suspend fun getTicketById(id: String): Result<SaleTicket> {
        return try {
            val response = supabaseClient.postgrest[SupabaseTables.SALE_TICKETS]
                .select { filter { eq("id", id) } }
                .decodeSingle<SaleTicket>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi tải phiếu bán", e)
        }
    }

    fun getItemsByTicketId(ticketId: String): Flow<Result<List<SaleItemWithDetails>>> = flow {
        emit(Result.Loading)
        try {
            val items = supabaseClient.postgrest[SupabaseTables.SALE_ITEMS]
                .select { filter { eq("sale_ticket_id", ticketId) } }
                .decodeList<SaleItem>()

            val products = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .select().decodeList<com.vinh.dyvat.data.model.Product>()
                .associateBy { it.id }

            val purchaseItems = supabaseClient.postgrest[SupabaseTables.PURCHASE_ITEMS]
                .select().decodeList<PurchaseItem>()
                .associateBy { it.id }

            val purchaseTickets = supabaseClient.postgrest[SupabaseTables.PURCHASE_TICKETS]
                .select().decodeList<PurchaseTicket>()
                .associateBy { it.id }

            val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select().decodeList<UnitModel>()
                .associateBy { it.id }

            val result = items.map { item ->
                val product = products[item.productId]
                val lot = purchaseItems[item.purchaseItemId]
                val ticket = lot?.let { purchaseTickets[it.purchaseTicketId] }
                SaleItemWithDetails(
                    item = item,
                    productName = product?.name ?: "",
                    productCode = product?.code ?: "",
                    lotCode = ticket?.code ?: "",
                    expiryDate = lot?.expiryDate,
                    unitName = units[item.unitId]?.name ?: ""
                )
            }
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải chi tiết phiếu bán", e))
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
                .select().decodeList<PurchaseTicket>()
                .associateBy { it.id }

            val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select().decodeList<UnitModel>()
                .associateBy { it.id }

            val result = items.mapNotNull { item ->
                val ticket = purchaseTickets[item.purchaseTicketId] ?: return@mapNotNull null
                val unitName = units[item.unitId]?.name ?: ""
                AvailableLot.fromPurchaseItem(item, ticket.code, unitName)
            }

            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải lô hàng", e))
        }
    }

    suspend fun createTicket(
        saleDate: String,
        items: List<SaleItemDraft>
    ): Result<String> {
        return try {
            val ticket = SaleTicket(saleDate = saleDate)
            val insertedTicket = supabaseClient.postgrest[SupabaseTables.SALE_TICKETS]
                .insert(ticket)
                .decodeSingle<SaleTicket>()

            for (draft in items) {
                val item = SaleItem(
                    saleTicketId = insertedTicket.id,
                    productId = draft.productId,
                    purchaseItemId = draft.purchaseItemId,
                    unitId = draft.unitId,
                    quantitySold = draft.quantitySold,
                    salePriceVnd = draft.salePriceVnd
                )
                supabaseClient.postgrest[SupabaseTables.SALE_ITEMS]
                    .insert(item)
            }

            Result.Success(insertedTicket.id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi tạo phiếu bán", e)
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
            Result.Error(e.message ?: "Lỗi khi hủy phiếu bán", e)
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
