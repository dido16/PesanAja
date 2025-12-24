package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CartItem(
    @SerializedName("menu_id") var menuId: Int,
    @SerializedName("price") var price: Int,
    @SerializedName("quantity") var quantity: Int,
    @SerializedName("notes") var notes: String? = "",

    var perluLevel: Boolean = false,
    @SerializedName("level_id") var levelId: Int? = null,
    @SerializedName("extra_cost") var extraCost: Int = 0,
    @SerializedName("menu_name") var menuName: String? = null,
    @SerializedName("menu") val menuData: MenuDetail? = null

) : Serializable

data class MenuDetail(
    @SerializedName("name") val name: String,
    @SerializedName("image") val image: String?
) : Serializable