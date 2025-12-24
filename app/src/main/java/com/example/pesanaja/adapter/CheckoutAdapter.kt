package com.example.pesanaja.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.R
import com.example.pesanaja.entities.CartItem

class CheckoutAdapter(
    private val items: MutableList<CartItem>,
    private val onTotalChanged: () -> Unit
) : RecyclerView.Adapter<CheckoutAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvNamaMenu)
        val tvDetail: TextView = view.findViewById(R.id.tvDetailHarga)
        val etNote: EditText = view.findViewById(R.id.etNoteItem)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveItem)
        val layoutLevel: LinearLayout = view.findViewById(R.id.layoutLevel)
        // Ganti Spinner jadi TextView
        val tvInfoLevel: TextView = view.findViewById(R.id.tvInfoLevel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_checkout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNama.text = item.menuName

        // Hitung total harga per item
        val totalPerItem = (item.price + item.extraCost) * item.quantity
        holder.tvDetail.text = "${item.quantity} x Rp ${item.price + item.extraCost} = Rp $totalPerItem"

        // --- FITUR LEVEL (TAMPILKAN SAJA) ---
        if (item.perluLevel) {
            holder.layoutLevel.visibility = View.VISIBLE

            // Data Level untuk Translasi ID ke Nama (Hanya Read-Only)
            val levelNames = listOf(
                "Level 0 (Netral)",            // ID 1
                "Level 1 (Sedikit)",           // ID 2
                "Level 2",                     // ID 3
                "Level 3",                     // ID 4
                "Level 4",                     // ID 5
                "Level 5 (+Rp 100)",           // ID 6
                "Level 6 (+Rp 200)",           // ID 7
                "Level 9 (Gila +Rp 500)",      // ID 10
                "Immortality (+Rp 500)",       // ID 14
                "Heavenly Demon (+Rp 1000)"    // ID 15
            )
            val levelIds = listOf(1, 2, 3, 4, 5, 6, 7, 10, 14, 15)

            // Cari nama level berdasarkan ID yang tersimpan di CartItem
            val index = levelIds.indexOf(item.levelId)
            if (index >= 0) {
                holder.tvInfoLevel.text = "Pedas: ${levelNames[index]}"
            } else {
                holder.tvInfoLevel.text = "Level: Standard"
            }

        } else {
            holder.layoutLevel.visibility = View.GONE
        }

        // --- Hapus Item ---
        holder.btnRemove.setOnClickListener {
            items.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
            notifyItemRangeChanged(holder.adapterPosition, items.size)
            onTotalChanged()
        }

        // --- Catatan ---
        holder.etNote.setOnFocusChangeListener(null)
        holder.etNote.setText(item.notes)
        holder.etNote.addTextChangedListener {
            item.notes = it.toString()
        }
    }

    override fun getItemCount(): Int = items.size
}