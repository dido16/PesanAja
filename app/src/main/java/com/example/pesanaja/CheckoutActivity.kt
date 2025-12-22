package com.example.pesanaja

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.adapter.CheckoutAdapter
import com.example.pesanaja.entities.CartItem // Gunakan CartItem agar ada Harga & Nama
import com.example.pesanaja.entities.OrderItemRequest
import com.example.pesanaja.entities.OrderRequest
import com.example.pesanaja.entities.OrderResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CheckoutActivity : AppCompatActivity() {

    private lateinit var etNama: EditText
    private lateinit var tvSubtotal: TextView
    private lateinit var tvPajak: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var rvItems: RecyclerView
    private lateinit var btnKirim: Button

    private var nomorMeja: String = ""
    private var checkoutList = mutableListOf<CartItem>() // List detail untuk UI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // 1. Inisialisasi View
        etNama = findViewById(R.id.etNamaPelanggan)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvPajak = findViewById(R.id.tvPajak)
        tvGrandTotal = findViewById(R.id.tvTotalBayar)
        rvItems = findViewById(R.id.rvCheckoutItems)
        btnKirim = findViewById(R.id.btnKonfirmasiPesan)

        // 2. Ambil data nomor meja
        nomorMeja = intent.getStringExtra("meja") ?: "0"
        findViewById<TextView>(R.id.tvDetailMeja).text = "Meja: $nomorMeja"

        // 3. Ambil data list belanja (Pastikan dikirim sebagai CartItem dari MenuActivity)
        val dataIntent = intent.getSerializableExtra("cart_list") as? ArrayList<CartItem>
        if (dataIntent != null) {
            checkoutList.addAll(dataIntent)
        }

        // 4. Jalankan UI & Perhitungan
        setupRecyclerView()
        hitungTagihan()

        btnKirim.setOnClickListener {
            val nama = etNama.text.toString().trim()
            if (nama.isEmpty()) {
                etNama.error = "Nama pelanggan wajib diisi!"
            } else {
                prosesKirimAPI(nama)
            }
        }
    }

    private fun setupRecyclerView() {
        rvItems.layoutManager = LinearLayoutManager(this)
        // Adapter sekarang memakai checkoutList agar bisa akses Nama & Harga
        rvItems.adapter = CheckoutAdapter(checkoutList)
    }

    private fun hitungTagihan() {
        var subtotal = 0.0
        // Hitung total harga dari setiap item
        for (item in checkoutList) {
            subtotal += (item.price * item.quantity)
        }

        val pajak = subtotal * 0.10 // PPN 10%
        val totalAkhir = subtotal + pajak

        tvSubtotal.text = "Subtotal: Rp ${subtotal.toInt()}"
        tvPajak.text = "PPN (10%): Rp ${pajak.toInt()}"
        tvGrandTotal.text = "Total Bayar: Rp ${totalAkhir.toInt()}"
    }

    private fun prosesKirimAPI(nama: String) {
        // Konversi CartItem (UI) kembali ke OrderItemRequest (API Laravel)
        val itemsToOrder = checkoutList.map {
            OrderItemRequest(
                menuId = it.menuId,
                quantity = it.quantity,
                notes = it.notes // Catatan "gulanya dikit" ikut terkirim
            )
        }

        val request = OrderRequest(
            meja = nomorMeja,
            customerName = nama,
            items = itemsToOrder
        )

        ApiClient.instance.createOrder(request).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@CheckoutActivity, "Sukses! Pesanan dikirim.", Toast.LENGTH_LONG).show()

                    // Kembali ke halaman awal setelah sukses
                    val intent = Intent(this@CheckoutActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } else {
                    val errorMsg = response.body()?.message ?: "Gagal mengirim pesanan"
                    Toast.makeText(this@CheckoutActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                Toast.makeText(this@CheckoutActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}