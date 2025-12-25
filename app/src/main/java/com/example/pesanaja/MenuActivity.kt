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

    // Simpan Jumlah: MenuID -> Qty
    private val cartMap = mutableMapOf<Int, Int>()

    // Simpan Level yang dipilih: MenuID -> Pair(LevelID, ExtraCost)
    private val selectedLevels = mutableMapOf<Int, Pair<Int, Int>>()

    private var listMenu: List<MenuModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // 1. Inisialisasi View (Sesuai ID di XML baru)
        recyclerView = findViewById(R.id.recyclerMenu)
        btnCheckout = findViewById(R.id.btnCheckout)
        tvMeja = findViewById(R.id.tvMeja)

        nomorMeja = intent.getStringExtra("meja") ?: "0"
        tvMeja.text = "Meja: $nomorMeja"

        recyclerView.layoutManager = LinearLayoutManager(this)

        // 2. Logic Tombol Checkout
        btnCheckout.setOnClickListener {
            if (cartMap.isEmpty()) {
                Toast.makeText(this, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show()
            } else {
                prosesKeCheckout()
            }
        }

        // 3. Ambil Data
        loadMenu()

        // Update tombol pertama kali (biar tulisannya Rp 0)
        updateCheckoutButton()
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

    // --- LOGIC HITUNG TOTAL HARGA (BIAR KEREN) ---
    private fun updateCheckoutButton() {
        var totalItem = 0
        var totalPrice = 0.0

        // Loop semua item yang dipilih
        cartMap.forEach { (menuId, qty) ->
            val menu = listMenu.find { it.id == menuId }
            if (menu != null) {
                totalItem += qty

                // Cek harga tambahan dari level
                val extraCost = selectedLevels[menuId]?.second ?: 0

                // Rumus: (Harga Dasar + Harga Level) * Jumlah
                totalPrice += (menu.price + extraCost) * qty
            }
        }

        // Format Rupiah
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
            btnCheckout.alpha = 0.7f // Agak transparan kalau kosong
        }
    }

    // Callback dari Adapter saat Qty berubah
    override fun onQuantityChange(menuId: Int, quantity: Int) {
        if (quantity > 0) {
            cartMap[menuId] = quantity
        } else {
            cartMap.remove(menuId)
            // Kalau qty 0, hapus juga data levelnya biar bersih
            selectedLevels.remove(menuId)
        }
        updateCheckoutButton() // Hitung ulang harga
    }

    // Callback dari Adapter saat Level berubah (via BottomSheet)
    override fun onLevelChange(menuId: Int, levelId: Int, extraCost: Int) {
        // Simpan pilihan user ke map
        selectedLevels[menuId] = Pair(levelId, extraCost)
        updateCheckoutButton() // Hitung ulang harga (karena ada extra cost)
    }

    private fun prosesKeCheckout() {
        val selectedItems = ArrayList<CartItem>()

        cartMap.forEach { (id, qty) ->
            val menuDetail = listMenu.find { it.id == id }

            if (menuDetail != null) {
                var finalLevelId: Int? = null
                var finalExtraCost = 0
                var perluLevel = false

                // Cek Level
                if (menuDetail.perluLevel == "YA") {
                    perluLevel = true
                    // Default ke Level 1 (Netral) kalau user lupa pilih
                    val levelInfo = selectedLevels[id] ?: Pair(1, 0)
                    finalLevelId = levelInfo.first
                    finalExtraCost = levelInfo.second
                }

                selectedItems.add(CartItem(
                    menuId = id,
                    menuName = menuDetail.name, // Penting buat dikirim ke History nanti
                    price = menuDetail.price,
                    quantity = qty,
                    notes = "",
                    perluLevel = perluLevel,
                    levelId = finalLevelId,
                    extraCost = finalExtraCost,

                    // Isi menuData buat CartItem (biar gak null di keranjang/checkout)
                    // Kita bisa abaikan image-nya dulu atau isi kalau mau
                    menuData = null
                ))
            }
        }

        val intent = Intent(this, CheckoutActivity::class.java)
        intent.putExtra("meja", nomorMeja)
        intent.putExtra("cart_list", selectedItems)
        startActivity(intent)
    }
}