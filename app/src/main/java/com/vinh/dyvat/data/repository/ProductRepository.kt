package com.vinh.dyvat.data.repository

import android.util.Log
import java.util.UUID
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
    fun getAll(
        activeOnly: Boolean = true,
        page: Int = 0,
        pageSize: Int = 20,
        sortField: ProductSortField = ProductSortField.NAME,
        ascending: Boolean = true,
        categoryId: String? = null,
        supplierId: String? = null
    ): Flow<Result<List<ProductWithDetails>>> = flow {
        emit(Result.Loading)
        try {
            val offset = page * pageSize
            Log.d(
                "ProductRepository",
                "getAll: page=$page, pageSize=$pageSize, offset=$offset, activeOnly=$activeOnly, sortField=$sortField, ascending=$ascending, categoryId=$categoryId, supplierId=$supplierId"
            )
            val products = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .select {
                    if (activeOnly || categoryId != null || supplierId != null) {
                        filter {
                            if (activeOnly) {
                                eq("status", "active")
                            }
                            categoryId?.let { eq("category_id", it) }
                            supplierId?.let { eq("supplier_id", it) }
                        }
                    }
                }
                .decodeList<Product>()
                .sortedWith(productComparator(sortField, ascending))
                .drop(offset)
                .take(pageSize)

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
            Log.d("ProductRepository", "getAll: fetched ${result.size} products")
            emit(Result.Success(result))
        } catch (e: Exception) {
            Log.e("ProductRepository", "getAll: error - ${e.message}", e)
            emit(Result.Error(e.message ?: "Lỗi khi tải sản phẩm", e))
        }
    }

    private fun productComparator(
        sortField: ProductSortField,
        ascending: Boolean
    ): Comparator<Product> {
        val comparator = when (sortField) {
            ProductSortField.NAME -> compareBy<Product> { it.name.lowercase() }
            ProductSortField.CODE -> compareBy { it.code.lowercase() }
            ProductSortField.SALE_PRICE -> compareBy { it.defaultSalePriceVnd }
            ProductSortField.CREATED_AT -> compareBy { it.createdAt }
        }.thenBy { it.id }

        return if (ascending) comparator else comparator.reversed()
    }

    fun search(query: String, activeOnly: Boolean = true): Flow<Result<List<ProductWithDetails>>> =
        flow {
            emit(Result.Loading)
            try {
                val products = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                    .select { filter { ilike("name", "%$query%") } }
                    .decodeList<Product>()
                    .let { list -> if (activeOnly) list.filter { it.status == ProductStatus.ACTIVE } else list }

                val codeProducts = supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                    .select { filter { ilike("code", "%$query%") } }
                    .decodeList<Product>()
                    .let { list -> if (activeOnly) list.filter { it.status == ProductStatus.ACTIVE } else list }
                    .filter { p -> products.none { it.id == p.id } }

                val categories = supabaseClient.postgrest[SupabaseTables.CATEGORIES]
                    .select().decodeList<com.vinh.dyvat.data.model.Category>()
                    .associateBy { it.id }

                val units = supabaseClient.postgrest[SupabaseTables.UNITS]
                    .select().decodeList<UnitModel>()
                    .associateBy { it.id }

                val suppliers = supabaseClient.postgrest[SupabaseTables.SUPPLIERS]
                    .select().decodeList<Supplier>()
                    .associateBy { it.id }

                fun mapToDetails(p: Product) = ProductWithDetails(
                    product = p,
                    categoryName = categories[p.categoryId]?.name ?: "",
                    unitName = units[p.unitId]?.name ?: "",
                    supplierName = suppliers[p.supplierId]?.name ?: ""
                )

                val result = (products.map { mapToDetails(it) } + codeProducts.map { mapToDetails(it) })
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

            Result.Success(
                ProductWithDetails(
                    product = p,
                    categoryName = category.name,
                    unitName = unit.name,
                    supplierName = supplier.name
                )
            )
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
            val id = UUID.randomUUID().toString()
            val product = Product(
                id = id,
                name = name,
                categoryId = categoryId,
                unitId = unitId,
                supplierId = supplierId,
                defaultPurchasePriceVnd = purchasePrice,
                defaultSalePriceVnd = salePrice
            )
            Log.d("ProductRepository", "insert: inserting product=$product")
            supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .insert(product)
            Log.d("ProductRepository", "insert: done, fetching by id")
            val saved = getById(id)
            Log.d("ProductRepository", "insert: saved=$saved")
            return when (saved) {
                is Result.Success -> Result.Success(saved.data.product)
                is Result.Error -> saved
                is Result.Loading -> saved
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "insert: error - ${e.message}", e)
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
            Log.d(
                "ProductRepository",
                "update: id=$id, name=$name, categoryId=$categoryId, unitId=$unitId, supplierId=$supplierId"
            )
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
                .decodeList<Product>()
                .firstOrNull()
            Log.d("ProductRepository", "update: success, response=$response")
            if (response != null) {
                Result.Success(response)
            } else {
                Result.Error("Cập nhật sản phẩm thất bại: không nhận được phản hồi từ server", null)
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "update: error - ${e.message}", e)
            return Result.Error(e.message ?: "Lỗi khi cập nhật sản phẩm", e)
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

    suspend fun delete(id: String): Result<Unit> {
        return try {
            supabaseClient.postgrest[SupabaseTables.PRODUCTS]
                .delete { filter { eq("id", id) } }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Lỗi khi xóa sản phẩm", e)
        }
    }

    suspend fun checkProductRelationships(id: String): ProductRelationships {
        return try {
            val purchaseCount = supabaseClient.postgrest[SupabaseTables.PURCHASE_ITEMS]
                .select { filter { eq("product_id", id) } }
                .decodeList<Unit>()
                .size

            val saleCount = supabaseClient.postgrest[SupabaseTables.SALE_ITEMS]
                .select { filter { eq("product_id", id) } }
                .decodeList<Unit>()
                .size

            ProductRelationships(
                hasPurchaseHistory = purchaseCount > 0,
                hasSaleHistory = saleCount > 0
            )
        } catch (e: Exception) {
            Log.e("ProductRepository", "checkProductRelationships: error - ${e.message}", e)
            ProductRelationships(hasPurchaseHistory = false, hasSaleHistory = false)
        }
    }
}

data class ProductRelationships(
    val hasPurchaseHistory: Boolean,
    val hasSaleHistory: Boolean
)

enum class ProductSortField {
    NAME,
    CODE,
    SALE_PRICE,
    CREATED_AT
}
