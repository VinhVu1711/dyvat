package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.UnitModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnitRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    fun getAllUnits(): Flow<Result<List<UnitModel>>> = flow {
        emit(Result.Loading)
        try {
            val response = supabaseClient.postgrest["units"]
                .select()
                .decodeList<UnitModel>()
            emit(Result.Success(response))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to fetch units", e))
        }
    }

    suspend fun insertUnit(name: String): Result<UnitModel> {
        return try {
            val unit = UnitModel(name = name)
            val response = supabaseClient.postgrest["units"]
                .insert(unit)
                .decodeSingle<UnitModel>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to insert unit", e)
        }
    }

    suspend fun updateUnit(id: String, name: String): Result<UnitModel> {
        return try {
            val response = supabaseClient.postgrest["units"]
                .update({ set("name", name) }) {
                    select()
                    filter {
                        eq("id", id)
                    } }
                .decodeSingle<UnitModel>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update unit", e)
        }
    }

    suspend fun deleteUnit(id: String): Result<UnitModel> {
        return try {
            val response = supabaseClient.postgrest["units"]
                .delete { select()
                    filter {
                        eq("id", id)
                    } }
                .decodeSingle<UnitModel>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete unit", e)
        }
    }
}
