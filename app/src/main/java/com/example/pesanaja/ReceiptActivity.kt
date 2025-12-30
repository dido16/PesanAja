package com.example.pesanaja

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.pesanaja.entities.CartItem
import com.example.pesanaja.entities.OrderResponse
import com.example.pesanaja.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class ReceiptActivity : AppCompatActivity() {

    private lateinit var btnAction: Button
    private lateinit var tvStatus: TextView

    // Data Order
    private var orderResponse: OrderResponse? = null
    private var currentStatus: String = "pending"

    // PERBAIKAN 1: Bikin variabel global buat simpan nomor meja
    private var nomorMeja: String = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        // 1. Inisialisasi View
        btnAction = findViewById(R.id.btnPayNow)
        tvStatus = findViewById(R.id.tvStatusOrder)

        // 2. Ambil Data dari Intent
        orderResponse = intent.getSerializableExtra("order_response") as? OrderResponse
        val cartList = intent.getSerializableExtra("cart_list") as? ArrayList<CartItem> ?: arrayListOf()

        // PERBAIKAN 2: Simpan ke variabel global
        nomorMeja = intent.getStringExtra("meja") ?: "0"

        val orderData = orderResponse?.data
        currentStatus = orderData?.status ?: "pending"

        // 3. Setup Teks Statis
        findViewById<TextView>(R.id.tvReceiptInfo).text = """
            Order ID: #${orderData?.id ?: "---"}
            Pelanggan: ${orderData?.customerName ?: "---"}
            Meja: $nomorMeja
        """.trimIndent()

        findViewById<TextView>(R.id.tvTimestamp).text = orderData?.createdAt ?: "---"

        // 4. Render List Item
        val container = findViewById<LinearLayout>(R.id.containerItems)
        container.removeAllViews()

        cartList.forEach { item ->
            val tvItem = TextView(this)
            val infoLevel = if (item.extraCost > 0.0) " (+Level)" else ""
            val totalItem = (item.price + item.extraCost) * item.quantity

            tvItem.text = "${item.menuName}$infoLevel x${item.quantity} - ${formatRupiah(totalItem)}"
            tvItem.textSize = 14f
            tvItem.setTextColor(Color.parseColor("#424242"))
            tvItem.setPadding(0, 4, 0, 4)
            container.addView(tvItem)
        }

        // 5. Tampilkan Total Harga
        findViewById<TextView>(R.id.tvReceiptSubtotal).text = formatRupiah(orderData?.subtotal ?: 0.0)
        findViewById<TextView>(R.id.tvReceiptPajak).text = formatRupiah(orderData?.taxAmount ?: 0.0)
        findViewById<TextView>(R.id.tvReceiptGrandTotal).text = formatRupiah(orderData?.finalTotal ?: 0.0)

        updateTampilanStatus()
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        return numberFormat.format(number).replace("Rp", "Rp ")
    }

    private fun updateTampilanStatus() {
        when (currentStatus) {
            "pending" -> {
                tvStatus.text = "BELUM BAYAR"
                tvStatus.setTextColor(Color.parseColor("#C62828"))
                btnAction.visibility = View.VISIBLE
                btnAction.text = "Bayar Sekarang"
                btnAction.isEnabled = true
                btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF6F00"))

                btnAction.setOnClickListener {
                    if (orderResponse != null) {
                        showPaymentDialog(orderResponse!!)
                    } else {
                        Toast.makeText(this, "Data order hilang", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "processing" -> {
                tvStatus.text = "LUNAS / DIPROSES"
                tvStatus.setTextColor(Color.parseColor("#F57C00"))

                btnAction.visibility = View.VISIBLE
                btnAction.text = "Selesai & Kembali ke Menu"
                btnAction.backgroundTintList = ColorStateList.valueOf(Color.GRAY)

                btnAction.setOnClickListener {
                    kembaliKeMenu()
                }
            }
            "completed" -> {
                tvStatus.text = "SELESAI"
                tvStatus.setTextColor(Color.parseColor("#2E7D32"))

                btnAction.visibility = View.VISIBLE
                btnAction.text = "Pesan Lagi"
                btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))

                btnAction.setOnClickListener {
                    kembaliKeMenu()
                }
            }
            "cancelled" -> {
                tvStatus.text = "DIBATALKAN"
                tvStatus.setTextColor(Color.GRAY)
                btnAction.visibility = View.GONE
            }
        }
    }

    // --- PERBAIKAN 3: BAWA NOMOR MEJA SAAT PINDAH ---
    private fun kembaliKeMenu() {
        val i = Intent(this, MenuActivity::class.java)
        i.putExtra("meja", nomorMeja) // <--- INI KUNCINYA BIAR GAK JADI 0
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
        finish()
    }

    private fun showPaymentDialog(responseAPI: OrderResponse) {
        val dialogBuilder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.payment, null)

        val tvTotal = view.findViewById<TextView>(R.id.tvTotalBayarDialog)
        val etPin = view.findViewById<EditText>(R.id.etPinPayment)
        val btnBayar = view.findViewById<Button>(R.id.btnProsesBayar)
        val btnBatal = view.findViewById<TextView>(R.id.btnBatalBayar)

        val totalHarga = responseAPI.data?.finalTotal ?: 0.0
        tvTotal.text = formatRupiah(totalHarga)

        dialogBuilder.setView(view)
        val dialog = dialogBuilder.create()
        dialog.setCancelable(false)

        btnBayar.setOnClickListener {
            val pin = etPin.text.toString()
            if (pin == "123456") {
                btnBayar.text = "Memproses..."
                btnBayar.isEnabled = false
                verifikasiPembayaran(responseAPI.data?.id ?: 0, dialog)
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

                if (response.isSuccessful && response.body()?.success == true) {
                    val resp = response.body()
                    currentStatus = resp?.data?.status ?: "processing"
                    updateTampilanStatus()
                    Toast.makeText(this@ReceiptActivity, "Pembayaran Berhasil!", Toast.LENGTH_SHORT).show()
                } else {
                    btnAction.isEnabled = true
                    btnAction.text = "Bayar Sekarang"
                    Toast.makeText(this@ReceiptActivity, "Gagal Verifikasi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                dialog.dismiss()
                btnAction.isEnabled = true
                btnAction.text = "Bayar Sekarang"
                Toast.makeText(this@ReceiptActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}