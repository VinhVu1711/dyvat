package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.Product
import com.vinh.dyvat.data.model.ProductStatus
import com.vinh.dyvat.data.model.ProductWithDetails
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.model.UnitModel
import com.vinh.dyvat.data.model.Supplier
import com.vinh.dyvat.data.remote.SupabaseTables
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun getAll(activeOnly: Boolean = true): Flow<Result<List<ProductWithDetails>>> = flow {
        emit(Result.Loading)
        try {
            val products = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .select()
                .decodeList<Product>()
                .let { list -> if (activeOnly) list.filter { it.status == ProductStatus.ACTIVE } else list }

            val categories = supabaseClient.postgrest[SupabaseTables.CATEGORIES]
                .select().decodeList<com.vinh.dyvat.data.model.Category>()
                .associateBy { it.id }

            val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select().decodeList<UnitModel>()
                .associateBy { it.id }

            val suppliers = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .select().decodeList<Supplier>()
                .associateBy { it.id }

            val result = products.map { p ->
                ProductWithDetails(
                    product = p,
                    categoryName = categories[p.categoryId]?.name ?: "",
                    unitName = units[p.unitId]?.name ?: "",
                    supplierName = suppliers[p.supplierId]?.name ?: ""
                )
            }
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải sản phẩm", e))
        }
    }

    fun search(query: String, activeOnly: Boolean = true): Flow<Result<List<ProductWithDetails>>> = flow {
        emit(Result.Loading)
        try {
            val products = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .select { filter { ilike("name", "%$query%") } }
                .decodeList<Product>()
                .let { list -> if (activeOnly) list.filter { it.status == ProductStatus.ACTIVE } else list }

            val categories = supabaseClient.postgrest[SupabaseTables.CATEGORIES]
                .select().decodeList<com.vinh.dyvat.data.model.Category>()
                .associateBy { it.id }

            val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select().decodeList<UnitModel>()
                .associateBy { it.id }

            val suppliers = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .select().decodeList<Supplier>()
                .associateBy { it.id }

            val result = products.map { p ->
                ProductWithDetails(
                    product = p,
                    categoryName = categories[p.categoryId]?.name ?: "",
                    unitName = units[p.unitId]?.name ?: "",
                    supplierName = suppliers[p.supplierId]?.name ?: ""
                )
            }
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tìm sản phẩm", e))
        }
    }

    fun getByCategory(categoryId: String): Flow<Result<List<ProductWithDetails>>> = flow {
        emit(Result.Loading)
        try {
            val products = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .select { filter { eq("category_id", categoryId) } }
                .decodeList<Product>()
                .filter { it.status == ProductStatus.ACTIVE }

            val categories = supabaseClient.postgrest[SupabaseTables.CATEGORIES]
                .select().decodeList<com.vinh.dyvat.data.model.Category>()
                .associateBy { it.id }

            val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select().decodeList<UnitModel>()
                .associateBy { it.id }

            val suppliers = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .select().decodeList<Supplier>()
                .associateBy { it.id }

            val result = products.map { p ->
                ProductWithDetails(
                    product = p,
                    categoryName = categories[p.categoryId]?.name ?: "",
                    unitName = units[p.unitId]?.name ?: "",
                    supplierName = suppliers[p.supplierId]?.name ?: ""
                )
            }
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải sản phẩm theo loại", e))
        }
    }

    fun getBySupplier(supplierId: String): Flow<Result<List<ProductWithDetails>>> = flow {
        emit(Result.Loading)
        try {
            val products = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .select { filter { eq("supplier_id", supplierId) } }
                .decodeList<Product>()
                .filter { it.status == ProductStatus.ACTIVE }

            val categories = supabaseClient.postgrest[SupabaseTables.CATEGORIES]
                .select().decodeList<com.vinh.dyvat.data.model.Category>()
                .associateBy { it.id }

            val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select().decodeList<UnitModel>()
                .associateBy { it.id }

            val suppliers = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .select().decodeList<Supplier>()
                .associateBy { it.id }

            val result = products.map { p ->
                ProductWithDetails(
                    product = p,
                    categoryName = categories[p.categoryId]?.name ?: "",
                    unitName = units[p.unitId]?.name ?: "",
                    supplierName = suppliers[p.supplierId]?.name ?: ""
                )
            }
            emit(Result.Success(result))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Lỗi khi tải sản phẩm theo NCC", e))
        }
    }

    suspend fun getById(id: String): Result<ProductWithDetails> {
        return try {
            val p = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .select { filter { eq("id", id) } }
                .decodeSingle<Product>()

            val category = supabaseClient.postgrest[SupabaseTables.CATEGORIES]
                .select { filter { eq("id", p.categoryId) } }
                .decodeSingle<com.vinh.dyvat.data.model.Category>()

            val unit = supabaseClient.postgrest[SupabaseTables.UNITS]
                .select { filter { eq("id", p.unitId) } }
                .decodeSingle<UnitModel>()

            val supplier = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                .select { filter { eq("id", p.supplierId) } }
                .decodeSingle<Supplier>()

            Result.Success(ProductWithDetails(
                product = p,
                categoryName = category.name,
                unitName = unit.name,
                supplierName = supplier.name
            ))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi tải sản phẩm", e)
        }
    }

    suspend fun insert(
        name: String,
        categoryId: String,
        unitId: String,
        supplierId: String,
        purchasePrice: Long,
        salePrice: Long
    ): Result<Product> {
        return try {
            val product = Product(
                name = name,
                categoryId = categoryId,
                unitId = unitId,
                supplierId = supplierId,
                defaultPurchasePriceVnd = purchasePrice,
                defaultSalePriceVnd = salePrice
            )
            val response = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .insert(product)
                .decodeSingle<Product>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi thêm sản phẩm", e)
        }
    }

    suspend fun update(
        id: String,
        name: String,
        categoryId: String,
        unitId: String,
        supplierId: String,
        purchasePrice: Long,
        salePrice: Long
    ): Result<Product> {
        return try {
            val response = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .update({
                    set("name", name)
                    set("category_id", categoryId)
                    set("unit_id", unitId)
                    set("supplier_id", supplierId)
                    set("default_purchase_price_vnd", purchasePrice)
                    set("default_sale_price_vnd", salePrice)
                }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeSingle<Product>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi cập nhật sản phẩm", e)
        }
    }

    suspend fun updateStatus(id: String, status: ProductStatus): Result<Product> {
        return try {
            val statusStr = when (status) {
                ProductStatus.ACTIVE -> "active"
                ProductStatus.DISCONTINUED -> "discontinued"
            }
            val response = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .update({ set("status", statusStr) }) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeSingle<Product>()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi cập nhật trạng thái sản phẩm", e)
        }
    }
}
