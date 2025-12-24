package com.example.pesanaja.repository

import com.example.pesanaja.entities.MenuModel
import com.example.pesanaja.entities.OrderRequest
import com.example.pesanaja.entities.OrderResponse
import com.example.pesanaja.entities.HistoryResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Ambil data menu
    @GET("api/menu")
    fun getMenus(): Call<List<MenuModel>>

    // Kirim pesanan ke Laravel
    @Headers("Accept: application/json")
    @POST("api/order")
    fun createOrder(@Body order: OrderRequest): Call<OrderResponse>

    @Headers("Accept: application/json")
    @POST("api/order/{id}/pay")
    fun payOrder(@Path("id") orderId: Int): Call<OrderResponse>

    @GET("api/history")
    fun getHistory(@Query("device_id") deviceId: String): Call<HistoryResponse>
}