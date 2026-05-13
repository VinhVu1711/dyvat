package com.vinh.dyvat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductImportSummary(
    val categoriesToCreate: Int = 0,
    val categoriesToReuse: Int = 0,
    val unitsToCreate: Int = 0,
    val unitsToReuse: Int = 0,
    val suppliersToCreate: Int = 0,
    val suppliersToReuse: Int = 0,
    val productsToImport: Int = 0,
    val errorCount: Int = 0,
    val hasMoreErrors: Boolean = false
)

@Serializable
data class ProductImportError(
    val sheet: String = "",
    val rowNumber: Int = 0,
    val column: String = "",
    val value: String = "",
    val message: String = "",
    val suggestion: String = ""
)

@Serializable
data class ProductImportValidateResponse(
    val success: Boolean = false,
    val summary: ProductImportSummary = ProductImportSummary(),
    val errors: List<ProductImportError> = emptyList(),
    val importToken: String? = null,
    val message: String = ""
)

@Serializable
data class ProductImportCommitResult(
    val categoriesCreated: Int = 0,
    val categoriesReused: Int = 0,
    val unitsCreated: Int = 0,
    val unitsReused: Int = 0,
    val suppliersCreated: Int = 0,
    val suppliersReused: Int = 0,
    val productsCreated: Int = 0
)

@Serializable
data class ProductImportCommitResponse(
    val success: Boolean = false,
    val result: ProductImportCommitResult = ProductImportCommitResult(),
    val message: String = ""
)

