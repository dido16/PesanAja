package com.example.pesanaja

import android.content.Intent // Tambahkan import Intent
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MenuActivity : AppCompatActivity(), MenuAdapter.OnCartChangeListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnPesan: Button
    private lateinit var tvMeja: TextView
    private var nomorMeja: String = ""
    private val cartMap = mutableMapOf<Int, Int>()
    private var listMenu: List<MenuModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Inisialisasi View
        recyclerView = findViewById(R.id.recyclerMenu)
        btnPesan = findViewById(R.id.btnCheckout)
        tvMeja = findViewById(R.id.tvMeja)

        nomorMeja = intent.getStringExtra("meja") ?: "0"
        tvMeja.text = "Meja: $nomorMeja"

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Alur Baru: Pindah ke halaman Checkout
        btnPesan.setOnClickListener {
            if (cartMap.isEmpty()) {
                Toast.makeText(this, "Pilih menu dulu!", Toast.LENGTH_SHORT).show()
            } else {
                // Ambil detail menu (Nama & Harga) dari list asli berdasarkan ID di cartMap
                val selectedItems = ArrayList<CartItem>()
                cartMap.forEach { (id, qty) ->
                    val menuDetail = listMenu.find { it.id == id } // listMenu adalah data dari API
                    if (menuDetail != null) {
                        selectedItems.add(CartItem(id, menuDetail.name, menuDetail.price, qty))
                    }
                }

                val intent = Intent(this, CheckoutActivity::class.java)
                intent.putExtra("meja", nomorMeja)
                intent.putExtra("cart_list", selectedItems) // Kirim list objek CartItem
                startActivity(intent)
            }
        }

        loadMenu()
    }

    private fun loadMenu() {
        ApiClient.instance.getMenus().enqueue(object : Callback<List<MenuModel>> {
            override fun onResponse(call: Call<List<MenuModel>>, response: Response<List<MenuModel>>) {
                if (response.isSuccessful) {
                    listMenu = response.body() ?: emptyList()
                    displayMenu(listMenu)
                } else {
                    Toast.makeText(this@MenuActivity, "Gagal ambil menu: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<MenuModel>>, t: Throwable) {
                Log.e("API_ERROR", "Error: ${t.message}")
                Toast.makeText(this@MenuActivity, "Koneksi Bermasalah!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayMenu(list: List<MenuModel>) {
        val adapter = MenuAdapter(list, this)
        recyclerView.adapter = adapter
    }

    override fun onQuantityChange(menuId: Int, quantity: Int) {
        if (quantity > 0) cartMap[menuId] = quantity else cartMap.remove(menuId)
        btnPesan.text = "Pesan (${cartMap.values.sum()} item)"
    }

    // Fungsi prosesCheckout() dihapus dari sini karena dipindah ke CheckoutActivity
}