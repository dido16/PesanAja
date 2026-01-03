package com.example.pesanaja

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private lateinit var tvTimestamp: TextView

    // View Baru sesuai XML Cantik
    private lateinit var tvMeja: TextView
    private lateinit var tvPelanggan: TextView

    // Data Order
    private var orderResponse: OrderResponse? = null
    private var currentStatus: String = "pending"
    private var nomorMeja: String = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        // 1. Inisialisasi View
        btnAction = findViewById(R.id.btnPayNow)
        tvStatus = findViewById(R.id.tvStatusOrder)
        tvTimestamp = findViewById(R.id.tvTimestamp)

        // Init View Baru (Pecahan Info)
        tvMeja = findViewById(R.id.tvReceiptMeja)
        tvPelanggan = findViewById(R.id.tvReceiptPelanggan)

        // 2. Ambil Data dari Intent
        orderResponse = intent.getSerializableExtra("order_response") as? OrderResponse
        val cartList = intent.getSerializableExtra("cart_list") as? ArrayList<CartItem> ?: arrayListOf()
        nomorMeja = intent.getStringExtra("meja") ?: "0"

        val orderData = orderResponse?.data
        currentStatus = orderData?.status ?: "pending"

        // 3. Set Data ke View (HEADER)
        tvTimestamp.text = orderData?.createdAt ?: "Waktu tidak tersedia"
        tvMeja.text = nomorMeja
        tvPelanggan.text = orderData?.customerName ?: "Pelanggan"

        // 4. Render List Item (Dibuat Kanan-Kiri biar Rapi)
        val container = findViewById<LinearLayout>(R.id.containerItems)
        container.removeAllViews()

        cartList.forEach { item ->
            // Container Baris per Item
            val row = LinearLayout(this)
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, 8, 0, 8) // Jarak antar baris

            // Bagian KIRI: Qty + Nama + Level
            val tvName = TextView(this)
            val infoLevel = if (item.extraCost > 0.0) " (+Level)" else ""
            val noteInfo = if (!item.notes.isNullOrEmpty()) "\n   (${item.notes})" else ""

            tvName.text = "${item.quantity}x ${item.menuName}$infoLevel$noteInfo"
            tvName.textSize = 14f
            tvName.setTextColor(Color.parseColor("#424242"))
            tvName.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) // Weight 1

            // Bagian KANAN: Total Harga per Item
            val totalItem = (item.price + item.extraCost) * item.quantity
            val tvPrice = TextView(this)
            tvPrice.text = formatRupiah(totalItem)
            tvPrice.textSize = 14f
            tvPrice.setTextColor(Color.parseColor("#212121"))
            tvPrice.typeface = Typeface.DEFAULT_BOLD

            // Gabung ke Row
            row.addView(tvName)
            row.addView(tvPrice)

            // Masukkan Row ke Container Utama
            container.addView(row)
        }

        // 5. Tampilkan Total Harga (Footer)
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
        // Reset Style Default Chip
        // (Asumsi di XML sudah pakai @drawable/bg_status_pending)

        when (currentStatus) {
            "pending" -> {
                tvStatus.text = "PENDING / BELUM BAYAR"
                tvStatus.setTextColor(Color.parseColor("#D32F2F")) // Merah
                // Background tetap merah muda (default xml)

                btnAction.visibility = View.VISIBLE
                btnAction.text = "Bayar Sekarang"
                btnAction.isEnabled = true
                btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5711B")) // Orange

                btnAction.setOnClickListener {
                    if (orderResponse != null) showPaymentDialog(orderResponse!!)
                }
            }
            "processing", "paid" -> {
                tvStatus.text = "LUNAS / DIPROSES"
                tvStatus.setTextColor(Color.parseColor("#388E3C")) // Hijau
                // Opsional: Ganti background jadi hijau muda kalau punya drawable-nya
                // tvStatus.setBackgroundResource(R.drawable.bg_status_success)

                btnAction.visibility = View.VISIBLE
                btnAction.text = "Selesai & Kembali ke Menu"
                btnAction.backgroundTintList = ColorStateList.valueOf(Color.GRAY)

                btnAction.setOnClickListener { kembaliKeMenu() }
            }
            "completed" -> {
                tvStatus.text = "SELESAI"
                tvStatus.setTextColor(Color.parseColor("#388E3C")) // Hijau

                btnAction.visibility = View.VISIBLE
                btnAction.text = "Pesan Lagi"
                btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))

                btnAction.setOnClickListener { kembaliKeMenu() }
            }
            "cancelled" -> {
                tvStatus.text = "DIBATALKAN"
                tvStatus.setTextColor(Color.GRAY)
                btnAction.visibility = View.GONE
            }
        }
    }

    private fun kembaliKeMenu() {
        val i = Intent(this, MenuActivity::class.java)
        i.putExtra("meja", nomorMeja)
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

        btnBatal.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun verifikasiPembayaran(orderId: Int, dialog: AlertDialog) {
        ApiClient.instance.payOrder(orderId).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                dialog.dismiss()

                if (response.isSuccessful && response.body()?.success == true) {
                    val resp = response.body()
                    // Update status lokal
                    currentStatus = resp?.data?.status ?: "processing"
                    // Update tampilan
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