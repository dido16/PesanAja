package com.example.pesanaja

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.pesanaja.entities.CartItem
import com.example.pesanaja.entities.OrderResponse
import com.example.pesanaja.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReceiptActivity : AppCompatActivity() {

    private lateinit var btnAction: Button
    private lateinit var tvStatus: TextView

    // Data Order
    private var orderData: OrderResponse? = null
    private var currentStatus: String = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        // 1. Ambil Data dari Intent
        orderData = intent.getSerializableExtra("order_response") as? OrderResponse
        val cartList = intent.getSerializableExtra("cart_list") as? ArrayList<CartItem> ?: arrayListOf()
        val meja = intent.getStringExtra("meja") ?: "0"

        val order = orderData?.orderData
        currentStatus = order?.status ?: "pending" // Ambil status awal

        // 2. Setup Tampilan Struk (Sama kayak sebelumnya)
        findViewById<TextView>(R.id.tvReceiptInfo).text = """
            Order ID: #${order?.id ?: "---"}
            Pelanggan: ${order?.customerName ?: "---"}
            Meja: $meja
        """.trimIndent()

        findViewById<TextView>(R.id.tvTimestamp).text = order?.createdAt ?: "---"

        val container = findViewById<LinearLayout>(R.id.containerItems)
        cartList.forEach { item ->
            val tvItem = TextView(this)
            val infoLevel = if (item.extraCost > 0) " (+Level)" else ""
            val totalItem = (item.price + item.extraCost) * item.quantity
            tvItem.text = "${item.menuName}$infoLevel x${item.quantity} - Rp $totalItem"
            tvItem.textSize = 14f
            tvItem.setPadding(0, 4, 0, 4)
            container.addView(tvItem)
        }

        findViewById<TextView>(R.id.tvReceiptSubtotal).text = "Rp ${order?.subtotal?.toInt() ?: 0}"
        findViewById<TextView>(R.id.tvReceiptPajak).text = "Rp ${order?.taxAmount?.toInt() ?: 0}"
        findViewById<TextView>(R.id.tvReceiptGrandTotal).text = "Rp ${order?.finalTotal?.toInt() ?: 0}"

        // 3. LOGIC TOMBOL DINAMIS (INI SOLUSI PLOT HOLE-NYA)
        btnAction = findViewById(R.id.btnPayNow) // Pastikan ID di XML adalah btnPayNow atau sesuaikan

        updateButtonState()
    }

    private fun updateButtonState() {
        if (currentStatus == "completed") {
            // Kalau sudah Lunas
            btnAction.text = "Selesai & Kembali ke Menu"
            btnAction.backgroundTintList = getColorStateList(android.R.color.darker_gray)
            btnAction.setOnClickListener {
                val i = Intent(this, MainActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
                finish()
            }
        } else {
            // Kalau Masih Pending (Plot Hole Case)
            btnAction.text = "Bayar Sekarang (Pending)"
            btnAction.setOnClickListener {
                // Munculin lagi Pop-up Bayar
                if (orderData != null) {
                    showPaymentDialog(orderData!!)
                } else {
                    Toast.makeText(this, "Data order hilang, tidak bisa bayar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- COPY PASTE FUNGSI PAYMENT DARI CHECKOUT ACTIVITY ---
    private fun showPaymentDialog(dataOrder: OrderResponse) {
        val dialogBuilder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.payment, null) // Pakai layout payment.xml yang sama

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
                btnBayar.text = "Memproses..."
                btnBayar.isEnabled = false
                verifikasiPembayaran(dataOrder.orderData?.id ?: 0, dialog)
            } else {
                etPin.error = "PIN Salah!"
                Toast.makeText(this, "PIN Salah!", Toast.LENGTH_SHORT).show()
            }
        }

        btnBatal.setOnClickListener {
            dialog.dismiss()
            // Kalau batal di sini, gak ngapa-ngapain, tetep di halaman receipt status pending
        }

        dialog.show()
    }

    private fun verifikasiPembayaran(orderId: Int, dialog: AlertDialog) {
        ApiClient.instance.payOrder(orderId).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                dialog.dismiss()
                if (response.isSuccessful) {
                    Toast.makeText(this@ReceiptActivity, "Pembayaran Lunas! Meja Kosong.", Toast.LENGTH_LONG).show()

                    // UPDATE STATUS JADI COMPLETED
                    currentStatus = "completed"
                    updateButtonState() // Ubah tombol jadi "Kembali ke Menu"

                } else {
                    Toast.makeText(this@ReceiptActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(this@ReceiptActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}