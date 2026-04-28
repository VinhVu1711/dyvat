package com.vinh.dyvat.data.repository
import android.util.Log
import com.vinh.dyvat.data.model.Category
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.remote.SupabaseTables
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CategoryRepository"

@Singleton
class CategoryRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun getAll(activeOnly: Boolean = true): Flow<Result<List<Category>>> = flow {
        emit(Result.Loading)
        try {
            Log.d(TAG, "getAll: fetching categories, activeOnly=$activeOnly")
            val all = supabaseClient.postgrest[SupabaseTables.CATEGORIES].select().decodeList<Category>()
            val filtered = if (activeOnly) all.filter { it.isActive } else all
            Log.d(TAG, "getAll: fetched ${all.size} categories, filtered to ${filtered.size}")
            emit(Result.Success(filtered))
        } catch (e: Exception) {
            Log.e(TAG, "getAll: error - ${e.message}", e)
            emit(Result.Error(e.message ?: "Lỗi khi tải danh mục", e))
        }
    }

    suspend fun getById(id: String): Result<Category> {
        return try {
            val response = supabaseClient.postgrest[SupabaseTables.CATEGORIES]
                .select { filter { eq("id", id) } }
                .decodeSingle<Category>()
            Result.Success(response)
        } catch (e: Exception) {
            Log.e(TAG, "getById: error - ${e.message}", e)
            Result.Error(e.message ?: "Lỗi khi tải danh mục", e)
        }
    }

    suspend fun insert(name: String): Result<Category> {
        return try {
            Log.d(TAG, "insert: name='$name'")
            val category = Category(name = name)
            Log.d(TAG, "insert: sending to Supabase - $category")
            val response = supabaseClient.postgrest[SupabaseTables.CATEGORIES]
                .insert(category)
                .decodeSingle<Category>()
            Log.d(TAG, "insert: success - ${response.id}, ${response.name}")
            Result.Success(response)
        } catch (e: Exception) {
            Log.e(TAG, "insert: error - ${e.message}", e)
            e.cause?.let { Log.e(TAG, "insert: caused by - ${it.message}", it) }
            Result.Error(e.message ?: "Lỗi khi thêm danh mục", e)
        }
    }

    suspend fun update(id: String, name: String): Result<Category> {
        return try {
            Log.d(TAG, "update: id=$id, name='$name'")
            val response = supabaseClient.postgrest[SupabaseTables.CATEGORIES]
                .update({ set("name", name) }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeSingle<Category>()
            Result.Success(response)
        } catch (e: Exception) {
            Log.e(TAG, "update: error - ${e.message}", e)
            Result.Error(e.message ?: "Lỗi khi cập nhật danh mục", e)
        }
    }

    suspend fun delete(id: String): Result<Unit> {
        return try {
            supabaseClient.postgrest[SupabaseTables.CATEGORIES]
                .delete { filter { eq("id", id) } }
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "delete: error - ${e.message}", e)
            Result.Error(e.message ?: "Lỗi khi xóa danh mục", e)
        }
    }
}
