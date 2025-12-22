package com.example.pesanaja.entities

import com.google.gson.annotations.SerializedName

data class OrderResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)