package com.example.pesanaja.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.R
import com.example.pesanaja.entities.CartItem

class CheckoutAdapter(private val items: List<CartItem>) :
    RecyclerView.Adapter<CheckoutAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvNamaMenu)
        val tvDetail: TextView = view.findViewById(R.id.tvDetailHarga) // Sesuaikan ID ini
        val etNote: EditText = view.findViewById(R.id.etNoteItem) // Sesuaikan ID ini
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_checkout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNama.text = item.menuName

        val totalPerItem = item.price * item.quantity
        holder.tvDetail.text = "${item.quantity} x Rp ${item.price} = Rp $totalPerItem"

        holder.etNote.setText(item.notes)
        holder.etNote.addTextChangedListener {
            item.notes = it.toString() // Nyimpan catatan otomatis ke list
        }
    }

    override fun getItemCount(): Int = items.size
}