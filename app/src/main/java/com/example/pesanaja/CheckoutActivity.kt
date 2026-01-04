package com.example.pesanaja

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pesanaja.adapter.CheckoutAdapter
import com.example.pesanaja.entities.*
import com.example.pesanaja.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class CheckoutActivity : AppCompatActivity() {

    private lateinit var etNama: EditText
    private lateinit var tvSubtotal: TextView
    private lateinit var tvPajak: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var rvItems: RecyclerView
    private lateinit var btnKirim: Button

    private var nomorMeja: String = ""
    private var checkoutList = mutableListOf<CartItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // 1. Inisialisasi View
        etNama = findViewById(R.id.etNamaPelanggan)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvPajak = findViewById(R.id.tvPajak)
        tvGrandTotal = findViewById(R.id.tvTotalBayar)
        rvItems = findViewById(R.id.rvCheckoutItems)
        btnKirim = findViewById(R.id.btnKonfirmasiPesan)

        // 2. Ambil data dari Intent
        nomorMeja = intent.getStringExtra("meja") ?: "0"
        findViewById<TextView>(R.id.tvDetailMeja).text = "Meja: $nomorMeja"

        val dataIntent = intent.getSerializableExtra("cart_list") as? ArrayList<CartItem>
        if (dataIntent != null) checkoutList.addAll(dataIntent)

        // 3. Setup RecyclerView & Hitung Awal
        setupRecyclerView()
        hitungTagihan()

        // 4. Tombol Kirim
        btnKirim.setOnClickListener {
            val nama = etNama.text.toString().trim()
            if (nama.isEmpty()) {
                etNama.error = "Nama pelanggan wajib diisi!"
            } else if (checkoutList.isEmpty()) {
                Toast.makeText(this, "Keranjang kosong!", Toast.LENGTH_SHORT).show()
            } else {
                showConfirmationDialog(nama)
            }
        }
    }

    // --- FIX BUG: Kirim data balik saat tombol Back HP ditekan ---
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        kembalikanDataKeMenu()
        super.onBackPressed()
    }

    private fun kembalikanDataKeMenu() {
        val resultIntent = Intent()
        resultIntent.putExtra("updated_cart", ArrayList(checkoutList))
        setResult(RESULT_OK, resultIntent)
    }

    // --- SETUP RECYCLERVIEW ---
    private fun setupRecyclerView() {
        rvItems.layoutManager = LinearLayoutManager(this)

        rvItems.adapter = CheckoutAdapter(
            items = checkoutList,
            onTotalChanged = { hitungTagihan() },
            onItemClick = { item, position ->
                showEditDialog(item, position)
            }
        )
    }

    // --- FITUR EDIT ITEM ---
    private fun showEditDialog(item: CartItem, position: Int) {
        if (item.menu == null) return

        val bottomSheet = MenuDetail(item.menu!!, item.quantity) { qtyBaru, lvlId, extra, note ->

            if (qtyBaru == 0) {
                checkoutList.removeAt(position)
                rvItems.adapter?.notifyItemRemoved(position)
            } else {
                item.quantity = qtyBaru
                item.levelId = lvlId
                item.extraCost = extra.toDouble()
                item.notes = note ?: ""
                item.level = item.menu!!.levels?.find { it.id == lvlId }

                rvItems.adapter?.notifyItemChanged(position)
            }

            hitungTagihan()
        }

        bottomSheet.show(supportFragmentManager, "EditItemSheet")
    }

    private fun hitungTagihan() {
        var subtotal = 0.0

        for (item in checkoutList) {
            subtotal += (item.price + item.extraCost) * item.quantity
        }

        val pajak = subtotal * 0.10
        val totalAkhir = subtotal + pajak

        tvSubtotal.text = formatRupiah(subtotal)
        tvPajak.text = formatRupiah(pajak)
        tvGrandTotal.text = formatRupiah(totalAkhir)
    }

    private fun formatRupiah(number: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        return numberFormat.format(number).replace("Rp", "Rp ")
    }

    // --- DIALOG KONFIRMASI AWAL ---
    private fun showConfirmationDialog(nama: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_order, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val tvNama = dialogView.findViewById<TextView>(R.id.tvConfirmNama)
        val tvMeja = dialogView.findViewById<TextView>(R.id.tvConfirmMeja)
        val containerItems = dialogView.findViewById<LinearLayout>(R.id.containerConfirmItems)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvConfirmTotal)
        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatalConfirm)
        val btnKirimConfirm = dialogView.findViewById<Button>(R.id.btnKirimConfirm)

        tvNama.text = "Atas Nama: $nama"
        tvMeja.text = "Meja: $nomorMeja"
        tvTotal.text = tvGrandTotal.text

        containerItems.removeAllViews()
        checkoutList.forEach { item ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, 8, 0, 8)

            val tvItemName = TextView(this)
            val levelInfo = if (item.extraCost > 0.0) "\n+Level" else ""
            val noteInfo = if (!item.notes.isNullOrEmpty()) "\nCatatan: ${item.notes}" else ""

            tvItemName.text = "${item.quantity}x ${item.menuName}$levelInfo$noteInfo"
            tvItemName.setTextColor(Color.parseColor("#424242"))
            tvItemName.textSize = 14f
            tvItemName.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val tvItemPrice = TextView(this)
            val totalItem = (item.price + item.extraCost) * item.quantity
            tvItemPrice.text = formatRupiah(totalItem)
            tvItemPrice.setTextColor(Color.parseColor("#757575"))
            tvItemPrice.textSize = 14f

            row.addView(tvItemName)
            row.addView(tvItemPrice)
            containerItems.addView(row)
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        btnBatal.setOnClickListener { dialog.dismiss() }
        btnKirimConfirm.setOnClickListener {
            dialog.dismiss()
            prosesKirimAPI(nama)
        }
    }

    private fun generateDeviceUUID(): String {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_uuid", null)

        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_uuid", id).apply()
        }
        return id ?: "unknown_device"
    }

    // --- KIRIM API CREATE ORDER ---
    private fun prosesKirimAPI(nama: String) {
        val itemsToOrder = checkoutList.map {
            OrderItemRequest(
                menuId = it.menuId,
                quantity = it.quantity,
                levelId = it.levelId,
                notes = it.notes
            )
        }

        val myDeviceId = generateDeviceUUID()

        val request = OrderRequest(
            meja = nomorMeja,
            customerName = nama,
            items = itemsToOrder,
            deviceId = myDeviceId
        )

        btnKirim.text = "Mengirim..."
        btnKirim.isEnabled = false

        ApiClient.instance.createOrder(request).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                btnKirim.text = "Konfirmasi Pesanan"
                btnKirim.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    val orderResponse = response.body()
                    if (orderResponse != null) {
                        showPaymentDialog(orderResponse)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    var errorMessage = "Gagal memproses pesanan."

                    if (errorBody != null) {
                        try {
                            val errorJson = org.json.JSONObject(errorBody)
                            errorMessage = errorJson.getString("message")
                        } catch (e: Exception) {
                            errorMessage = "Terjadi kesalahan server (${response.code()})"
                        }
                    }

                    AlertDialog.Builder(this@CheckoutActivity)
                        .setTitle("Gagal Order")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                btnKirim.text = "Konfirmasi Pesanan"
                btnKirim.isEnabled = true
                val msg = if (t is java.net.ConnectException) "Cek koneksi internet Anda." else t.message
                Toast.makeText(this@CheckoutActivity, "Error: $msg", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- LOGIC PEMBAYARAN  ---
    private fun showPaymentDialog(dataOrder: OrderResponse) {
        val dialogBuilder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.payment, null)

        val tvTotal = view.findViewById<TextView>(R.id.tvTotalBayarDialog)
        val rgMethod = view.findViewById<RadioGroup>(R.id.rgPaymentMethod)

        // Container PIN dan EditText
        val layoutPin = view.findViewById<LinearLayout>(R.id.layoutPinContainer)
        val etPin = view.findViewById<EditText>(R.id.etPinPayment)

        val btnBayar = view.findViewById<Button>(R.id.btnProsesBayar)
        val btnBatal = view.findViewById<TextView>(R.id.btnBatalBayar)

        val totalHarga = dataOrder.data?.finalTotal?.toDouble() ?: 0.0
        tvTotal.text = formatRupiah(totalHarga)

        dialogBuilder.setView(view)
        val dialog = dialogBuilder.create()
        dialog.setCancelable(false)

        // 1. LISTENER RADIO BUTTON
        rgMethod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbTunai) {
                // Pilih Tunai: Sembunyikan PIN, Ubah Tombol
                layoutPin.visibility = View.GONE
                btnBayar.text = "KONFIRMASI DI KASIR"
            } else {
                // Pilih Non-Tunai: Munculkan PIN
                layoutPin.visibility = View.VISIBLE
                btnBayar.text = "BAYAR SEKARANG"
            }
        }

        // 2. LOGIKA TOMBOL BAYAR
        btnBayar.setOnClickListener {
            val selectedId = rgMethod.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Pilih metode bayar dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // A. KASIR / TUNAI
            if (selectedId == R.id.rbTunai) {
                dialog.dismiss()
                // Pindah Receipt tanpa verifikasi PIN (Status tetap Pending)
                pindahKeReceipt(dataOrder)
            }
            // B. E-WALLET / QRIS
            else {
                val pin = etPin.text.toString()
                if (pin.isEmpty()) {
                    etPin.error = "Masukkan PIN dulu!"
                    return@setOnClickListener
                }

                if (pin == "123456") {
                    btnBayar.text = "Memproses..."
                    btnBayar.isEnabled = false
                    verifikasiPembayaran(dataOrder.data?.id ?: 0, dataOrder, dialog)
                } else {
                    etPin.error = "PIN Salah! Coba 123456"
                }
            }
        }

        // 3. LOGIKA TOMBOL BATAL
        btnBatal.setOnClickListener {
            dialog.dismiss()
            pindahKeReceipt(dataOrder)
        }

        dialog.show()
    }

    private fun verifikasiPembayaran(orderId: Int, originalResponse: OrderResponse, dialog: AlertDialog) {
        ApiClient.instance.payOrder(orderId).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                dialog.dismiss()
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@CheckoutActivity, "Pembayaran Lunas!", Toast.LENGTH_LONG).show()
                    val dataTerbaru = response.body()!!
                    pindahKeReceipt(dataTerbaru)
                } else {
                    Toast.makeText(this@CheckoutActivity, "Gagal Verifikasi: ${response.code()}", Toast.LENGTH_SHORT).show()
                    pindahKeReceipt(originalResponse)
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(this@CheckoutActivity, "Koneksi Error saat Bayar", Toast.LENGTH_SHORT).show()
                pindahKeReceipt(originalResponse)
            }
        })
    }

    private fun pindahKeReceipt(dataOrder: OrderResponse) {
        val intent = Intent(this@CheckoutActivity, ReceiptActivity::class.java)
        intent.putExtra("order_response", dataOrder)
        intent.putExtra("cart_list", ArrayList(checkoutList))
        intent.putExtra("meja", nomorMeja)
        startActivity(intent)
        finish()
    }
}