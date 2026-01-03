package com.example.pesanaja

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.pesanaja.entities.MenuModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.util.Locale

class MenuDetail(
    private val menu: MenuModel,
    private val currentQty: Int,
    private val onSave: (Int, Int?, Int, String?) -> Unit
) : BottomSheetDialogFragment() {

    private var qty = if (currentQty > 0) currentQty else 1
    private var selectedLevelId: Int? = null
    private var selectedExtraCost: Double = 0.0

    // Variabel buat simpan catatan sementara
    private var currentNote: String = ""

    // View references for updating UI
    private lateinit var tvNotePreview: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ganti nama layout sesuai XML kamu (bottom_sheet_menu)
        return inflater.inflate(R.layout.bottom_sheet_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inisialisasi View
        val ivImage: ImageView = view.findViewById(R.id.ivSheetImage)
        val tvName: TextView = view.findViewById(R.id.tvSheetName)
        val tvDesc: TextView = view.findViewById(R.id.tvSheetDesc)
        val tvPrice: TextView = view.findViewById(R.id.tvSheetPrice)

        val btnMinus: ImageButton = view.findViewById(R.id.btnSheetMinus)
        val btnPlus: ImageButton = view.findViewById(R.id.btnSheetPlus)
        val tvQty: TextView = view.findViewById(R.id.tvSheetQty)
        val btnSave: Button = view.findViewById(R.id.btnSheetSave)

        // Komponen Level
        val layoutLevel: LinearLayout = view.findViewById(R.id.layoutSheetLevel)
        val spinnerLevel: Spinner = view.findViewById(R.id.spinnerLevel)

        // Komponen Catatan
        val layoutNoteTrigger: LinearLayout = view.findViewById(R.id.layoutNoteTrigger)
        tvNotePreview = view.findViewById(R.id.tvNotePreview)

        // 2. SET DATA
        tvName.text = menu.name
        tvDesc.text = menu.description ?: "Tidak ada deskripsi."
        updatePriceDisplay(tvPrice)

        // Load Gambar
        val fullImageUrl = "http://192.168.1.104:8000/storage/images/menu/" + (menu.image ?: "")
        try {
            Glide.with(this)
                .load(fullImageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(ivImage)
        } catch (e: Exception) {
            // Handle error silently
        }

        tvQty.text = qty.toString()

        // 3. LOGIC LEVEL
        if (menu.hasLevel == 1 && !menu.levels.isNullOrEmpty()) {
            layoutLevel.visibility = View.VISIBLE
            spinnerLevel.visibility = View.VISIBLE

            val levelNames = menu.levels.map {
                val cost = if (it.extraCost > 0) " (+${formatRupiah(it.extraCost.toDouble())})" else ""
                "${it.name}$cost"
            }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, levelNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerLevel.adapter = adapter

            spinnerLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    val level = menu.levels[pos]
                    selectedLevelId = level.id
                    selectedExtraCost = level.extraCost.toDouble()
                    updatePriceDisplay(tvPrice)
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        } else {
            layoutLevel.visibility = View.GONE
            selectedLevelId = null
            selectedExtraCost = 0.0
        }

        // 4. LOGIC CATATAN (MENGGUNAKAN POP-UP CANTIK)
        layoutNoteTrigger.setOnClickListener {
            showCustomNoteDialog()
        }

        // 5. TOMBOL PLUS MINUS
        btnPlus.setOnClickListener {
            qty++
            tvQty.text = qty.toString()
        }

        btnMinus.setOnClickListener {
            if (qty > 1) {
                qty--
                tvQty.text = qty.toString()
            }
        }

        // 6. TOMBOL SIMPAN
        btnSave.setOnClickListener {
            if (menu.hasLevel == 1 && selectedLevelId == null) {
                Toast.makeText(context, "Harap pilih level dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kirim currentNote (hasil input dialog) ke Activity
            onSave(qty, selectedLevelId, selectedExtraCost.toInt(), currentNote)
            dismiss()
        }
    }

    // --- FUNGSI TAMPILKAN POP-UP DIALOG CATATAN (VERSI CANTIK) ---
    private fun showCustomNoteDialog() {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)

        // 1. Inflate Layout Custom (dialog_note.xml)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_note, null)

        // 2. Init Views
        val etDialogNote = view.findViewById<EditText>(R.id.etDialogNote)
        val btnDialogSave = view.findViewById<Button>(R.id.btnDialogSave)
        val btnDialogCancel = view.findViewById<Button>(R.id.btnDialogCancel)
        val tvDialogTitle = view.findViewById<TextView>(R.id.tvDialogTitle)

        // 3. Set Data Awal
        tvDialogTitle.text = "Catatan ${menu.name}"
        etDialogNote.setText(currentNote)
        etDialogNote.setSelection(etDialogNote.text.length)

        builder.setView(view)
        val dialog = builder.create()

        // 4. Set Background Transparan (PENTING untuk Rounded Corner)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 5. Action Tombol Simpan
        btnDialogSave.setOnClickListener {
            val inputNote = etDialogNote.text.toString().trim()
            currentNote = inputNote

            // Update UI Preview di BottomSheet
            if (currentNote.isNotEmpty()) {
                tvNotePreview.text = currentNote
                tvNotePreview.setTextColor(Color.parseColor("#FF9800")) // Orange (Active)
            } else {
                tvNotePreview.text = "Tambahkan catatan..."
                tvNotePreview.setTextColor(Color.parseColor("#757575")) // Grey
            }

            dialog.dismiss()
        }

        // 6. Action Tombol Batal
        btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updatePriceDisplay(tvPrice: TextView) {
        val totalSatuPorsi = menu.price.toDouble() + selectedExtraCost
        tvPrice.text = formatRupiah(totalSatuPorsi)
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        return numberFormat.format(number).replace("Rp", "Rp ")
    }
}