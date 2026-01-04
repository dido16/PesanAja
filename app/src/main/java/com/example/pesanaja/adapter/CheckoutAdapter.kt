package com.example.pesanaja.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
    private val onTotalChanged: () -> Unit,
    private val onItemClick: (CartItem, Int) -> Unit
) : RecyclerView.Adapter<CheckoutAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvCheckoutName)
        val tvLevel: TextView = view.findViewById(R.id.tvCheckoutLevel)
        val tvQty: TextView = view.findViewById(R.id.tvCheckoutQty)
        val tvPrice: TextView = view.findViewById(R.id.tvCheckoutPrice)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveItem)

        // Bagian Catatan
        val tvNote: TextView = view.findViewById(R.id.tvCheckoutNote)
        val layoutNote: LinearLayout = view.findViewById(R.id.layoutNoteTrigger)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_checkout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)

        // 1. Set Nama
        holder.tvNama.text = item.menuName

        // 2. Hitung Harga
        val hargaSatuan = item.price + item.extraCost
        val totalHargaItem = hargaSatuan * item.quantity

        // Format: "1 x Rp 12.000"
        holder.tvQty.text = "${item.quantity} x ${numberFormat.format(hargaSatuan)}"
        holder.tvPrice.text = numberFormat.format(totalHargaItem)

        // 3. Logic Level
        if (item.perluLevel) {
            holder.tvLevel.visibility = View.VISIBLE
            val namaLevel = item.level?.name ?: "Level Custom"
            val extraInfo = if (item.extraCost > 0.0) " (+${numberFormat.format(item.extraCost)})" else ""
            holder.tvLevel.text = "+ Level $namaLevel$extraInfo"
        } else {
            holder.tvLevel.visibility = View.GONE
        }

        // 4. Logic Catatan
        holder.tvNote.visibility = View.VISIBLE

        if (item.notes.isNullOrEmpty()) {
            holder.tvNote.text = "Tambahkan catatan..."
            holder.tvNote.setTextColor(Color.parseColor("#9E9E9E")) // Abu-abu
            holder.tvNote.setTypeface(null, android.graphics.Typeface.ITALIC)
        } else {
            holder.tvNote.text = "Catatan: ${item.notes}"
            holder.tvNote.setTextColor(Color.parseColor("#FF9800")) // Oranye (Biar kelihatan ada isi)
            holder.tvNote.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        // 5. Tombol Hapus
        holder.btnRemove.setOnClickListener {
            items.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
            notifyItemRangeChanged(holder.adapterPosition, items.size)
            onTotalChanged()
        }

        // --- FITUR EDIT ---

        holder.itemView.setOnClickListener {
            onItemClick(item, holder.adapterPosition)
        }

        holder.layoutNote.setOnClickListener {
            showCustomNoteDialog(holder.itemView.context, item, holder.adapterPosition)
        }
    }

    // --- FUNGSI DIALOG ---
    private fun showCustomNoteDialog(context: Context, item: CartItem, position: Int) {
        val dialogBuilder = AlertDialog.Builder(context)

        // Inflate layout custom yang kita buat (dialog_note.xml)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_note, null)

        val etNote = view.findViewById<EditText>(R.id.etDialogNote)
        val btnSave = view.findViewById<Button>(R.id.btnDialogSave)
        val btnCancel = view.findViewById<Button>(R.id.btnDialogCancel)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)

        // Set Data Awal
        tvTitle.text = "Catatan ${item.menuName}"
        etNote.setText(item.notes)
        etNote.setSelection(etNote.text.length)

        dialogBuilder.setView(view)
        val dialog = dialogBuilder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Tombol Simpan
        btnSave.setOnClickListener {
            val catatannya = etNote.text.toString().trim()
            item.notes = catatannya

            notifyItemChanged(position)
            dialog.dismiss()
        }

        // Tombol Batal
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun getItemCount(): Int = items.size
}