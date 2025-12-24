package com.example.pesanaja

import android.content.Context
import android.content.Intent
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
        for (item in checkoutList) {
            subtotal += (item.price + item.extraCost) * item.quantity
        }
        val pajak = subtotal * 0.10
        val totalAkhir = subtotal + pajak

        tvSubtotal.text = "Subtotal: Rp ${subtotal.toInt()}"
        tvPajak.text = "PPN (10%): Rp ${pajak.toInt()}"
        tvGrandTotal.text = "Total Bayar: Rp ${totalAkhir.toInt()}"
    }

    private fun showConfirmationDialog(nama: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi Pesanan")

        var summary = "Nama: $nama\nMeja: $nomorMeja\n\n"
        checkoutList.forEach {
            val levelInfo = if (it.extraCost > 0) "(+Level)" else ""
            summary += "- ${it.menuName} $levelInfo (${it.quantity}x)\n"
        }
        summary += "\n${tvGrandTotal.text}"

        builder.setMessage(summary)
        builder.setPositiveButton("Gas, Pesan!") { _, _ -> prosesKirimAPI(nama) }
        builder.setNegativeButton("Cek Lagi", null)
        builder.show()
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
            OrderItemRequest(it.menuId, it.quantity, it.levelId, it.notes)
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
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        try {
                            val errorResponse = com.google.gson.Gson().fromJson(errorBody, OrderResponse::class.java)
                            Toast.makeText(this@CheckoutActivity, errorResponse.message, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@CheckoutActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@CheckoutActivity, "Terjadi Kesalahan (Code: ${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                btnKirim.text = "Konfirmasi Pesanan"
                btnKirim.isEnabled = true
                Toast.makeText(this@CheckoutActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_SHORT).show()
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

        val totalHarga = dataOrder.orderData?.finalTotal?.toInt() ?: 0
        tvTotal.text = "Rp $totalHarga"

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
                val selectedId = rgMethod.checkedRadioButtonId
                val method = view.findViewById<RadioButton>(selectedId)?.text.toString()
                Toast.makeText(this, "Metode: $method terpilih", Toast.LENGTH_SHORT).show()

                btnBayar.text = "Memproses..."
                btnBayar.isEnabled = false
                verifikasiPembayaran(dataOrder.orderData?.id ?: 0, dataOrder, dialog)
            } else {
                etPin.error = "PIN Salah! Coba 123456"
                Toast.makeText(this, "PIN Salah!", Toast.LENGTH_SHORT).show()
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

                // --- BAGIAN INI KUNCINYA ---
                // Pastikan kita ambil data TERBARU dari response bayar (yang statusnya sudah completed)
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@CheckoutActivity, "Pembayaran Lunas! Struk Dicetak.", Toast.LENGTH_LONG).show()

                    // AMBIL DATA TERBARU (STATUS: COMPLETED)
                    val dataTerbaru = response.body()!!

                    // Kirim data TERBARU ini ke ReceiptActivity
                    pindahKeReceipt(dataTerbaru)

                } else {
                    Toast.makeText(this@CheckoutActivity, "Gagal Verifikasi: ${response.code()}", Toast.LENGTH_SHORT).show()
                    // Kalau gagal bayar, baru kita kirim data lama (masih pending) biar user tau
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