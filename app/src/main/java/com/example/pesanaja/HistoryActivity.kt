package com.example.pesanaja

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.adapter.HistoryAdapter
import com.example.pesanaja.entities.HistoryResponse
import com.example.pesanaja.entities.OrderModel
import com.example.pesanaja.entities.OrderResponse
import com.example.pesanaja.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        rvHistory = findViewById(R.id.rvHistory)
        progressBar = findViewById(R.id.progressBarHistory)
        tvEmpty = findViewById(R.id.tvEmptyHistory)
        btnBack = findViewById(R.id.btnBackHistory)

        rvHistory.layoutManager = LinearLayoutManager(this)
        btnBack.setOnClickListener { finish() }

        loadHistory()
    }

    private fun loadHistory() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_uuid", null)

        if (deviceId == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Belum ada riwayat."
            progressBar.visibility = View.GONE
            return
        }

        // Tampilkan loading pas refresh
        progressBar.visibility = View.VISIBLE

        ApiClient.instance.getHistory(deviceId).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body()?.success == true) {
                    val listData = response.body()?.data ?: emptyList()

                    if (listData.isNotEmpty()) {
                        // DISINI BEDANYA: Kita pasang aksi buat tombol 'onPayClick'
                        val adapter = HistoryAdapter(listData) { orderYangMauDibayar ->
                            // Pas tombol diklik, jalankan fungsi ini:
                            showPaymentDialog(orderYangMauDibayar)
                        }
                        rvHistory.adapter = adapter
                        tvEmpty.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(this@HistoryActivity, "Gagal ambil data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@HistoryActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- FUNGSI BAYAR SUSULAN (Copy-Paste Logic dari CheckoutActivity) ---
    private fun showPaymentDialog(order: OrderModel) {
        val dialogBuilder = AlertDialog.Builder(this)
        // Kita pakai layout 'payment.xml' yang SAMA dengan Checkout
        val view = LayoutInflater.from(this).inflate(R.layout.payment, null)

        val tvTotal = view.findViewById<TextView>(R.id.tvTotalBayarDialog)
        val rgMethod = view.findViewById<RadioGroup>(R.id.rgPaymentMethod)
        val etPin = view.findViewById<EditText>(R.id.etPinPayment)
        val btnBayar = view.findViewById<Button>(R.id.btnProsesBayar)
        val btnBatal = view.findViewById<TextView>(R.id.btnBatalBayar)

        // Set Total Harga
        tvTotal.text = "Rp ${order.finalTotal.toInt()}"

        dialogBuilder.setView(view)
        val dialog = dialogBuilder.create()
        dialog.setCancelable(true) // Kalau di history, boleh dicancel (tutup dialog)

        btnBayar.setOnClickListener {
            val pin = etPin.text.toString()
            if (pin == "123456") {
                btnBayar.text = "Memproses..."
                btnBayar.isEnabled = false

                // Panggil API Bayar
                prosesBayarKeAPI(order.id, dialog)
            } else {
                Toast.makeText(this, "PIN Salah!", Toast.LENGTH_SHORT).show()
            }
        }

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun prosesBayarKeAPI(orderId: Int, dialog: AlertDialog) {
        ApiClient.instance.payOrder(orderId).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                dialog.dismiss()
                if (response.isSuccessful) {
                    Toast.makeText(this@HistoryActivity, "Pembayaran Berhasil!", Toast.LENGTH_LONG).show()

                    // REFRESH LIST BIAR STATUSNYA JADI COMPLETED
                    loadHistory()
                } else {
                    Toast.makeText(this@HistoryActivity, "Gagal Bayar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(this@HistoryActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}