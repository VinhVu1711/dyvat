package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.Supplier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    fun getAllSuppliers(): Flow<Result<List<Supplier>>> = flow {
        emit(Result.Loading)
        try {
            val response = supabaseClient
                .from("suppliers")
                .select()
                .decodeList<Supplier>()

            emit(Result.Success(response))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to fetch suppliers", e))
        }
    }

    suspend fun insertSupplier(name: String, phone: String?): Result<Supplier> {
        return try {
            val supplier = Supplier(name = name, phone = phone)

            val response = supabaseClient
                .from("suppliers")
                .insert(supplier) {
                    select()
                }
                .decodeSingle<Supplier>()

            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to insert supplier", e)
        }
    }

    suspend fun updateSupplier(id: String, name: String, phone: String?): Result<Supplier> {
        return try {
            val response = supabaseClient
                .from("suppliers")
                .update({
                    set("name", name)
                    set("phone", phone)
                }) {
                    select()
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<Supplier>()

            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update supplier", e)
        }
    }

    suspend fun deleteSupplier(id: String): Result<Unit> {
        return try {
            supabaseClient
                .from("suppliers")
                .delete {
                    filter {
                        eq("id", id)
                    }
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete supplier", e)
        }
    }
}