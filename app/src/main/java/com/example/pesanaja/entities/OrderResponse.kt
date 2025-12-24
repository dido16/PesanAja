package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class OrderResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val orderData: OrderData? // Menangkap objek 'data' dari Laravel
) : Serializable

data class OrderData(
    @SerializedName("id") val id: Int,
    @SerializedName("table_id") val tableId: Int,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("subtotal") val subtotal: Double,    // UBAH KE DOUBLE
    @SerializedName("tax_amount") val taxAmount: Double, // UBAH KE DOUBLE
    @SerializedName("final_total") val finalTotal: Double, // UBAH KE DOUBLE
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String
) : Serializable