package com.example.pesanaja.repository

import com.example.pesanaja.entities.MenuModel
import com.example.pesanaja.entities.OrderRequest
import com.example.pesanaja.entities.OrderResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {

    // Ambil data menu
    @GET("api/menu")
    fun getMenus(): Call<List<MenuModel>>

    // Kirim pesanan ke Laravel
    @Headers("Accept: application/json")
    @POST("api/order")
    fun createOrder(@Body order: OrderRequest): Call<OrderResponse>
}