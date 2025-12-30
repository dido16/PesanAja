package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Wrapper utama respon API
data class OrderResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,

    // PERBAIKAN 1: Ganti nama variabel jadi 'data' biar sinkron sama CheckoutActivity
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

    // PERBAIKAN 2: Di Laravel relasi biasanya bernama "order_items"
    @SerializedName("order_items") val items: List<OrderItem>? = null
) : Serializable

// Class Baru: Struktur per Item Makanan
data class OrderItem(
    @SerializedName("id") val id: Int,
    @SerializedName("menu_id") val menuId: Int,

    // Di DB biasanya tidak simpan menu_name, tapi diambil dari relasi menu
    @SerializedName("quantity") val quantity: Int,

    // PERBAIKAN 3: Di Laravel kolomnya 'unit_price', bukan 'price'
    @SerializedName("unit_price") val price: Double,

    // Di Laravel kolomnya 'level_id', extra cost biasanya diambil dari relasi Level atau disimpan terpisah
    // Kalau di DB kamu simpan total price, sesuaikan. Tapi defaultnya begini:
    @SerializedName("notes") val notes: String?,

    // Relasi ke Menu (PENTING: Buat ambil nama menu & gambar)
    @SerializedName("menu") val menuData: MenuModel? = null,

    // Relasi ke Level (Buat ambil nama level & extra cost)
    @SerializedName("level") val levelData: LevelModel? = null

) : Serializable