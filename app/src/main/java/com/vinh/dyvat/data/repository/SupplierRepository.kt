package com.vinh.dyvat.data.repository

import android.util.Log
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.Supplier
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

private const val TAG = "SupplierRepository"

@Singleton
class SupplierRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun getAll(activeOnly: Boolean = true): Flow<Result<List<Supplier>>> = flow {
        emit(Result.Loading)
        try {
            Log.d(TAG, "getAll: fetching suppliers, activeOnly=$activeOnly")
            val all = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .select()
                .decodeList<Supplier>()
            Log.d(TAG, "getAll: raw decoded ${all.size} suppliers, all=$all")
            val filtered = if (activeOnly) all.filter { it.isActive } else all
            Log.d(TAG, "getAll: fetched ${all.size} suppliers, filtered to ${filtered.size}, data=$filtered")
            emit(Result.Success(filtered))
        } catch (e: Exception) {
            Log.e(TAG, "getAll: error - ${e.message}", e)
            emit(Result.Error(e.message ?: "Lỗi khi tải nhà cung cấp", e))
        }
    }

    suspend fun getById(id: String): Result<Supplier> {
        return try {
            val response = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .select { filter { eq("id", id) } }
                .decodeSingle<Supplier>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi tải nhà cung cấp", e)
        }
    }

    suspend fun insert(name: String, phone: String?): Result<Supplier> {
        return try {
            val id = UUID.randomUUID().toString()
            val supplier = SupplierInsert(
                id = id,
                name = name.trim(),
                phone = phone?.trim()?.ifBlank { null }
            )
            supabaseClient.postgrest[SupabaseTables.SUPPLIERS].insert(supplier)
            getById(id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi thêm nhà cung cấp", e)
        }
    }

    suspend fun update(id: String, name: String, phone: String?): Result<Supplier> {
        return try {
            supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .update({
                    set("name", name.trim())
                    set("phone", phone?.trim()?.ifBlank { null })
                }) {
                    filter { eq("id", id) }
                }
            getById(id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi cập nhật nhà cung cấp", e)
        }
    }

    suspend fun setActive(id: String, isActive: Boolean): Result<Supplier> {
        return try {
            supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .update({ set("is_active", isActive) }) {
                    filter { eq("id", id) }
                }
            getById(id)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi cập nhật trạng thái nhà cung cấp", e)
        }
    }

    suspend fun delete(id: String): Result<Unit> {
        return try {
            supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .delete { filter { eq("id", id) } }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi xóa nhà cung cấp", e)
        }
    }
}

@Serializable
private data class SupplierInsert(
    val id: String,
    val name: String,
    val phone: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true
)
