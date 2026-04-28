package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.DailySummary
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.remote.SupabaseViews
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
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
            val all = supabaseClient.postgrest[SupabaseViews.V_DAILY_BUSINESS_SUMMARY]
                .select()
                .decodeList<DailySummary>()

            val filtered = all.filter { summary ->
                summary.businessDate >= startDate && summary.businessDate <= endDate
            }

            emit(Result.Success(filtered))
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

            val all = supabaseClient.postgrest[SupabaseViews.V_DAILY_BUSINESS_SUMMARY]
                .select()
                .decodeList<DailySummary>()

            val filtered = all.filter { summary ->
                summary.businessDate >= startDate && summary.businessDate <= endDate
            }

            emit(Result.Success(filtered))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải thống kê tháng", e))
        }
    }

    fun getYearlySummary(year: Int): Flow<Result<List<DailySummary>>> = flow {
        emit(Result.Loading)
        try {
            val startDate = "$year-01-01"
            val endDate = "$year-12-31"

            val all = supabaseClient.postgrest[SupabaseViews.V_DAILY_BUSINESS_SUMMARY]
                .select()
                .decodeList<DailySummary>()

            val filtered = all.filter { summary ->
                summary.businessDate >= startDate && summary.businessDate <= endDate
            }

            emit(Result.Success(filtered))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải thống kê năm", e))
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
