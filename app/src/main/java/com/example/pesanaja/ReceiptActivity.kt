package com.example.pesanaja

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.pesanaja.entities.CartItem
import com.example.pesanaja.entities.OrderResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class ReceiptActivity : AppCompatActivity() {

    private lateinit var btnAction: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var tvMeja: TextView
    private lateinit var tvPelanggan: TextView

    private var orderResponse: OrderResponse? = null
    private var currentStatus: String = "pending"
    private var nomorMeja: String = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        btnAction = findViewById(R.id.btnPayNow)
        tvStatus = findViewById(R.id.tvStatusOrder)
        tvTimestamp = findViewById(R.id.tvTimestamp)
        tvMeja = findViewById(R.id.tvReceiptMeja)
        tvPelanggan = findViewById(R.id.tvReceiptPelanggan)

        orderResponse = intent.getSerializableExtra("order_response") as? OrderResponse
        val cartList = intent.getSerializableExtra("cart_list") as? ArrayList<CartItem> ?: arrayListOf()
        nomorMeja = intent.getStringExtra("meja") ?: "0"

        val orderData = orderResponse?.data
        currentStatus = orderData?.status ?: "pending"

        tvTimestamp.text = orderData?.createdAt ?: "-"
        tvMeja.text = nomorMeja
        tvPelanggan.text = orderData?.customerName ?: "-"

        // Render Items
        val container = findViewById<LinearLayout>(R.id.containerItems)
        container.removeAllViews()
        cartList.forEach { item ->
            val row = LinearLayout(this)
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, 8, 0, 8)

            val tvName = TextView(this)
            val infoLevel = if (item.extraCost > 0.0) " (+Level)" else ""
            val noteInfo = if (!item.notes.isNullOrEmpty()) "\n   (${item.notes})" else ""

            tvName.text = "${item.quantity}x ${item.menuName}$infoLevel$noteInfo"
            tvName.textSize = 14f
            tvName.setTextColor(Color.parseColor("#424242"))
            tvName.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val totalItem = (item.price + item.extraCost) * item.quantity
            val tvPrice = TextView(this)
            tvPrice.text = formatRupiah(totalItem)
            tvPrice.textSize = 14f
            tvPrice.setTextColor(Color.parseColor("#212121"))
            tvPrice.typeface = Typeface.DEFAULT_BOLD

            row.addView(tvName)
            row.addView(tvPrice)
            container.addView(row)
        }

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
        val paymentMethod = orderResponse?.data?.paymentMethod ?: ""

        when (currentStatus) {
            "pending" -> {
                if (paymentMethod.equals("cash", ignoreCase = true) || paymentMethod.equals("tunai", ignoreCase = true)) {
                    tvStatus.text = "MENUNGGU PEMBAYARAN DI KASIR"
                    tvStatus.setTextColor(Color.parseColor("#FF6F00")) // Oranye

                    btnAction.visibility = View.VISIBLE
                    btnAction.text = "Saya Sudah Bayar (Refresh)"
                    btnAction.backgroundTintList = ColorStateList.valueOf(Color.GRAY)

                    btnAction.setOnClickListener {
                        Toast.makeText(this, "Silakan cek status di kasir", Toast.LENGTH_SHORT).show()
                        // Opsional: Bisa panggil API cek status disini
                    }
                } else {
                    tvStatus.text = "BELUM LUNAS"
                    tvStatus.setTextColor(Color.parseColor("#D32F2F")) // Merah

                    btnAction.visibility = View.VISIBLE
                    btnAction.text = "Bayar Sekarang"
                    btnAction.isEnabled = true
                    btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5711B"))

                    btnAction.setOnClickListener {
                        if (orderResponse != null) showPaymentDialog(orderResponse!!)
                    }
                }
            }
            "processing", "paid" -> {
                tvStatus.text = "LUNAS / DIPROSES"
                tvStatus.setTextColor(Color.parseColor("#388E3C"))

                btnAction.visibility = View.VISIBLE
                btnAction.text = "Selesai & Kembali ke Menu"
                btnAction.backgroundTintList = ColorStateList.valueOf(Color.GRAY)

                btnAction.setOnClickListener { kembaliKeMenu() }
            }
            "completed" -> {
                tvStatus.text = "SELESAI"
                tvStatus.setTextColor(Color.parseColor("#388E3C"))

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
        val rgMethod = view.findViewById<RadioGroup>(R.id.rgPaymentMethod)
        val layoutPin = view.findViewById<LinearLayout>(R.id.layoutPinContainer)
        val etPin = view.findViewById<EditText>(R.id.etPinPayment)
        val btnBayar = view.findViewById<Button>(R.id.btnProsesBayar)
        val btnBatal = view.findViewById<TextView>(R.id.btnBatalBayar)

        val totalHarga = responseAPI.data?.finalTotal ?: 0.0
        tvTotal.text = formatRupiah(totalHarga)

        dialogBuilder.setView(view)
        val dialog = dialogBuilder.create()
        dialog.setCancelable(false)

        rgMethod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbTunai) {
                layoutPin.visibility = View.GONE
                btnBayar.text = "KONFIRMASI DI KASIR"
            } else {
                layoutPin.visibility = View.VISIBLE
                btnBayar.text = "BAYAR SEKARANG"
            }
        }

        btnBayar.setOnClickListener {
            val selectedId = rgMethod.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Pilih metode bayar dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // A. TUNAI
            if (selectedId == R.id.rbTunai) {
                dialog.dismiss()
                Toast.makeText(this, "Silakan menuju kasir untuk pembayaran.", Toast.LENGTH_LONG).show()

                // Update data lokal manual agar UI berubah jadi 'Menunggu Kasir'
                val oldData = orderResponse?.data
                if (oldData != null) {
                    val newData = oldData.copy(paymentMethod = "cash")
                    orderResponse = OrderResponse(true, "Local Update", newData)
                }
                updateTampilanStatus()

            }
            // B. NON-TUNAI
            else {
                val pin = etPin.text.toString()
                if (pin == "123456") {
                    btnBayar.text = "Memproses..."
                    btnBayar.isEnabled = false
                    verifikasiPembayaran(responseAPI.data?.id ?: 0, dialog)
                } else {
                    etPin.error = "PIN Salah!"
                }
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
                    currentStatus = resp?.data?.status ?: "processing"
                    orderResponse = resp

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