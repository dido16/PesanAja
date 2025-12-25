package com.example.pesanaja

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private var isScanned = false

    // Variabel untuk mengontrol kamera dan scanner secara global
    private var cameraProvider: ProcessCameraProvider? = null
    private val scanner = BarcodeScanning.getClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        previewView = findViewById(R.id.previewView)

        // --- TAMBAHAN: LOGIC ANIMASI LASER ---
        // Kita cari ID garis merah yang ada di XML tadi
        val scanLine = findViewById<ImageView>(R.id.ivScanLine)

        // Bikin animasi gerak vertikal (translationY) dari -300 ke 300
        val animation = ObjectAnimator.ofFloat(scanLine, "translationY", -300f, 300f)
        animation.duration = 2000 // Durasi 2 detik sekali jalan
        animation.repeatMode = ValueAnimator.REVERSE // Bolak-balik (naik-turun)
        animation.repeatCount = ValueAnimator.INFINITE // Gak berhenti-berhenti
        animation.start()
        // -------------------------------------

        // Cek izin kamera saat aplikasi dibuka
        if (hasCameraPermission()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    // --- FUNGSI IZIN KAMERA ---
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Izin kamera ditolak. Tidak bisa scan QR!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // --- FUNGSI KAMERA ---
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Preview: Menampilkan apa yang dilihat kamera ke layar
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Analysis: Mengambil gambar per frame untuk dibaca QR-nya
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analyzer.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                    processImageProxy(imageProxy)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Lepaskan kamera dari session sebelumnya agar tidak bentrok
                cameraProvider?.unbindAll()

                // Hubungkan kamera ke lifecycle Activity ini
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, analyzer)

            } catch (e: Exception) {
                Log.e("SCAN_ERROR", "Gagal memulai kamera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        // Jika sudah berhasil scan satu kali, abaikan frame berikutnya
        if (isScanned) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val value = barcode.rawValue ?: continue

                    isScanned = true

                    // Matikan kamera segera agar tidak BufferQueue Error
                    cameraProvider?.unbindAll()

                    // Pindah ke MenuActivity membawa data nomor meja
                    val i = Intent(this, MenuActivity::class.java)
                    i.putExtra("meja", value)
                    startActivity(i)
                    finish()
                    break
                }
            }
            .addOnFailureListener {
                Log.e("SCAN_ERROR", "Gagal scan: ${it.message}")
            }
            .addOnCompleteListener {
                // Wajib ditutup agar frame berikutnya bisa masuk
                imageProxy.close()
            }
    }

    // Membersihkan resource kamera saat Activity ditutup
    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        scanner.close()
    }
}