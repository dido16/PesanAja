package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName

data class HistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<OrderModel> // List of Order
)