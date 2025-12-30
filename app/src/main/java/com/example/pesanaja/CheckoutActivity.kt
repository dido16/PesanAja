package com.example.pesanaja

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.adapter.CheckoutAdapter
import com.example.pesanaja.entities.*
import com.example.pesanaja.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class CheckoutActivity : AppCompatActivity() {

    private lateinit var etNama: EditText
    private lateinit var tvSubtotal: TextView
    private lateinit var tvPajak: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var rvItems: RecyclerView
    private lateinit var btnKirim: Button

    private var nomorMeja: String = ""
    private var checkoutList = mutableListOf<CartItem>()

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

        // 2. Ambil data dari Intent
        nomorMeja = intent.getStringExtra("meja") ?: "0"
        findViewById<TextView>(R.id.tvDetailMeja).text = "Meja: $nomorMeja"

        val dataIntent = intent.getSerializableExtra("cart_list") as? ArrayList<CartItem>
        if (dataIntent != null) checkoutList.addAll(dataIntent)

        // 3. Setup RecyclerView & Hitung Awal
        setupRecyclerView()
        hitungTagihan()

        // 4. Tombol Kirim
        btnKirim.setOnClickListener {
            val nama = etNama.text.toString().trim()
            if (nama.isEmpty()) {
                etNama.error = "Nama pelanggan wajib diisi!"
            } else if (checkoutList.isEmpty()) {
                Toast.makeText(this, "Keranjang kosong!", Toast.LENGTH_SHORT).show()
            } else {
                showConfirmationDialog(nama)
            }
        }
    }

    private fun setupRecyclerView() {
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = CheckoutAdapter(checkoutList) { hitungTagihan() }
    }

    private fun hitungTagihan() {
        var subtotal = 0.0

        // Loop item dan hitung (Aman karena tipe data Double)
        for (item in checkoutList) {
            subtotal += (item.price + item.extraCost) * item.quantity
        }

        val pajak = subtotal * 0.10
        val totalAkhir = subtotal + pajak

        // Format Rupiah
        tvSubtotal.text = formatRupiah(subtotal)
        tvPajak.text = formatRupiah(pajak)
        tvGrandTotal.text = formatRupiah(totalAkhir)
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        return numberFormat.format(number).replace("Rp", "Rp ")
    }

    // --- POP-UP KONFIRMASI ---
    private fun showConfirmationDialog(nama: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_order, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val tvNama = dialogView.findViewById<TextView>(R.id.tvConfirmNama)
        val tvMeja = dialogView.findViewById<TextView>(R.id.tvConfirmMeja)
        val containerItems = dialogView.findViewById<LinearLayout>(R.id.containerConfirmItems)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvConfirmTotal)
        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatalConfirm)
        val btnKirimConfirm = dialogView.findViewById<Button>(R.id.btnKirimConfirm)

        // Set Data
        tvNama.text = "Atas Nama: $nama"
        tvMeja.text = "Meja: $nomorMeja"
        tvTotal.text = tvGrandTotal.text

        // Loop Items (Dynamic View)
        containerItems.removeAllViews()
        checkoutList.forEach { item ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, 8, 0, 8)

            val tvItemName = TextView(this)
            // Info Level & Catatan
            val levelInfo = if (item.extraCost > 0.0) "\n+Level" else ""
            val noteInfo = if (!item.notes.isNullOrEmpty()) "\nCatatan: ${item.notes}" else ""

            tvItemName.text = "${item.quantity}x ${item.menuName}$levelInfo$noteInfo"
            tvItemName.setTextColor(Color.parseColor("#424242"))
            tvItemName.textSize = 14f
            tvItemName.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val tvItemPrice = TextView(this)
            val totalItem = (item.price + item.extraCost) * item.quantity
            tvItemPrice.text = formatRupiah(totalItem)
            tvItemPrice.setTextColor(Color.parseColor("#757575"))
            tvItemPrice.textSize = 14f

            row.addView(tvItemName)
            row.addView(tvItemPrice)
            containerItems.addView(row)
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        btnBatal.setOnClickListener { dialog.dismiss() }
        btnKirimConfirm.setOnClickListener {
            dialog.dismiss()
            prosesKirimAPI(nama)
        }
    }

    private fun generateDeviceUUID(): String {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_uuid", null)

        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_uuid", id).apply()
        }
        return id ?: "unknown_device"
    }

    private fun prosesKirimAPI(nama: String) {
        val itemsToOrder = checkoutList.map {
            OrderItemRequest(
                menuId = it.menuId,
                quantity = it.quantity,
                levelId = it.levelId,
                notes = it.notes
            )
        }

        val myDeviceId = generateDeviceUUID()

        val request = OrderRequest(
            meja = nomorMeja,
            customerName = nama,
            items = itemsToOrder,
            deviceId = myDeviceId
        )

        btnKirim.text = "Mengirim..."
        btnKirim.isEnabled = false

        ApiClient.instance.createOrder(request).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                btnKirim.text = "Konfirmasi Pesanan"
                btnKirim.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    val orderResponse = response.body()
                    if (orderResponse != null) {
                        showPaymentDialog(orderResponse)
                    }
                } else {
                    // --- PERBAIKAN: Menangani Error 400 dengan Pesan Jelas ---
                    val errorBody = response.errorBody()?.string()
                    var errorMessage = "Gagal memproses pesanan."

                    if (errorBody != null) {
                        try {
                            // Coba parsing pesan error dari JSON Laravel
                            val errorJson = org.json.JSONObject(errorBody)
                            errorMessage = errorJson.getString("message")
                        } catch (e: Exception) {
                            errorMessage = "Terjadi kesalahan server (${response.code()})"
                        }
                    }

                    // Munculkan Alert Dialog biar user sadar
                    AlertDialog.Builder(this@CheckoutActivity)
                        .setTitle("Gagal Order")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                btnKirim.text = "Konfirmasi Pesanan"
                btnKirim.isEnabled = true

                val msg = if (t is java.net.ConnectException) "Cek koneksi internet Anda." else t.message
                Toast.makeText(this@CheckoutActivity, "Error: $msg", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPaymentDialog(dataOrder: OrderResponse) {
        val dialogBuilder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.payment, null)

        val tvTotal = view.findViewById<TextView>(R.id.tvTotalBayarDialog)
        val rgMethod = view.findViewById<RadioGroup>(R.id.rgPaymentMethod)
        val etPin = view.findViewById<EditText>(R.id.etPinPayment)
        val btnBayar = view.findViewById<Button>(R.id.btnProsesBayar)
        val btnBatal = view.findViewById<TextView>(R.id.btnBatalBayar)

        // Konversi Double ke String Rupiah
        val totalHarga = dataOrder.data?.finalTotal?.toDouble() ?: 0.0
        tvTotal.text = formatRupiah(totalHarga)

        dialogBuilder.setView(view)
        val dialog = dialogBuilder.create()
        dialog.setCancelable(false)

        btnBayar.setOnClickListener {
            val pin = etPin.text.toString()
            if (pin.isEmpty()) {
                etPin.error = "Masukkan PIN dulu!"
                return@setOnClickListener
            }

            if (pin == "123456") {
                btnBayar.text = "Memproses..."
                btnBayar.isEnabled = false
                verifikasiPembayaran(dataOrder.data?.id ?: 0, dataOrder, dialog)
            } else {
                etPin.error = "PIN Salah! Coba 123456"
            }
        }

        btnBatal.setOnClickListener {
            dialog.dismiss()
            pindahKeReceipt(dataOrder)
        }

        dialog.show()
    }

    private fun verifikasiPembayaran(orderId: Int, originalResponse: OrderResponse, dialog: AlertDialog) {
        ApiClient.instance.payOrder(orderId).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                dialog.dismiss()
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@CheckoutActivity, "Pembayaran Lunas!", Toast.LENGTH_LONG).show()
                    val dataTerbaru = response.body()!!
                    pindahKeReceipt(dataTerbaru)
                } else {
                    Toast.makeText(this@CheckoutActivity, "Gagal Verifikasi: ${response.code()}", Toast.LENGTH_SHORT).show()
                    pindahKeReceipt(originalResponse)
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(this@CheckoutActivity, "Koneksi Error saat Bayar", Toast.LENGTH_SHORT).show()
                pindahKeReceipt(originalResponse)
            }
        })
    }

    private fun pindahKeReceipt(dataOrder: OrderResponse) {
        val intent = Intent(this@CheckoutActivity, ReceiptActivity::class.java)
        intent.putExtra("order_response", dataOrder)
        intent.putExtra("cart_list", ArrayList(checkoutList))
        intent.putExtra("meja", nomorMeja)
        startActivity(intent)
        finish()
    }
}