package com.example.pesanaja.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.R
import com.example.pesanaja.entities.OrderModel // Pastikan ini sesuai dengan Entity kamu (OrderModel atau OrderData)
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HistoryAdapter(
    private val historyList: List<OrderModel>, // Sesuaikan tipe data listnya
    private val onPayClick: (OrderModel) -> Unit,
    private val onCancelClick: (OrderModel) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvTime: TextView = view.findViewById(R.id.tvHistoryTime)

        // [BARU] Tambahkan binding untuk Nama Pelanggan
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

        // 1. [BARU] Set Nama Pelanggan
        // Pastikan di OrderModel/OrderData sudah ada field 'customerName'
        holder.tvName.text = "Atas Nama: ${order.customerName ?: "Pelanggan"}"

        // 2. Format Tanggal & Jam
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

        // 3. Logic Warna Status & Tombol
        val status = order.status ?: "unknown"
        holder.tvStatus.text = status.uppercase()

        when (status.lowercase()) {
            "completed", "paid" -> {
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Hijau Muda
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32")) // Hijau Tua
                holder.tvStatus.text = "SELESAI"

                holder.btnPay.visibility = View.GONE
                holder.btnCancel.visibility = View.GONE
            }
            "processing" -> {
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#FFF3E0")) // Oranye Muda
                holder.tvStatus.setTextColor(Color.parseColor("#EF6C00")) // Oranye Tua
                holder.tvStatus.text = "DIPROSES"

                holder.btnPay.visibility = View.GONE
                holder.btnCancel.visibility = View.GONE
            }
            "pending" -> {
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Merah Muda
                holder.tvStatus.setTextColor(Color.parseColor("#C62828")) // Merah Tua
                holder.tvStatus.text = "BELUM BAYAR"

                // Tombol Muncul
                holder.btnPay.visibility = View.VISIBLE
                holder.btnCancel.visibility = View.VISIBLE
            }
            "cancelled" -> {
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#EEEEEE")) // Abu
                holder.tvStatus.setTextColor(Color.GRAY)
                holder.tvStatus.text = "DIBATALKAN"

                holder.btnPay.visibility = View.GONE
                holder.btnCancel.visibility = View.GONE
            }
            else -> {
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                holder.tvStatus.setTextColor(Color.GRAY)
                holder.btnPay.visibility = View.GONE
                holder.btnCancel.visibility = View.GONE
            }
        }

        // 4. List Item
        val description = StringBuilder()
        order.items?.forEach { item ->
            val namaMenu = item.menu?.name ?: item.menuName ?: "Menu"
            val namaLevel = item.level?.name ?: "" // Ambil nama level dari relasi (jika ada)
            val qty = item.quantity

            description.append("- $qty x $namaMenu")
            if (namaLevel.isNotEmpty()) {
                description.append(" ($namaLevel)")
            }
            description.append("\n")
        }

        val resText = description.toString().trim()
        holder.tvItems.text = if (resText.isEmpty()) "Detail tidak tersedia" else resText

        // 5. Total Harga
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        holder.tvTotal.text = numberFormat.format(order.finalTotal)

        // 6. Listener Klik Tombol
        holder.btnCancel.setOnClickListener {
            onCancelClick(order)
        }

        holder.btnPay.setOnClickListener {
            onPayClick(order)
        }
    }

    override fun getItemCount(): Int = historyList.size
}