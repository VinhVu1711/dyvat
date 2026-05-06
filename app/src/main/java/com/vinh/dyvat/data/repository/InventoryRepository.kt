package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.InventoryLotCard
import com.vinh.dyvat.data.model.InventoryLotDetail
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.remote.SupabaseViews
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun getLotCardByTicketId(ticketId: String): Result<InventoryLotCard?> {
        return try {
            val cards = supabaseClient.postgrest[SupabaseViews.V_INVENTORY_LOT_CARDS]
                .select { filter { eq("purchase_ticket_id", ticketId) } }
                .decodeList<InventoryLotCard>()

            Result.Success(cards.firstOrNull())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi tải thông tin lô", e)
        }
    }

    fun getLotCards(
        startDate: String? = null,
        endDate: String? = null,
        showOutOfStock: Boolean = false
    ): Flow<Result<List<InventoryLotCard>>> = flow {
        emit(Result.Loading)
        try {
            val all = supabaseClient.postgrest[SupabaseViews.V_INVENTORY_LOT_CARDS]
                .select()
                .decodeList<InventoryLotCard>()

            val nearestExpiryByLotKey = supabaseClient.postgrest[SupabaseViews.V_INVENTORY_LOT_DETAILS]
                .select()
                .decodeList<InventoryLotDetail>()
                .filter { it.quantityRemaining > 0 && !it.expiryDate.isNullOrBlank() }
                .groupBy { it.lotKey() }
                .mapValues { (_, details) ->
                    details.mapNotNull { it.expiryDate?.toDateOnlyOrNull() }.minOrNull()
                }

            var filtered = all.map { lot ->
                lot.copy(nearestExpiryDate = nearestExpiryByLotKey[lot.lotKey()])
            }

            if (startDate != null) {
                filtered = filtered.filter { it.purchaseDate >= startDate }
            }
            if (endDate != null) {
                filtered = filtered.filter { it.purchaseDate <= endDate }
            }
            if (!showOutOfStock) {
                filtered = filtered.filter { it.totalRemainingQuantity > 0 }
            }

            emit(Result.Success(filtered))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải kho", e))
        }
    }

    fun getLotDetails(ticketId: String): Flow<Result<List<InventoryLotDetail>>> = flow {
        emit(Result.Loading)
        try {
            val all = supabaseClient.postgrest[SupabaseViews.V_INVENTORY_LOT_DETAILS]
                .select { filter { eq("purchase_ticket_id", ticketId) } }
                .decodeList<InventoryLotDetail>()

            val withStock = all.filter { it.quantityRemaining > 0 }

            emit(Result.Success(withStock))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải chi tiết lô", e))
        }
    }
}

private fun String.toDateOnlyOrNull(): String? {
    return split("T").firstOrNull()?.takeIf { it.isNotBlank() }
}

private fun InventoryLotDetail.lotKey(): String {
    return purchaseTicketId.ifBlank { lotCode }
}

private fun InventoryLotCard.lotKey(): String {
    return purchaseTicketId.ifBlank { lotCode }
}
