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
import com.example.pesanaja.entities.OrderModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HistoryAdapter(
    private val historyList: List<OrderModel>,
    private val onPayClick: (OrderModel) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // ID Sesuai Layout Baru
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvTime: TextView = view.findViewById(R.id.tvHistoryTime)
        val cvStatus: CardView = view.findViewById(R.id.cvStatusBadge) // Badge Background
        val tvStatus: TextView = view.findViewById(R.id.tvHistoryStatus) // Badge Text
        val tvItems: TextView = view.findViewById(R.id.tvHistoryItems)
        val tvTotal: TextView = view.findViewById(R.id.tvHistoryTotal)
        val btnPay: Button = view.findViewById(R.id.btnPayLater)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = historyList[position]

        // 1. Format Tanggal & Jam
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

        // 2. Logic Warna Status & Tombol Bayar
        val status = order.status ?: "unknown"
        holder.tvStatus.text = status.uppercase()

        when (status.lowercase()) {
            "completed", "paid" -> {
                // Style Hijau (Sukses)
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Hijau Muda
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32")) // Hijau Tua
                holder.tvStatus.text = "LUNAS"
                holder.btnPay.visibility = View.GONE // Umpetin tombol bayar
            }
            "pending" -> {
                // Style Merah/Oranye (Belum Lunas)
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Merah Muda
                holder.tvStatus.setTextColor(Color.parseColor("#C62828")) // Merah Tua
                holder.tvStatus.text = "BELUM BAYAR"
                holder.btnPay.visibility = View.VISIBLE // Munculin tombol bayar
            }
            else -> {
                holder.cvStatus.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                holder.tvStatus.setTextColor(Color.GRAY)
                holder.btnPay.visibility = View.GONE
            }
        }

        // 3. List Item
        val itemNames = order.items?.joinToString(", ") { item ->
            val realName = item.menuData?.name ?: item.menuName ?: "Menu"
            "${item.quantity}x $realName"
        } ?: "Detail tidak tersedia"
        holder.tvItems.text = itemNames

        // 4. Total Harga
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        holder.tvTotal.text = numberFormat.format(order.finalTotal)

        // Listener Tombol Bayar
        holder.btnPay.setOnClickListener {
            onPayClick(order)
        }
    }

    override fun getItemCount(): Int = historyList.size
}