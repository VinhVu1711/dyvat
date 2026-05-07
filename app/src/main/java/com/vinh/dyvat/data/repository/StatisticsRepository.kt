package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.DailySummary
import com.vinh.dyvat.data.model.PurchaseExportRow
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.SaleExportRow
import com.vinh.dyvat.data.remote.SupabaseViews
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun getDailySummary(
        startDate: String,
        endDate: String
    ): Flow<Result<List<DailySummary>>> = flow {
        emit(Result.Loading)
        try {
            val data = supabaseClient.postgrest[SupabaseViews.V_DAILY_BUSINESS_SUMMARY]
                .select {
                    filter {
                        gte("business_date", startDate)
                        lte("business_date", endDate)
                    }
                    order("business_date", Order.ASCENDING)
                }
                .decodeList<DailySummary>()

            emit(Result.Success(data))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải thống kê", e))
        }
    }

    fun getMonthlySummary(year: Int, month: Int): Flow<Result<List<DailySummary>>> = flow {
        emit(Result.Loading)
        try {
            val monthStr = month.toString().padStart(2, '0')
            val startDate = "$year-$monthStr-01"

            val daysInMonth = when (month) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> if (isLeapYear(year)) 29 else 28
                else -> 30
            }
            val endDate = "$year-$monthStr-$daysInMonth"

            val data = supabaseClient.postgrest[SupabaseViews.V_DAILY_BUSINESS_SUMMARY]
                .select {
                    filter {
                        gte("business_date", startDate)
                        lte("business_date", endDate)
                    }
                    order("business_date", Order.ASCENDING)
                }
                .decodeList<DailySummary>()

            emit(Result.Success(data))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải thống kê tháng", e))
        }
    }

    fun getYearlySummary(year: Int): Flow<Result<List<DailySummary>>> = flow {
        emit(Result.Loading)
        try {
            val startDate = "$year-01-01"
            val endDate = "$year-12-31"

            val data = supabaseClient.postgrest[SupabaseViews.V_DAILY_BUSINESS_SUMMARY]
                .select {
                    filter {
                        gte("business_date", startDate)
                        lte("business_date", endDate)
                    }
                    order("business_date", Order.ASCENDING)
                }
                .decodeList<DailySummary>()

            emit(Result.Success(data))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải thống kê năm", e))
        }
    }

    suspend fun getPurchaseExportRows(
        startDate: String,
        endDate: String,
        page: Int,
        pageSize: Int = EXPORT_PAGE_SIZE
    ): Result<List<PurchaseExportRow>> {
        return try {
            val from = (page * pageSize).toLong()
            val to = from + pageSize - 1
            val data = supabaseClient.postgrest[SupabaseViews.V_PURCHASE_EXPORT_DETAILS]
                .select {
                    filter {
                        gte("purchase_date", startDate)
                        lte("purchase_date", endDate)
                    }
                    order("purchase_date", Order.ASCENDING)
                    order("ticket_code", Order.ASCENDING)
                    order("product_name", Order.ASCENDING)
                    range(from, to)
                }
                .decodeList<PurchaseExportRow>()

            Result.Success(data)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi tải chi tiết nhập hàng", e)
        }
    }

    suspend fun getSaleExportRows(
        startDate: String,
        endDate: String,
        page: Int,
        pageSize: Int = EXPORT_PAGE_SIZE
    ): Result<List<SaleExportRow>> {
        return try {
            val from = (page * pageSize).toLong()
            val to = from + pageSize - 1
            val data = supabaseClient.postgrest[SupabaseViews.V_SALE_EXPORT_DETAILS]
                .select {
                    filter {
                        gte("sale_date", startDate)
                        lte("sale_date", endDate)
                    }
                    order("sale_date", Order.ASCENDING)
                    order("ticket_code", Order.ASCENDING)
                    order("product_name", Order.ASCENDING)
                    range(from, to)
                }
                .decodeList<SaleExportRow>()

            Result.Success(data)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi tải chi tiết bán hàng", e)
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    companion object {
        const val EXPORT_PAGE_SIZE = 1000
    }
}
