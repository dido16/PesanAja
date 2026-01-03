package com.example.pesanaja

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    private var cartList = ArrayList<CartItem>()

    private var listMenu: List<MenuModel> = emptyList()

    // --- FIX BUG 2: Penangkap Data Balikan dari Checkout ---
    // Ini menangkap data keranjang terbaru kalau user menghapus item di Checkout
    private val checkoutLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedCart = result.data?.getSerializableExtra("updated_cart") as? ArrayList<CartItem>
            if (updatedCart != null) {
                cartList.clear()
                cartList.addAll(updatedCart)
                updateCheckoutButton()

                // Opsional: Refresh tampilan list menu biar angkanya sinkron
                (recyclerView.adapter as? MenuAdapter)?.notifyDataSetChanged()
            }
        }
    }

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

    private fun updateCheckoutButton() {
        var totalItem = 0
        var totalPrice = 0.0

        for (item in cartList) {
            totalItem += item.quantity
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

    override fun onQuantityChange(menuId: Int, quantity: Int) {
        val existingItem = cartList.find { it.menuId == menuId && it.levelId == null }

        if (quantity > 0) {
            if (existingItem != null) {
                existingItem.quantity = quantity
            } else {
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
            if (existingItem != null) cartList.remove(existingItem)
        }
        updateCheckoutButton()
    }

    // --- FIX BUG 1: DOUBLE ITEM ---
    override fun onVariantChange(menuId: Int, qty: Int, levelId: Int?, extraCost: Int, note: String?) {
        val menuDetail = listMenu.find { it.id == menuId } ?: return

        // PENTING: Ubah null jadi "" biar perbandingannya akurat
        val noteBaru = note ?: ""

        // Cek apakah item dengan spek SAMA PERSIS sudah ada?
        val existingItem = cartList.find {
            it.menuId == menuId &&
                    it.levelId == levelId &&
                    (it.notes ?: "") == noteBaru // Bandingkan string vs string (aman dari null)
        }

        if (existingItem != null) {
            // Update qty item yang sudah ada
            existingItem.quantity = qty

            // Kalau qty jadi 0, hapus
            if (qty == 0) cartList.remove(existingItem)

        } else {
            // Item belum ada, buat baru
            if (qty > 0) {
                cartList.add(CartItem(
                    menuId = menuId,
                    menuName = menuDetail.name,
                    price = menuDetail.price.toDouble(),
                    quantity = qty,
                    notes = noteBaru,
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

        // PENTING: Gunakan launcher agar bisa terima data balik
        checkoutLauncher.launch(intent)
    }
}