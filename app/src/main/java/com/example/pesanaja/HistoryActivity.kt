package com.example.pesanaja

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.adapter.HistoryAdapter
import com.example.pesanaja.entities.HistoryResponse
import com.example.pesanaja.entities.OrderModel
import okhttp3.ResponseBody
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

        progressBar.visibility = View.VISIBLE

        ApiClient.instance.getHistory(deviceId).enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body()?.success == true) {
                    val listData = response.body()?.data ?: emptyList()

                    if (listData.isNotEmpty()) {

                        // --- UPDATE DI SINI ---
                        // Menambahkan callback ke-3 untuk DELETE
                        val adapter = HistoryAdapter(
                            historyList = listData,

                            // Callback 1: Refresh list (misal habis bayar)
                            onOrderUpdated = {
                                loadHistory()
                            },

                            // Callback 2: Batalkan pesanan (Status Pending)
                            onCancelClick = { order ->
                                showCancelConfirmation(order)
                            },

                            // Callback 3: Hapus Riwayat (Status Cancelled)
                            onDeleteClick = { order ->
                                showDeleteConfirmation(order)
                            }
                        )

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

    // --- LOGIKA BATALKAN PESANAN (UPDATE STATUS -> CANCELLED) ---
    private fun showCancelConfirmation(order: OrderModel) {
        AlertDialog.Builder(this)
            .setTitle("Batalkan Pesanan")
            .setMessage("Apakah Anda yakin ingin membatalkan pesanan ini?")
            .setPositiveButton("Ya, Batal") { _, _ ->
                prosesBatalKeAPI(order.id)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun prosesBatalKeAPI(orderId: Int) {
        Toast.makeText(this, "Membatalkan pesanan #$orderId...", Toast.LENGTH_SHORT).show()

        ApiClient.instance.updateOrderStatus(orderId, "cancelled").enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@HistoryActivity, "Berhasil Dibatalkan", Toast.LENGTH_SHORT).show()
                    loadHistory() // Refresh agar status berubah jadi CANCELLED dan tombol jadi HAPUS
                } else {
                    Toast.makeText(this@HistoryActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@HistoryActivity, "Kesalahan Jaringan", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- LOGIKA HAPUS RIWAYAT (DELETE FROM DB) ---
    private fun showDeleteConfirmation(order: OrderModel) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Riwayat")
            .setMessage("Hapus riwayat pesanan ini selamanya? Data tidak bisa dikembalikan.")
            .setPositiveButton("Hapus") { _, _ ->
                prosesHapusKeAPI(order.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun prosesHapusKeAPI(orderId: Int) {
        ApiClient.instance.deleteOrder(orderId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@HistoryActivity, "Riwayat dihapus", Toast.LENGTH_SHORT).show()
                    loadHistory() // Refresh agar item hilang dari list
                } else {
                    Toast.makeText(this@HistoryActivity, "Gagal menghapus", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@HistoryActivity, "Error koneksi", Toast.LENGTH_SHORT).show()
            }
        })
    }
}