package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MenuModel(
    @SerializedName("id") val id: Int,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("price") val price: Int,
    @SerializedName("has_level") val hasLevel: Int, // Ubah dari String ke Int (0/1)

    // 👇 TAMBAHAN: Serialization untuk mengambil data dari tabel levels Laravel
    @SerializedName("levels") val levels: List<LevelModel>?
) : Serializable

// 👇 Tambahkan class ini di bawahnya
data class LevelModel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("extra_cost") val extraCost: Int
) : Serializable