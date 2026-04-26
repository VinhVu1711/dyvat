package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.Category
import com.vinh.dyvat.data.model.ImportItem
import com.vinh.dyvat.data.model.ImportItemWithDetails
import com.vinh.dyvat.data.model.ItemStatus
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.data.model.UnitModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    fun getAllImportItems(): Flow<Result<List<ImportItemWithDetails>>> = flow {
        emit(Result.Loading)
        try {
            val items = supabaseClient
                .from("import_items")
                .select()
                .decodeList<ImportItem>()

            val categories = supabaseClient
                .from("categories")
                .select()
                .decodeList<Category>()
                .associateBy { it.id }

            val units = supabaseClient
                .from("units")
                .select()
                .decodeList<UnitModel>()
                .associateBy { it.id }

            val suppliers = supabaseClient
                .from("suppliers")
                .select()
                .decodeList<Supplier>()
                .associateBy { it.id }

            val itemsWithDetails = items.map { item ->
                ImportItemWithDetails(
                    importItem = item,
                    categoryName = item.categoryId?.let { categories[it]?.name } ?: "",
                    unitName = units[item.unitId]?.name ?: "",
                    supplierName = item.supplierId?.let { suppliers[it]?.name } ?: ""
                )
            }

            emit(Result.Success(itemsWithDetails))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to fetch import items", e))
        }
    }

    fun getItemsForSale(): Flow<Result<List<ImportItemWithDetails>>> = flow {
        emit(Result.Loading)
        try {
            val items = supabaseClient
                .from("import_items")
                .select {
                    filter {
                        eq("status", "for_sale")
                    }
                }
                .decodeList<ImportItem>()

            val categories = supabaseClient
                .from("categories")
                .select()
                .decodeList<Category>()
                .associateBy { it.id }

            val units = supabaseClient
                .from("units")
                .select()
                .decodeList<UnitModel>()
                .associateBy { it.id }

            val suppliers = supabaseClient
                .from("suppliers")
                .select()
                .decodeList<Supplier>()
                .associateBy { it.id }

            val itemsWithDetails = items.map { item ->
                ImportItemWithDetails(
                    importItem = item,
                    categoryName = item.categoryId?.let { categories[it]?.name } ?: "",
                    unitName = units[item.unitId]?.name ?: "",
                    supplierName = item.supplierId?.let { suppliers[it]?.name } ?: ""
                )
            }

            emit(Result.Success(itemsWithDetails))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to fetch items for sale", e))
        }
    }

    suspend fun insertImportItem(item: ImportItem): Result<ImportItem> {
        return try {
            val response = supabaseClient
                .from("import_items")
                .insert(item) {
                    select()
                }
                .decodeSingle<ImportItem>()

            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to insert import item", e)
        }
    }

    suspend fun updateImportItem(id: String, item: ImportItem): Result<ImportItem> {
        return try {
            val response = supabaseClient
                .from("import_items")
                .update({
                    set("product_name", item.productName)
                    set("category_id", item.categoryId)
                    set("unit_id", item.unitId)
                    set("supplier_id", item.supplierId)
                    set("total_import_amount", item.totalImportAmount)
                    set("unit_price", item.unitPrice)
                    set("total_quantity", item.totalQuantity)
                    set("quantity_for_sale", item.quantityForSale)
                    set("quantity_in_stock", item.quantityInStock)
                    set("status", item.status)
                    set("sale_location", item.saleLocation)
                    set("expiry_date", item.expiryDate)
                    set("notes", item.notes)
                }) {
                    select()
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<ImportItem>()

            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update import item", e)
        }
    }

    suspend fun updateItemStatus(
        id: String,
        status: ItemStatus,
        quantityForSale: Int?,
        saleLocation: String?
    ): Result<ImportItem> {
        return try {
            val current = supabaseClient
                .from("import_items")
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<ImportItem>()

            val updated = current.copy(
                status = status,
                quantityForSale = quantityForSale ?: current.quantityForSale,
                saleLocation = if (status == ItemStatus.FOR_SALE) saleLocation else null,
                quantityInStock = current.totalQuantity - (quantityForSale ?: current.quantityForSale)
            )

            val response = supabaseClient
                .from("import_items")
                .update({
                    set("status", updated.status)
                    set("quantity_for_sale", updated.quantityForSale)
                    set("quantity_in_stock", updated.quantityInStock)
                    set("sale_location", updated.saleLocation)
                }) {
                    select()
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<ImportItem>()

            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update item status", e)
        }
    }

    suspend fun deleteImportItem(id: String): Result<Unit> {
        return try {
            supabaseClient
                .from("import_items")
                .delete {
                    filter {
                        eq("id", id)
                    }
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete import item", e)
        }
    }

    fun getCategories(): Flow<Result<List<Category>>> = flow {
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

    fun getUnits(): Flow<Result<List<UnitModel>>> = flow {
        emit(Result.Loading)
        try {
            val response = supabaseClient
                .from("units")
                .select()
                .decodeList<UnitModel>()

            emit(Result.Success(response))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Failed to fetch units", e))
        }
    }

    fun getSuppliers(): Flow<Result<List<Supplier>>> = flow {
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
}