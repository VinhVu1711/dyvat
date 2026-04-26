package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.Category
import com.vinh.dyvat.data.model.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    fun getAllCategories(): Flow<Result<List<Category>>> = flow {
        emit(Result.Loading)
        try {
            val response = supabaseClient
                .from("categories")
                .select()
                .decodeList<Category>()

            emit(Result.Success(response))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to fetch categories", e))
        }
    }

    suspend fun insertCategory(name: String): Result<Category> {
        return try {
            val category = Category(name = name)

            val response = supabaseClient
                .from("categories")
                .insert(category) {
                    select()
                }
                .decodeSingle<Category>()

            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to insert category", e)
        }
    }

    suspend fun updateCategory(id: String, name: String): Result<Category> {
        return try {
            val response = supabaseClient
                .from("categories")
                .update({
                    set("name", name)
                }) {
                    select()
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<Category>()

            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update category", e)
        }
    }

    suspend fun deleteCategory(id: String): Result<Unit> {
        return try {
            supabaseClient
                .from("categories")
                .delete {
                    filter {
                        eq("id", id)
                    }
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete category", e)
        }
    }
}