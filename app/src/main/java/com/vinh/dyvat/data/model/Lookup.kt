package com.vinh.dyvat.data.model

data class LookupItem(
    val id: String,
    val name: String
) {
    override fun toString() = name
}

data class ProductLookup(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    @Suppress("PropertyName")
    val default_sale_price_vnd: Long = 0L,
    val unitId: String = "",
    val unitName: String = ""
)

data class ProductLookupWithStock(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    @Suppress("PropertyName")
    val default_sale_price_vnd: Long = 0L,
    val unitId: String = "",
    val unitName: String = "",
    val hasStock: Boolean = false
)
