package com.vinh.dyvat.data.repository

import android.util.Log
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.data.remote.SupabaseTables
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
            val supplier = Supplier(name = name, phone = phone)
            val response = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .insert(supplier)
                .decodeSingle<Supplier>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi thêm nhà cung cấp", e)
        }
    }

    suspend fun update(id: String, name: String, phone: String?): Result<Supplier> {
        return try {
            val response = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .update({
                    set("name", name)
                    set("phone", phone)
                }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeSingle<Supplier>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi cập nhật nhà cung cấp", e)
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
