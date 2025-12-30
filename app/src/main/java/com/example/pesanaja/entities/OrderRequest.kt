package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName

data class OrderRequest(
    @SerializedName("meja") val meja: String,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("items") val items: List<OrderItemRequest>,
    @SerializedName("device_id") val deviceId: String
)