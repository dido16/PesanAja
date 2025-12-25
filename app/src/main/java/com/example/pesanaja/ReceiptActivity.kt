package com.example.pesanaja

import android.content.Intent
import android.graphics.Color
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
    private lateinit var tvStatus: TextView // Tambahin ini biar bisa diakses global

    // Data Order
    private var orderData: OrderResponse? = null
    private var currentStatus: String = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        // 1. Inisialisasi View
        btnAction = findViewById(R.id.btnPayNow)
        tvStatus = findViewById(R.id.tvStatusOrder) // Inisialisasi TextView Status

        // 2. Ambil Data dari Intent
        orderData = intent.getSerializableExtra("order_response") as? OrderResponse
        val cartList = intent.getSerializableExtra("cart_list") as? ArrayList<CartItem> ?: arrayListOf()
        val meja = intent.getStringExtra("meja") ?: "0"

        val order = orderData?.orderData
        currentStatus = order?.status ?: "pending"

        // 3. Setup Teks Statis
        findViewById<TextView>(R.id.tvReceiptInfo).text = """
            Order ID: #${order?.id ?: "---"}
            Pelanggan: ${order?.customerName ?: "---"}
            Meja: $meja
        """.trimIndent()

        findViewById<TextView>(R.id.tvTimestamp).text = order?.createdAt ?: "---"

        // 4. Render List Item
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

        // 5. PANGGIL FUNGSI UPDATE TAMPILAN
        updateTampilanStatus()
    }

    // --- FUNGSI BARU: UPDATE SEMUA (TEKS + WARNA + TOMBOL) ---
    private fun updateTampilanStatus() {
        if (currentStatus == "completed" || currentStatus == "paid") {
            // A. UPDATE STATUS JADI HIJAU
            tvStatus.text = "LUNAS / COMPLETED"
            tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Hijau

            // B. UPDATE TOMBOL JADI KEMBALI
            btnAction.text = "Selesai & Kembali ke Menu"
            btnAction.backgroundTintList = getColorStateList(android.R.color.darker_gray)
            btnAction.setOnClickListener {
                val i = Intent(this, MainActivity::class.java) // Atau MenuActivity
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
                finish()
            }
        } else {
            // A. UPDATE STATUS JADI MERAH
            tvStatus.text = "PENDING / BELUM BAYAR"
            tvStatus.setTextColor(Color.parseColor("#F44336")) // Merah

            // B. UPDATE TOMBOL JADI BAYAR
            btnAction.text = "Bayar Sekarang"
            btnAction.setOnClickListener {
                if (orderData != null) {
                    showPaymentDialog(orderData!!)
                } else {
                    Toast.makeText(this, "Data order hilang", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPaymentDialog(dataOrder: OrderResponse) {
        val dialogBuilder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.payment, null)

        val tvTotal = view.findViewById<TextView>(R.id.tvTotalBayarDialog)
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
            if (pin == "123456") {
                btnBayar.text = "Memproses..."
                btnBayar.isEnabled = false
                verifikasiPembayaran(dataOrder.orderData?.id ?: 0, dialog)
            } else {
                etPin.error = "PIN Salah!"
            }
        }

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun verifikasiPembayaran(orderId: Int, dialog: AlertDialog) {
        ApiClient.instance.payOrder(orderId).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                dialog.dismiss()
                if (response.isSuccessful) {
                    Toast.makeText(this@ReceiptActivity, "Pembayaran Berhasil!", Toast.LENGTH_LONG).show()

                    // 1. UBAH STATUS DI VARIABEL
                    currentStatus = "completed"

                    // 2. REFRESH TAMPILAN (Biar teks Pending jadi Lunas seketika)
                    updateTampilanStatus()

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