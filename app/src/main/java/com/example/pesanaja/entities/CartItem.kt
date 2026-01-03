package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CartItem(
    @SerializedName("menu_id") var menuId: Int,

    // UBAH KE DOUBLE: Biar aman hitung-hitungannya & sinkron sama OrderResponse
    @SerializedName("price") var price: Double,

    @SerializedName("quantity") var quantity: Int,
    @SerializedName("notes") var notes: String? = "",

    // --- PERBAIKAN DI SINI (UBAH 'val' JADI 'var') ---
    // Agar bisa diedit/diupdate dari CheckoutActivity
    @SerializedName("menu") var menu: MenuModel? = null,
    @SerializedName("level") var level: LevelModel? = null,
    // --------------------------------------------------

    var perluLevel: Boolean = false,
    @SerializedName("level_id") var levelId: Int? = null,

    // UBAH KE DOUBLE: Biar konsisten sama price
    @SerializedName("extra_cost") var extraCost: Double = 0.0,

    @SerializedName("menu_name") var menuName: String? = null

) : Serializable