package com.example.pesanaja

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.example.pesanaja.entities.MenuModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.util.Locale

// Callback diupdate: Mengirim Qty, LevelId, ExtraCost, dan Notes kembali ke Activity
class MenuDetail(
    private val menu: MenuModel,
    private val currentQty: Int,
    private val onSave: (Int, Int?, Int, String) -> Unit
) : BottomSheetDialogFragment() {

    private var qty = 1
    private var selectedLevelId: Int? = null
    private var selectedExtraCost: Int = 0

    // Data Level (Hardcoded sementara, idealnya dari API)
    private val levelNames = listOf("Level 0 (Netral)", "Level 1", "Level 2", "Level 3", "Level 4", "Level 5 (+100)", "Level 6 (+200)", "Level 9 (+500)", "Immortality (+500)", "Heavenly Demon (+1000)")
    private val levelIds = listOf(1, 2, 3, 4, 5, 6, 7, 10, 14, 15)
    private val extraCosts = listOf(0, 0, 0, 0, 0, 100, 200, 500, 500, 1000)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Pastikan nama layout XML sesuai dengan yang kita buat tadi (bottom_sheet_menu)
        return inflater.inflate(R.layout.bottom_sheet_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init View
        val ivImage = view.findViewById<ImageView>(R.id.ivSheetImage)
        val tvName = view.findViewById<TextView>(R.id.tvSheetName)
        val tvPrice = view.findViewById<TextView>(R.id.tvSheetPrice)
        val tvDesc = view.findViewById<TextView>(R.id.tvSheetDesc)
        val btnMinus = view.findViewById<ImageButton>(R.id.btnSheetMinus)
        val btnPlus = view.findViewById<ImageButton>(R.id.btnSheetPlus)
        val tvQty = view.findViewById<TextView>(R.id.tvSheetQty)
        val btnSave = view.findViewById<Button>(R.id.btnSheetSave)

        // Komponen Level (Yang baru kita tambah di XML)
        val layoutLevel = view.findViewById<LinearLayout>(R.id.layoutSheetLevel)
        val rgLevel = view.findViewById<RadioGroup>(R.id.rgSheetLevel)

        // 1. SET DATA DASAR
        tvName.text = menu.name
        tvDesc.text = menu.description ?: "Menu lezat siap disantap."

        // Load Gambar Server
        // PENTING: Sesuaikan IP Address Laptop kamu
        val baseUrl = "http://192.168.0.102:8000/storage/images/menu/"
        Glide.with(this)
            .load(baseUrl + (menu.image ?: ""))
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(ivImage)

        // Set Harga Awal
        updatePriceDisplay(tvPrice)

        // Set Qty Awal
        qty = if (currentQty > 0) currentQty else 1
        tvQty.text = qty.toString()

        // 2. LOGIC LEVEL PEDAS (Otomatis muncul jika perlu)
        if (menu.perluLevel == "YA") {
            layoutLevel.visibility = View.VISIBLE
            rgLevel.removeAllViews() // Bersihkan dulu biar gak dobel

            // Bikin RadioButton secara coding (Dinamis)
            for (i in levelNames.indices) {
                val rb = RadioButton(context)
                rb.text = levelNames[i]
                rb.id = i // ID pake index array
                rb.setPadding(0, 10, 0, 10)
                rgLevel.addView(rb)
            }

            // Listener kalau user ganti pilihan
            rgLevel.setOnCheckedChangeListener { _, checkedId ->
                // checkedId adalah index (0..9)
                if (checkedId >= 0 && checkedId < levelIds.size) {
                    selectedLevelId = levelIds[checkedId]
                    selectedExtraCost = extraCosts[checkedId]

                    // Update tampilan harga (Harga Dasar + Biaya Level)
                    updatePriceDisplay(tvPrice)
                }
            }

            // Default pilih Level 0 (Index 0)
            rgLevel.check(0)
        } else {
            // Kalau bukan makanan pedas (misal Minuman), sembunyikan level
            layoutLevel.visibility = View.GONE
            selectedLevelId = null
            selectedExtraCost = 0
        }

        // 3. LOGIC TOMBOL QTY
        btnMinus.setOnClickListener {
            if (qty > 1) { // Di detail minimal 1
                qty--
                tvQty.text = qty.toString()
            }
        }

        btnPlus.setOnClickListener {
            qty++
            tvQty.text = qty.toString()
        }

        // 4. TOMBOL SIMPAN
        btnSave.setOnClickListener {
            // Validasi: Kalau butuh level tapi anehnya belum terpilih
            if (menu.perluLevel == "YA" && selectedLevelId == null) {
                Toast.makeText(context, "Mohon pilih level pedas dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kirim 4 data penting ke Adapter/Activity
            onSave(qty, selectedLevelId, selectedExtraCost, "")
            dismiss()
        }
    }

    // Fungsi update harga biar rapi
    private fun updatePriceDisplay(tvPrice: TextView) {
        val totalSatuPorsi = menu.price + selectedExtraCost
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)

        if (selectedExtraCost > 0) {
            tvPrice.text = "${numberFormat.format(totalSatuPorsi)}"
            // Opsional: tvPrice.text = "${numberFormat.format(totalSatuPorsi)} (+Level)"
        } else {
            tvPrice.text = numberFormat.format(totalSatuPorsi)
        }
    }
}