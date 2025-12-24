package com.example.pesanaja.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button // Import Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.R
import com.example.pesanaja.entities.OrderModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// PERUBAHAN 1: Tambah parameter 'onPayClick' di konstruktor
// Ini fungsi "Titipan" dari Activity buat ngurusin klik bayar
class HistoryAdapter(
    private val historyList: List<OrderModel>,
    private val onPayClick: (OrderModel) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvStatus: TextView = view.findViewById(R.id.tvHistoryStatus)
        val tvItems: TextView = view.findViewById(R.id.tvHistoryItems)
        val tvTotal: TextView = view.findViewById(R.id.tvHistoryTotal)
        val btnPay: Button = view.findViewById(R.id.btnPayLater) // <--- Tambahan Button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = historyList[position]

        // 1. Format Tanggal
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(order.createdAt)
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            holder.tvDate.text = outputFormat.format(date!!)
        } catch (e: Exception) {
            holder.tvDate.text = order.createdAt ?: "-"
        }

        // 2. Logic Status & Tombol Bayar
        val status = order.status ?: "unknown"
        holder.tvStatus.text = status.uppercase()

        when (status) {
            "completed", "paid" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32")) // Hijau
                holder.tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
                holder.btnPay.visibility = View.GONE // Lunas? Umpetin tombolnya
            }
            "pending" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#EF6C00")) // Orange
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0"))
                holder.btnPay.visibility = View.VISIBLE // Belum lunas? Munculin tombolnya
            }
            else -> {
                holder.tvStatus.setTextColor(Color.GRAY)
                holder.tvStatus.setBackgroundColor(Color.LTGRAY)
                holder.btnPay.visibility = View.GONE
            }
        }

        // Kalau tombol diklik, lapor ke Activity lewat 'onPayClick'
        holder.btnPay.setOnClickListener {
            onPayClick(order)
        }

        // 3. Nama Items
        val itemNames = order.items?.joinToString(", ") { item ->
            val realName = item.menuData?.name ?: item.menuName ?: "Menu Dihapus"
            "$realName x${item.quantity}"
        } ?: "Detail kosong"
        holder.tvItems.text = itemNames

        // 4. Total Harga
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        holder.tvTotal.text = numberFormat.format(order.finalTotal)
    }

    override fun getItemCount(): Int = historyList.size
}