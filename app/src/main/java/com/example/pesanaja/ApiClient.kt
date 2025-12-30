package com.example.pesanaja

import com.example.pesanaja.entities.MenuModel
import com.example.pesanaja.repository.ApiService
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path


object ApiClient {
    // Ganti dengan IP Laptop kamu. Jangan lupa port :8000 jika pakai php artisan serve
    private const val BASE_URL = "http://192.168.0.102:8000/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}