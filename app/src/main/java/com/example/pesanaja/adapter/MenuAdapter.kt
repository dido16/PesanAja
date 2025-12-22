package com.example.pesanaja.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pesanaja.R
import com.example.pesanaja.entities.MenuModel

class MenuAdapter(
    private val listMenu: List<MenuModel>,
    private val listener: OnCartChangeListener // Tambahkan listener ini
) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

    // Simpan jumlah per menuId secara lokal di adapter
    private val quantities = mutableMapOf<Int, Int>()

    interface OnCartChangeListener {
        fun onQuantityChange(menuId: Int, quantity: Int)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgMenu: ImageView = view.findViewById(R.id.imgMenu)
        val txtNama: TextView = view.findViewById(R.id.txtNama)
        val txtHarga: TextView = view.findViewById(R.id.txtHarga)
        val txtQty: TextView = view.findViewById(R.id.txtQty) // Tambahkan di XML: TextView untuk angka
        val btnPlus: Button = view.findViewById(R.id.btnPlus)   // Tambahkan di XML: Button +
        val btnMinus: Button = view.findViewById(R.id.btnMinus) // Tambahkan di XML: Button -
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menu = listMenu[position]
        val currentQty = quantities[menu.id] ?: 0
        val baseUrl = "http://192.168.36.97:8000/storage/images/menu/"
        val fullImageUrl = baseUrl + menu.image

        holder.txtNama.text = menu.name
        holder.txtHarga.text = "Rp ${menu.price}"
        holder.txtQty.text = currentQty.toString()


        Glide.with(holder.imgMenu.context)
            .load(fullImageUrl)
            .placeholder(R.drawable.placeholder_loading) // Buat drawable sementara
            .error(R.drawable.error_image)
            .into(holder.imgMenu)

        holder.btnPlus.setOnClickListener {
            val newQty = (quantities[menu.id] ?: 0) + 1
            quantities[menu.id] = newQty
            holder.txtQty.text = newQty.toString()
            listener.onQuantityChange(menu.id, newQty)
        }

        holder.btnMinus.setOnClickListener {
            val current = quantities[menu.id] ?: 0
            if (current > 0) {
                val newQty = current - 1
                quantities[menu.id] = newQty
                holder.txtQty.text = newQty.toString()
                listener.onQuantityChange(menu.id, newQty)
            }
        }
    }

    override fun getItemCount(): Int = listMenu.size
}