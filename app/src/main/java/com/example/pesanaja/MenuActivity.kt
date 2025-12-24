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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MenuActivity : AppCompatActivity(), MenuAdapter.OnCartChangeListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnPesan: Button
    private lateinit var tvMeja: TextView
    private var nomorMeja: String = ""

    // Simpan Jumlah: MenuID -> Qty
    private val cartMap = mutableMapOf<Int, Int>()

    // BARU: Simpan Level yang dipilih: MenuID -> Pair(LevelID, ExtraCost)
    private val selectedLevels = mutableMapOf<Int, Pair<Int, Int>>()

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

        // Alur Baru: Pindah ke halaman Checkout dengan membawa data Level
        btnPesan.setOnClickListener {
            if (cartMap.isEmpty()) {
                Toast.makeText(this, "Pilih menu dulu!", Toast.LENGTH_SHORT).show()
            } else {
                val selectedItems = ArrayList<CartItem>()

                cartMap.forEach { (id, qty) ->
                    val menuDetail = listMenu.find { it.id == id } // Ambil data menu asli

                    if (menuDetail != null) {
                        // LOGIKA BARU: Tentukan Level dan Harga
                        var finalLevelId: Int? = null
                        var finalExtraCost = 0
                        var perluLevel = false

                        // Cek apakah menu butuh level (Sesuai database "YA")
                        if (menuDetail.perluLevel == "YA") {
                            perluLevel = true

                            // Ambil level yang dipilih user dari map
                            // Kalau user gak utak-atik spinner, Default ke Level ID 1 (Level 0/Netral)
                            val levelInfo = selectedLevels[id] ?: Pair(1, 0)

                            finalLevelId = levelInfo.first
                            finalExtraCost = levelInfo.second
                        }

                        // Masukkan ke CartItem
                        selectedItems.add(CartItem(
                            menuId = id,
                            menuName = menuDetail.name,
                            price = menuDetail.price,
                            quantity = qty,
                            notes = "", // Catatan nanti diisi di Checkout
                            perluLevel = perluLevel,
                            levelId = finalLevelId,      // ID Level (misal 14 buat Immortality)
                            extraCost = finalExtraCost   // Harga Tambahan (misal 500)
                        ))
                    }
                }

                // Kirim semua data ke CheckoutActivity
                val intent = Intent(this, CheckoutActivity::class.java)
                intent.putExtra("meja", nomorMeja)
                intent.putExtra("cart_list", selectedItems)
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

    // Callback saat tombol Plus/Minus ditekan
    override fun onQuantityChange(menuId: Int, quantity: Int) {
        if (quantity > 0) cartMap[menuId] = quantity else cartMap.remove(menuId)
        btnPesan.text = "Pesan (${cartMap.values.sum()} item)"
    }

    // Callback saat Spinner Level dipilih (BARU)
    override fun onLevelChange(menuId: Int, levelId: Int, extraCost: Int) {
        // Simpan pilihan user ke map: ID Menu -> (ID Level, Harga Tambahan)
        selectedLevels[menuId] = Pair(levelId, extraCost)
    }
}