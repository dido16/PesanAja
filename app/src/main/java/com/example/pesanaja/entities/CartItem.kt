package com.example.pesanaja.entities

import java.io.Serializable

data class CartItem(
    val menuId: Int,
    val menuName: String,
    val price: Int,
    var quantity: Int,
    var notes: String = "",
    var perluLevel: Boolean = false,
    var levelId: Int? = null,
    var extraCost: Int = 0
) : Serializable