package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.UnitModel
import com.vinh.dyvat.data.remote.SupabaseTables
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
    fun getAll(activeOnly: Boolean = true): Flow<Result<List<UnitModel>>> = flow {
        emit(Result.Loading)
        try {
            val all = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select()
                .decodeList<UnitModel>()
            val filtered = if (activeOnly) all.filter { it.isActive } else all
            emit(Result.Success(filtered))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải đơn vị tính", e))
        }
    }

    suspend fun getById(id: String): Result<UnitModel> {
        return try {
            val response = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select { filter { eq("id", id) } }
                .decodeSingle<UnitModel>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi tải đơn vị tính", e)
        }
    }

    suspend fun insert(name: String): Result<UnitModel> {
        return try {
            val unit = UnitModel(name = name)
            val response = supabaseClient.postgrest[SupabaseTables.UNITS]
                .insert(unit)
                .decodeSingle<UnitModel>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi thêm đơn vị tính", e)
        }
    }

    suspend fun update(id: String, name: String): Result<UnitModel> {
        return try {
            val response = supabaseClient.postgrest[SupabaseTables.UNITS]
                .update({ set("name", name) }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeSingle<UnitModel>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi cập nhật đơn vị tính", e)
        }
    }

    suspend fun delete(id: String): Result<Unit> {
        return try {
            supabaseClient.postgrest[SupabaseTables.UNITS]
                .delete { filter { eq("id", id) } }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi xóa đơn vị tính", e)
        }
    }
}
