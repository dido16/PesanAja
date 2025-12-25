package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Wrapper utama respon API
data class OrderResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val orderData: OrderData?
) : Serializable

// Data Detail Order
data class OrderData(
    @SerializedName("id") val id: Int,
    @SerializedName("table_id") val tableId: Int,
    @SerializedName("customer_name") val customerName: String?,

    // Harga (Double biar aman buat matematika)
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("tax_amount") val taxAmount: Double,
    @SerializedName("final_total") val finalTotal: Double,

    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,

    // --- TAMBAHAN PENTING: LIST ITEM BELANJAAN ---
    // Ini yang bikin error sebelumnya karena belum ada
    @SerializedName("items") val items: List<OrderItem>? = null
) : Serializable

// Class Baru: Struktur per Item Makanan
data class OrderItem(
    @SerializedName("id") val id: Int,
    @SerializedName("menu_id") val menuId: Int,

    @SerializedName("menu_name") val menuName: String?, // Nama snapshot
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Double,
    @SerializedName("extra_cost") val extraCost: Double = 0.0,
    @SerializedName("notes") val notes: String?,

    // Relasi ke Menu (Optional, buat jaga-jaga kalau server kirim nested object)
    @SerializedName("menu") val menuData: MenuModel? = null
) : Serializable