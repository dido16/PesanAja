package com.example.pesanaja.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.ApiClient
import com.example.pesanaja.R
import com.example.pesanaja.entities.OrderModel
import com.example.pesanaja.entities.OrderResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HistoryAdapter(
    private val historyList: List<OrderModel>,
    // Callback refresh list
    private val onOrderUpdated: () -> Unit,
    // Callback batalkan pesanan (pending)
    private val onCancelClick: (OrderModel) -> Unit,
    // Callback hapus riwayat (cancelled)
    private val onDeleteClick: (OrderModel) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvTime: TextView = view.findViewById(R.id.tvHistoryTime)
        val tvName: TextView = view.findViewById(R.id.tvHistoryName)
        val cvStatus: CardView = view.findViewById(R.id.cvStatusBadge)
        val tvStatus: TextView = view.findViewById(R.id.tvHistoryStatus)
        val tvItems: TextView = view.findViewById(R.id.tvHistoryItems)
        val tvTotal: TextView = view.findViewById(R.id.tvHistoryTotal)
        val btnPay: Button = view.findViewById(R.id.btnPayLater)
        val btnCancel: Button = view.findViewById(R.id.btnCancelOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = historyList[position]

        // 1. Data Dasar
        holder.tvName.text = "Atas Nama: ${order.customerName ?: "Pelanggan"}"

        // 2. Format Tanggal
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(order.createdAt ?: "")
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
            holder.tvDate.text = dateFormat.format(date!!)
            holder.tvTime.text = "${timeFormat.format(date)} WIB"
        } catch (e: Exception) {
            holder.tvDate.text = order.createdAt ?: "-"
            holder.tvTime.text = ""
        }

        // 3. Logic Status & Warna
        val status = order.status ?: "unknown"
        val paymentMethod = order.paymentMethod ?: ""

        holder.tvStatus.text = status.uppercase()

        // Reset Listener tombol cancel biar gak numpuk
        holder.btnCancel.setOnClickListener(null)

        when (status.lowercase()) {
            "completed", "paid" -> {
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Hijau
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                holder.tvStatus.text = "SELESAI"
                holder.btnPay.visibility = View.GONE
                holder.btnCancel.visibility = View.GONE
            }
            "processing" -> {
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#FFF3E0")) // Oranye
                holder.tvStatus.setTextColor(Color.parseColor("#EF6C00"))
                holder.tvStatus.text = "DIPROSES"
                holder.btnPay.visibility = View.GONE
                holder.btnCancel.visibility = View.GONE
            }
            "pending" -> {
                // LOGIKA TUNAI vs QRIS
                if (paymentMethod.equals("cash", ignoreCase = true) || paymentMethod.equals("tunai", ignoreCase = true)) {
                    holder.cvStatus.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                    holder.tvStatus.setTextColor(Color.parseColor("#FF6F00"))
                    holder.tvStatus.text = "BAYAR DI KASIR"

                    holder.btnPay.text = "Refresh Status"
                    holder.btnPay.visibility = View.VISIBLE
                    holder.btnCancel.visibility = View.GONE
                } else {
                    holder.cvStatus.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                    holder.tvStatus.setTextColor(Color.parseColor("#C62828"))
                    holder.tvStatus.text = "BELUM BAYAR"

                    holder.btnPay.text = "Bayar"
                    holder.btnPay.visibility = View.VISIBLE
                    holder.btnCancel.visibility = View.VISIBLE

                    // Aksi Cancel Order
                    holder.btnCancel.text = "Batalkan"
                    holder.btnCancel.setBackgroundColor(Color.parseColor("#D32F2F")) // Merah default
                    holder.btnCancel.setOnClickListener { onCancelClick(order) }
                }
            }
            "cancelled" -> {
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#EEEEEE"))
                holder.tvStatus.setTextColor(Color.GRAY)
                holder.tvStatus.text = "DIBATALKAN"

                holder.btnPay.visibility = View.GONE

                // [FITUR BARU] Tombol Delete muncul disini
                holder.btnCancel.visibility = View.VISIBLE
                holder.btnCancel.text = "Hapus Riwayat"
                holder.btnCancel.setBackgroundColor(Color.GRAY) // Warna Abu biar beda

                holder.btnCancel.setOnClickListener {
                    onDeleteClick(order)
                }
            }
        }

        // 4. Detail Items
        val description = StringBuilder()
        order.items?.forEach { item ->
            val namaMenu = item.menu?.name ?: item.menuName ?: "Menu"
            val qty = item.quantity
            description.append("- $qty x $namaMenu\n")
        }
        holder.tvItems.text = description.toString().trim().ifEmpty { "Detail tidak tersedia" }

        // 5. Total Harga
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        holder.tvTotal.text = numberFormat.format(order.finalTotal)

        // TOMBOL BAYAR (Sama seperti sebelumnya)
        holder.btnPay.setOnClickListener {
            if (holder.btnPay.text == "Refresh Status") {
                onOrderUpdated()
            } else {
                showPaymentDialog(holder.itemView.context, order)
            }
        }
    }

    override fun getItemCount(): Int = historyList.size

    // --- LOGIKA DIALOG BAYAR ---
    private fun showPaymentDialog(context: Context, order: OrderModel) {
        val dialogBuilder = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.payment, null)

        val tvTotal = view.findViewById<TextView>(R.id.tvTotalBayarDialog)
        val rgMethod = view.findViewById<RadioGroup>(R.id.rgPaymentMethod)
        val layoutPin = view.findViewById<LinearLayout>(R.id.layoutPinContainer)
        val etPin = view.findViewById<EditText>(R.id.etPinPayment)
        val btnBayar = view.findViewById<Button>(R.id.btnProsesBayar)
        val btnBatal = view.findViewById<TextView>(R.id.btnBatalBayar)

        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        tvTotal.text = numberFormat.format(order.finalTotal)

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
                Toast.makeText(context, "Pilih metode bayar dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedId == R.id.rbTunai) {
                dialog.dismiss()
                Toast.makeText(context, "Silakan menuju kasir untuk pembayaran.", Toast.LENGTH_LONG).show()
                onOrderUpdated()
            } else {
                val pin = etPin.text.toString()
                if (pin == "123456") {
                    btnBayar.text = "Memproses..."
                    btnBayar.isEnabled = false
                    verifikasiPembayaran(context, order.id, dialog)
                } else {
                    etPin.error = "PIN Salah!"
                }
            }
        }

        btnBatal.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun verifikasiPembayaran(context: Context, orderId: Int, dialog: AlertDialog) {
        ApiClient.instance.payOrder(orderId).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                dialog.dismiss()
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(context, "Pembayaran Berhasil!", Toast.LENGTH_SHORT).show()
                    onOrderUpdated()
                } else {
                    Toast.makeText(context, "Gagal Verifikasi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}