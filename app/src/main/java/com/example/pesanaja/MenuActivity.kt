package com.example.pesanaja

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.adapter.MenuAdapter
import com.example.pesanaja.entities.CartItem
import com.example.pesanaja.entities.MenuModel
import com.example.pesanaja.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class MenuActivity : AppCompatActivity(), MenuAdapter.OnCartChangeListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnCheckout: Button
    private lateinit var tvMeja: TextView
    private var nomorMeja: String = ""
    private val cartList = ArrayList<CartItem>()

    private var listMenu: List<MenuModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        recyclerView = findViewById(R.id.recyclerMenu)
        btnCheckout = findViewById(R.id.btnCheckout)
        tvMeja = findViewById(R.id.tvMeja)

        nomorMeja = intent.getStringExtra("meja") ?: "0"
        tvMeja.text = "Meja: $nomorMeja"

        recyclerView.layoutManager = LinearLayoutManager(this)

        btnCheckout.setOnClickListener {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show()
            } else {
                prosesKeCheckout()
            }
        }

        loadMenu()
        updateCheckoutButton()
    }

    private fun loadMenu() {
        ApiClient.instance.getMenus().enqueue(object : Callback<List<MenuModel>> {
            override fun onResponse(call: Call<List<MenuModel>>, response: Response<List<MenuModel>>) {
                if (response.isSuccessful) {
                    listMenu = response.body() ?: emptyList()
                    val adapter = MenuAdapter(listMenu, this@MenuActivity)
                    recyclerView.adapter = adapter
                } else {
                    Toast.makeText(this@MenuActivity, "Gagal ambil menu", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<MenuModel>>, t: Throwable) {
                Toast.makeText(this@MenuActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- LOGIC BARU: HITUNG TOTAL DARI LIST ---
    private fun updateCheckoutButton() {
        var totalItem = 0
        var totalPrice = 0.0

        for (item in cartList) {
            totalItem += item.quantity
            // Harga = (Harga Dasar + Extra Cost) * Qty
            totalPrice += (item.price + item.extraCost) * item.quantity
        }

        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        val formattedPrice = numberFormat.format(totalPrice)

        if (totalItem > 0) {
            btnCheckout.text = "Pesan ($totalItem Item) • $formattedPrice"
            btnCheckout.isEnabled = true
            btnCheckout.alpha = 1.0f
        } else {
            btnCheckout.text = "Keranjang Kosong"
            btnCheckout.isEnabled = false
            btnCheckout.alpha = 0.7f
        }
    }

    // Callback 1: Dari Tombol Plus/Minus di Menu Biasa (Tanpa Level)
    override fun onQuantityChange(menuId: Int, quantity: Int) {
        val existingItem = cartList.find { it.menuId == menuId && it.levelId == null }

        if (quantity > 0) {
            if (existingItem != null) {
                // Update qty kalau sudah ada
                existingItem.quantity = quantity
            } else {
                // Tambah baru kalau belum ada
                val menuDetail = listMenu.find { it.id == menuId }
                if (menuDetail != null) {
                    cartList.add(CartItem(
                        menuId = menuId,
                        menuName = menuDetail.name,
                        price = menuDetail.price.toDouble(),
                        quantity = quantity,
                        notes = "",
                        perluLevel = false,
                        levelId = null,
                        extraCost = 0.0,
                        menu = menuDetail
                    ))
                }
            }
        } else {
            // Kalau qty jadi 0, hapus dari list
            if (existingItem != null) cartList.remove(existingItem)
        }
        updateCheckoutButton()
    }

    override fun onVariantChange(menuId: Int, qty: Int, levelId: Int?, extraCost: Int, note: String?) {
        val menuDetail = listMenu.find { it.id == menuId } ?: return

        // Cek apakah item dengan spek SAMA PERSIS sudah ada?
        val existingItem = cartList.find {
            it.menuId == menuId &&
                    it.levelId == levelId &&
                    it.notes == (note ?: "")
        }

        if (existingItem != null) {
            existingItem.quantity = qty
            if (qty == 0) cartList.remove(existingItem)

        } else {
            // Kalau spek beda (Mie Lvl 5 vs Mie Lvl 3), BUAT BARU
            if (qty > 0) {
                cartList.add(CartItem(
                    menuId = menuId,
                    menuName = menuDetail.name,
                    price = menuDetail.price.toDouble(),
                    quantity = qty, // Pakai Qty dari parameter
                    notes = note ?: "",
                    perluLevel = (menuDetail.hasLevel == 1),
                    levelId = levelId,
                    extraCost = extraCost.toDouble(),
                    menu = menuDetail,
                    level = menuDetail.levels?.find { it.id == levelId }
                ))
            }
        }
        updateCheckoutButton()
    }

    private fun prosesKeCheckout() {
        val intent = Intent(this, CheckoutActivity::class.java)
        intent.putExtra("meja", nomorMeja)
        intent.putExtra("cart_list", cartList)
        startActivity(intent)
    }
}