package com.example.pesanaja.adapter

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.R
import com.example.pesanaja.entities.CartItem
import java.text.NumberFormat
import java.util.Locale

class CheckoutAdapter(
    private val items: MutableList<CartItem>,
    private val onTotalChanged: () -> Unit
) : RecyclerView.Adapter<CheckoutAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // ID BARU SESUAI item_checkout.xml
        val tvNama: TextView = view.findViewById(R.id.tvCheckoutName)
        val tvLevel: TextView = view.findViewById(R.id.tvCheckoutLevel)
        val tvQty: TextView = view.findViewById(R.id.tvCheckoutQty) // Tulisan "1 x @Rp..."
        val tvPrice: TextView = view.findViewById(R.id.tvCheckoutPrice) // Total harga kanan atas
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveItem)

        // Komponen Catatan Baru
        val layoutNote: LinearLayout = view.findViewById(R.id.layoutNoteTrigger)
        val tvNote: TextView = view.findViewById(R.id.tvCheckoutNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Pastikan layout yang dipanggil benar 'item_checkout'
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_checkout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Setup Format Rupiah
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)

        // 1. Set Nama Menu
        holder.tvNama.text = item.menuName

        // 2. Hitung Harga
        val hargaSatuan = item.price + item.extraCost
        val totalHargaItem = hargaSatuan * item.quantity

        // Tampilan: "2 x @Rp 15.000"
        holder.tvQty.text = "${item.quantity} x @${numberFormat.format(hargaSatuan)}"

        // Tampilan Total Kanan Atas: "Rp 30.000"
        holder.tvPrice.text = numberFormat.format(totalHargaItem)

        // 3. LOGIC LEVEL (Tampilkan Nama Kerennya)
        if (item.perluLevel) {
            holder.tvLevel.visibility = View.VISIBLE

            // Data Mapping Level (Sama kayak sebelumnya)
            val levelNames = listOf("Level 0 (Netral)", "Level 1", "Level 2", "Level 3", "Level 4", "Level 5", "Level 6", "Level 9", "Immortality", "Heavenly Demon")
            val levelIds = listOf(1, 2, 3, 4, 5, 6, 7, 10, 14, 15)

            // Cari nama level berdasarkan ID
            val index = levelIds.indexOf(item.levelId)
            val namaLevel = if (index >= 0) levelNames[index] else "Level Custom"

            // Format teks: "+ Immortality (Extra Rp 500)"
            val extraInfo = if (item.extraCost > 0) " (+Rp ${item.extraCost})" else ""
            holder.tvLevel.text = "+ $namaLevel$extraInfo"

        } else {
            holder.tvLevel.visibility = View.GONE
        }

        // 4. LOGIC CATATAN (POP-UP DIALOG)
        if (item.notes.isNullOrEmpty()) {
            holder.tvNote.text = "Tambahkan catatan..."
            holder.tvNote.setTextColor(Color.parseColor("#9E9E9E")) // Abu-abu
        } else {
            holder.tvNote.text = item.notes
            holder.tvNote.setTextColor(Color.parseColor("#212121")) // Hitam (Sudah diisi)
        }

        // Klik Kotak Catatan -> Muncul Dialog
        holder.layoutNote.setOnClickListener {
            showNoteDialog(holder.itemView.context, item, position)
        }

        // 5. Tombol Hapus
        holder.btnRemove.setOnClickListener {
            items.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
            notifyItemRangeChanged(holder.adapterPosition, items.size)
            onTotalChanged() // Update total harga di Activity
        }
    }

    // FUNGSI BUAT MUNCULIN DIALOG INPUT TEXT
    private fun showNoteDialog(context: Context, item: CartItem, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Catatan untuk ${item.menuName}")

        // Bikin EditText programmatically (tanpa XML tambahan)
        val input = EditText(context)
        input.hint = "opsional"
        input.setText(item.notes) // Isi teks yang sudah ada
        input.setSelection(input.text.length) // Kursor taruh di belakang

        // Kasih padding biar rapi dikit
        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = 50
        params.rightMargin = 50
        params.topMargin = 20
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        // Tombol Simpan
        builder.setPositiveButton("Simpan") { _, _ ->
            val catatannya = input.text.toString().trim()
            item.notes = catatannya // Update data di list
            notifyItemChanged(position) // Refresh tampilan baris ini aja
        }

        // Tombol Batal
        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    override fun getItemCount(): Int = items.size
}