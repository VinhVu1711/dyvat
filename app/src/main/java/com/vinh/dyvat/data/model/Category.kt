package com.vinh.dyvat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class Supplier(
    val id: String = "",
    val name: String = "",
    val phone: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)
