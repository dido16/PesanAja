package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Wrapper utama respon API
data class OrderResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: OrderData?
) : Serializable

// Data Detail Order
data class OrderData(
    @SerializedName("id") val id: Int,
    @SerializedName("table_id") val tableId: Int,
    @SerializedName("customer_name") val customerName: String?,

    // Harga (Double)
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("tax_amount") val taxAmount: Double,
    @SerializedName("final_total") val finalTotal: Double,

    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,

    // --- INI YANG KETINGGALAN! Tambahkan ini biar ReceiptActivity gak merah ---
    @SerializedName("payment_method") val paymentMethod: String?,
    // --------------------------------------------------------------------------

    @SerializedName("order_items") val items: List<OrderItem>? = null
) : Serializable

// Class Baru: Struktur per Item Makanan
data class OrderItem(
    @SerializedName("id") val id: Int,
    @SerializedName("menu_id") val menuId: Int,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_price") val price: Double,
    @SerializedName("notes") val notes: String?,

    // Relasi ke Menu
    @SerializedName("menu") val menuData: MenuModel? = null,

    // Relasi ke Level
    @SerializedName("level") val levelData: LevelModel? = null
) : Serializable