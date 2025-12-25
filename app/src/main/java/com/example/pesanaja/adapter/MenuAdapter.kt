package com.example.pesanaja.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pesanaja.MenuDetail // Pastikan nama class BottomSheet kamu ini (MenuDetail atau MenuDetailBottomSheet)
import com.example.pesanaja.R
import com.example.pesanaja.entities.MenuModel
import java.text.NumberFormat
import java.util.Locale

class MenuAdapter(
    private val listMenu: List<MenuModel>,
    private val listener: OnCartChangeListener
) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

    // Simpan jumlah per menuId secara lokal
    private val quantities = mutableMapOf<Int, Int>()

    interface OnCartChangeListener {
        fun onQuantityChange(menuId: Int, quantity: Int)

        // --- PERBAIKAN 1: BUKA KOMENTAR INI ---
        // Biar MenuActivity gak error "overrides nothing"
        fun onLevelChange(menuId: Int, levelId: Int, extraCost: Int)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // ID BARU SESUAI item_menu.xml
        val ivImage: ImageView = view.findViewById(R.id.ivMenuImage)
        val tvName: TextView = view.findViewById(R.id.tvMenuName)
        val tvPrice: TextView = view.findViewById(R.id.tvMenuPrice)
        val tvDesc: TextView = view.findViewById(R.id.tvMenuDesc)

        // Komponen Kontrol
        val btnAdd: CardView = view.findViewById(R.id.btnAdd)
        val btnMinus: CardView = view.findViewById(R.id.btnMinus)
        val tvQty: TextView = view.findViewById(R.id.tvQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Pastikan nama file layout XML kamu 'item_menu' (sesuai yang kita buat sebelumnya)
        // Kalau nama file kamu 'menu_item.xml', ganti jadi R.layout.menu_item
        val view = LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menu = listMenu[position]
        val currentQty = quantities[menu.id] ?: 0

        // 1. Set Data
        holder.tvName.text = menu.name
        holder.tvDesc.text = menu.description ?: "Menu lezat siap disantap."

        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        holder.tvPrice.text = numberFormat.format(menu.price)

        // 2. Load Gambar
        val baseUrl = "http://192.168.0.102:8000/storage/images/menu/" // Sesuaikan IP
        val fullImageUrl = baseUrl + (menu.image ?: "")

        Glide.with(holder.itemView.context)
            .load(fullImageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_delete)
            .into(holder.ivImage)

        // 3. Logika Visibility Tombol
        if (currentQty > 0) {
            holder.btnMinus.visibility = View.VISIBLE
            holder.tvQty.visibility = View.VISIBLE
            holder.tvQty.text = currentQty.toString()
        } else {
            holder.btnMinus.visibility = View.GONE
            holder.tvQty.visibility = View.GONE
        }

        // --- INTERAKSI TOMBOL ---

        holder.btnAdd.setOnClickListener {
            val newQty = currentQty + 1
            updateQty(menu.id, newQty)
        }

        holder.btnMinus.setOnClickListener {
            if (currentQty > 0) {
                val newQty = currentQty - 1
                updateQty(menu.id, newQty)
            }
        }

        // C. KLIK KARTU -> BUKA BOTTOM SHEET
        holder.itemView.setOnClickListener {
            val activity = holder.itemView.context as? AppCompatActivity

            activity?.let { act ->
                // --- PERBAIKAN 2: TERIMA 4 PARAMETER ---
                // (qtyBaru, lvlId, extra, note)
                val bottomSheet = MenuDetail(menu, currentQty) { qtyBaru, lvlId, extra, note ->

                    // 1. Update Quantity
                    updateQty(menu.id, qtyBaru)

                    // 2. Kalau ada Level, kirim ke Activity
                    if (lvlId != null) {
                        listener.onLevelChange(menu.id, lvlId, extra)
                    }

                    // (Nanti 'note' bisa kita kirim juga kalau mau fitur catatan)
                }
                bottomSheet.show(act.supportFragmentManager, "MenuDetail")
            }
        }
    }

    private fun updateQty(id: Int, newQty: Int) {
        quantities[id] = newQty
        notifyDataSetChanged()
        listener.onQuantityChange(id, newQty)
    }

    override fun getItemCount(): Int = listMenu.size
}