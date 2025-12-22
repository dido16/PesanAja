package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName

data class MenuModel(
    @SerializedName("id") val id: Int,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("price") val price: Int,
    @SerializedName("has_level") val hasLevel: Int
)