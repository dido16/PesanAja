package com.example.pesanaja.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pesanaja.MenuDetail // Pastikan ini sesuai nama class BottomSheet kamu
import com.example.pesanaja.R
import com.example.pesanaja.entities.MenuModel
import com.example.pesanaja.ApiClient // Import ApiClient buat ambil URL Gambar
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

        // Tambahin parameter 'qty: Int' di depan
        fun onVariantChange(menuId: Int, qty: Int, levelId: Int?, extraCost: Int, note: String?)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        // Pastikan nama file XML layout kamu benar (misal: item_menu.xml)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menu = listMenu[position]
        val currentQty = quantities[menu.id] ?: 0

        // 1. Set Data Teks
        holder.tvName.text = menu.name
        holder.tvDesc.text = menu.description ?: "Menu lezat siap disantap."

        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        holder.tvPrice.text = numberFormat.format(menu.price)

        val fullImageUrl = "http://192.168.0.102:8000/storage/images/menu/" + (menu.image ?: "")

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

        // A. TOMBOL PLUS (+)
        holder.btnAdd.setOnClickListener {
            if (menu.hasLevel == 1) {
                // Jika punya level, harus buka BottomSheet buat pilih varian
                showBottomSheet(holder.itemView.context, menu, currentQty)
            } else {
                // Menu biasa, langsung tambah
                val newQty = currentQty + 1
                updateQty(menu.id, newQty, holder.adapterPosition)
            }
        }

        // B. TOMBOL MINUS (-)
        holder.btnMinus.setOnClickListener {
            if (currentQty > 0) {
                val newQty = currentQty - 1
                updateQty(menu.id, newQty, holder.adapterPosition)
            }
        }

        // C. KLIK KARTU (GAMBAR/BODY) -> BUKA DETAIL
        holder.itemView.setOnClickListener {
            showBottomSheet(holder.itemView.context, menu, currentQty)
        }
    }

    // Fungsi Helper buat Buka BottomSheet
    private fun showBottomSheet(context: android.content.Context, menu: MenuModel, currentQty: Int) {
        val activity = context as? AppCompatActivity
        activity?.let { act ->
            // Callback dari MenuDetail: (qtyBaru, lvlId, extra, note)
            val bottomSheet = MenuDetail(menu, currentQty) { qtyBaru, lvlId, extra, note ->

                // Update tampilan angka di list utama (opsional, visual aja)
                updateQty(menu.id, qtyBaru, -1)

                // KIRIM QTY JUGA KE ACTIVITY
                listener.onVariantChange(menu.id, qtyBaru, lvlId, extra, note)
            }
            bottomSheet.show(act.supportFragmentManager, "MenuDetail")
        }
    }


    private fun updateQty(id: Int, newQty: Int, position: Int) {
        quantities[id] = newQty
        listener.onQuantityChange(id, newQty) // Kabari Activity soal perubahan jumlah

        if (position != -1) {
            notifyItemChanged(position) // Refresh cuma item ini (biar ga kedip)
        } else {
            notifyDataSetChanged() // Fallback refresh semua
        }
    }

    override fun getItemCount(): Int = listMenu.size
}