package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName

data class OrderItemRequest(
    @SerializedName("menu_id") val menuId: Int,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("level_id") val levelId: Int?,
    @SerializedName("notes") val notes: String? = null

)