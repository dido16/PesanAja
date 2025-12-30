package com.example.pesanaja.adapter

import android.content.Context
import android.graphics.Color
import android.text.InputType
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
        val tvNama: TextView = view.findViewById(R.id.tvCheckoutName)
        val tvLevel: TextView = view.findViewById(R.id.tvCheckoutLevel)
        val tvQty: TextView = view.findViewById(R.id.tvCheckoutQty)
        val tvPrice: TextView = view.findViewById(R.id.tvCheckoutPrice)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveItem)

        // Komponen Catatan
        val layoutNote: LinearLayout = view.findViewById(R.id.layoutNoteTrigger)
        val tvNote: TextView = view.findViewById(R.id.tvCheckoutNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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

        // 2. Hitung Harga (Double)
        val hargaSatuan = item.price + item.extraCost
        val totalHargaItem = hargaSatuan * item.quantity

        // Tampilan Qty & Harga Satuan
        holder.tvQty.text = "${item.quantity} x @${numberFormat.format(hargaSatuan)}"

        // Tampilan Total Kanan Atas
        holder.tvPrice.text = numberFormat.format(totalHargaItem)

        // 3. LOGIC LEVEL
        if (item.perluLevel) {
            holder.tvLevel.visibility = View.VISIBLE

            // Mapping Nama Level (Bisa disesuaikan dengan DB kamu)
            val levelNames = listOf("Level 0 (Netral)", "Level 1", "Level 2", "Level 3", "Level 4", "Level 5", "Level 6", "Level 9", "Immortality", "Heavenly Demon")
            val levelIds = listOf(1, 2, 3, 4, 5, 6, 7, 10, 14, 15)

            val index = levelIds.indexOf(item.levelId)
            val namaLevel = if (index >= 0) levelNames[index] else "Level Custom"

            // Format Biaya Tambahan (Cek > 0.0 karena Double)
            val extraInfo = if (item.extraCost > 0.0) " (+${numberFormat.format(item.extraCost)})" else ""

            holder.tvLevel.text = "+ $namaLevel$extraInfo"
        } else {
            holder.tvLevel.visibility = View.GONE
        }

        // 4. LOGIC CATATAN
        if (item.notes.isNullOrEmpty()) {
            holder.tvNote.text = "Tambahkan catatan..."
            holder.tvNote.setTextColor(Color.parseColor("#9E9E9E")) // Abu
        } else {
            holder.tvNote.text = item.notes
            holder.tvNote.setTextColor(Color.parseColor("#FF6F00")) // Oranye biar kelihatan kalau ada isinya
        }

        // Klik untuk edit catatan
        holder.layoutNote.setOnClickListener {
            showNoteDialog(holder.itemView.context, item, position)
        }

        // 5. Tombol Hapus
        holder.btnRemove.setOnClickListener {
            items.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
            notifyItemRangeChanged(holder.adapterPosition, items.size)
            onTotalChanged() // Kabari Activity untuk hitung ulang total
        }
    }

    private fun showNoteDialog(context: Context, item: CartItem, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Catatan ${item.menuName}")

        val input = EditText(context)
        input.hint = "Contoh: Jangan pedas, Es sedikit..."
        input.setText(item.notes)
        input.inputType = InputType.TYPE_CLASS_TEXT // Set keyboard text biasa
        input.setSelection(input.text.length)

        // Layout Container biar ada margin
        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = 60
        params.rightMargin = 60
        params.topMargin = 20
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        builder.setPositiveButton("Simpan") { _, _ ->
            val catatannya = input.text.toString().trim()
            item.notes = catatannya
            notifyItemChanged(position) // Refresh item ini saja
        }

        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    override fun getItemCount(): Int = items.size
}