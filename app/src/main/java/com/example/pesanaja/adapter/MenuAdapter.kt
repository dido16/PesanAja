package com.example.pesanaja.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pesanaja.R
import com.example.pesanaja.entities.MenuModel

class MenuAdapter(
    private val listMenu: List<MenuModel>,
    private val listener: OnCartChangeListener
) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

    // Simpan jumlah per menuId secara lokal
    private val quantities = mutableMapOf<Int, Int>()

    // Interface diperbarui: Tambah onLevelChange buat nangkep pilihan spinner
    interface OnCartChangeListener {
        fun onQuantityChange(menuId: Int, quantity: Int)
        fun onLevelChange(menuId: Int, levelId: Int, extraCost: Int)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgMenu: ImageView = view.findViewById(R.id.imgMenu)
        val txtNama: TextView = view.findViewById(R.id.txtNama)
        val txtHarga: TextView = view.findViewById(R.id.txtHarga)
        val txtQty: TextView = view.findViewById(R.id.txtQty)
        val btnPlus: Button = view.findViewById(R.id.btnPlus)
        val btnMinus: Button = view.findViewById(R.id.btnMinus)

        // Komponen Baru buat Level (Sesuai XML item_menu.xml yang baru)
        val layoutLevel: LinearLayout = view.findViewById(R.id.layoutLevelInput)
        val spinnerLevel: Spinner = view.findViewById(R.id.spinnerLevel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false) // Pastikan nama file xml bener (item_menu atau menu_item)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val menu = listMenu[position]
        val currentQty = quantities[menu.id] ?: 0

        // Sesuaikan IP ini dengan konfigurasi lokalmu
        val baseUrl = "http://192.168.0.102:8000/storage/images/menu/"
        val fullImageUrl = baseUrl + (menu.image ?: "")

        holder.txtNama.text = menu.name
        holder.txtHarga.text = "Rp ${menu.price}"
        holder.txtQty.text = currentQty.toString()

        Glide.with(holder.imgMenu.context)
            .load(fullImageUrl)
            .placeholder(R.drawable.placeholder_loading) // Pastikan gambar ini ada di drawable
            .error(R.drawable.error_image) // Pastikan gambar ini ada di drawable
            .into(holder.imgMenu)

        // --- FITUR LEVEL PEDAS ---
        // Cek apakah menu ini butuh level (Sesuai data dari Laravel "YA"/"TIDAK")
        if (menu.perluLevel == "YA") {
            holder.layoutLevel.visibility = View.VISIBLE

            // Data Level Hardcoded Sesuai Database Kamu
            val levelNames = listOf(
                "Level 0 (Netral)",
                "Level 1",
                "Level 2",
                "Level 3",
                "Level 4",
                "Level 5 (+100)",
                "Level 6 (+200)",
                "Level 9 (+500)",
                "Immortality (+500)",
                "Heavenly Demon (+1000)"
            )
            val levelIds = listOf(1, 2, 3, 4, 5, 6, 7, 10, 14, 15)
            val extraCosts = listOf(0, 0, 0, 0, 0, 100, 200, 500, 500, 1000)

            val adapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_dropdown_item, levelNames)
            holder.spinnerLevel.adapter = adapter

            holder.spinnerLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    // Kirim data Level yang dipilih ke Activity
                    listener.onLevelChange(menu.id, levelIds[pos], extraCosts[pos])
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } else {
            // Kalau menu nggak butuh level (misal Minuman), sembunyikan spinner
            holder.layoutLevel.visibility = View.GONE
        }

        // --- TOMBOL PLUS MINUS ---
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