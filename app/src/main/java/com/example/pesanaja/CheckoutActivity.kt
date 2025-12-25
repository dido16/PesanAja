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
import com.example.pesanaja.repository.ApiService
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

    // --- POP-UP KONFIRMASI (ESTETIK) ---
    private fun showConfirmationDialog(nama: String) {
        // 1. Inflate Layout Custom
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_order, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        // 2. Inisialisasi View di dalam Dialog
        val tvNama = dialogView.findViewById<TextView>(R.id.tvConfirmNama)
        val tvMeja = dialogView.findViewById<TextView>(R.id.tvConfirmMeja)
        val containerItems = dialogView.findViewById<LinearLayout>(R.id.containerConfirmItems)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvConfirmTotal)
        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatalConfirm)
        val btnKirimConfirm = dialogView.findViewById<Button>(R.id.btnKirimConfirm)

        // 3. Set Data
        tvNama.text = "Atas Nama: $nama"
        tvMeja.text = "Meja: $nomorMeja"
        // Ambil teks total bayar dari Activity utama (bersihkan labelnya)
        tvTotal.text = tvGrandTotal.text.toString().replace("Total Bayar: ", "")

        // 4. Loop Items & Masukkan ke Container (Dynamic View)
        containerItems.removeAllViews()
        checkoutList.forEach { item ->
            // Bikin Layout Baris Item secara Programmatic
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, 8, 0, 8)

            val tvItemName = TextView(this)
            val levelInfo = if (item.extraCost > 0) "\n+Level" else ""
            tvItemName.text = "${item.quantity}x ${item.menuName}$levelInfo"
            tvItemName.setTextColor(Color.parseColor("#424242"))
            tvItemName.textSize = 14f
            tvItemName.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val tvItemPrice = TextView(this)
            val totalItem = (item.price + item.extraCost) * item.quantity
            tvItemPrice.text = "Rp ${totalItem.toInt()}"
            tvItemPrice.setTextColor(Color.parseColor("#757575"))
            tvItemPrice.textSize = 14f

            row.addView(tvItemName)
            row.addView(tvItemPrice)
            containerItems.addView(row)
        }

        // 5. Tampilkan Dialog
        val dialog = builder.create()
        // PENTING: Bikin background dialog transparan biar rounded corner CardView kelihatan
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        // 6. Aksi Tombol
        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

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
                    Toast.makeText(this@CheckoutActivity, "Gagal Order: ${response.code()}", Toast.LENGTH_SHORT).show()
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

                // Pastikan kita ambil data TERBARU dari response bayar
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@CheckoutActivity, "Pembayaran Lunas! Struk Dicetak.", Toast.LENGTH_LONG).show()

                    // AMBIL DATA TERBARU (STATUS: COMPLETED)
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