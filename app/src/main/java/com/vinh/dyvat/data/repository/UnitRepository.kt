package com.vinh.dyvat.data.repository

import android.util.Log
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.UnitModel
import com.vinh.dyvat.data.remote.SupabaseTables
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UnitRepository"

@Singleton
class UnitRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun getAll(activeOnly: Boolean = true): Flow<Result<List<UnitModel>>> = flow {
        emit(Result.Loading)
        try {
            Log.d(TAG, "getAll: fetching units, activeOnly=$activeOnly")
            val all = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select()
                .decodeList<UnitModel>()
            Log.d(TAG, "getAll: raw decoded ${all.size} units, all=$all")
            val filtered = if (activeOnly) all.filter { it.isActive } else all
            Log.d(TAG, "getAll: fetched ${all.size} units, filtered to ${filtered.size}, data=$filtered")
            emit(Result.Success(filtered))
        } catch (e: Exception) {
            Log.e(TAG, "getAll: error - ${e.message}", e)
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
            val id = UUID.randomUUID().toString()
            val unit = UnitInsert(id = id, name = name.trim())
            supabaseClient.postgrest[SupabaseTables.UNITS].insert(unit)
            getById(id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi thêm đơn vị tính", e)
        }
    }

    suspend fun update(id: String, name: String): Result<UnitModel> {
        return try {
            supabaseClient.postgrest[SupabaseTables.UNITS]
                .update({ set("name", name.trim()) }) {
                    filter { eq("id", id) }
                }
            getById(id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi cập nhật đơn vị tính", e)
        }
    }

    suspend fun setActive(id: String, isActive: Boolean): Result<UnitModel> {
        return try {
            supabaseClient.postgrest[SupabaseTables.UNITS]
                .update({ set("is_active", isActive) }) {
                    filter { eq("id", id) }
                }
            getById(id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi cập nhật trạng thái đơn vị tính", e)
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

@Serializable
private data class UnitInsert(
    val id: String,
    val name: String,
    @SerialName("is_active")
    val isActive: Boolean = true
)
