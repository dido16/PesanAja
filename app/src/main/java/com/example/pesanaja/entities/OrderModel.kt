package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class OrderModel(
    @SerializedName("id") val id: Int,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("table_id") val tableId: Int,

    // Field penting buat History
    @SerializedName("status") val status: String,
    @SerializedName("final_total") val finalTotal: Double,
    @SerializedName("created_at") val createdAt: String,

    // List item belanjaan di dalam order itu
    // Pastikan di Laravel namanya 'order_items' atau 'items' (sesuaikan dengan JSON)
    @SerializedName("order_items") val items: List<CartItem>?
) : Serializable