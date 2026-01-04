package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class OrderModel(
    @SerializedName("id") val id: Int,
    @SerializedName("customer_name") val customerName: String?,
    @SerializedName("table_id") val tableId: Int,

    @SerializedName("status") val status: String?,
    @SerializedName("final_total") val finalTotal: Double,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("order_items") val items: List<CartItem>?
) : Serializable