package com.example.pesanaja.entities

import java.io.Serializable

data class CartItem(
    val menuId: Int,
    val menuName: String,
    val price: Int,
    val quantity: Int,
    var notes: String = ""
) : Serializable