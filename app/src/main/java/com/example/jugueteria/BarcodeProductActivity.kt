package com.example.jugueteria

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeProductActivity : AppCompatActivity() {

    private lateinit var codeInput: EditText
    private lateinit var resultText: TextView
    private lateinit var scannerContainer: View
    private lateinit var previewView: PreviewView
    private lateinit var scanStatus: TextView
    private lateinit var toggleScanButton: Button
    private lateinit var findButton: Button

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null
    private var scanning = false
    private var lookingUp = false
    private val codeDetected = AtomicBoolean(false)

    private val barcodeScanner: BarcodeScanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startScanner()
            } else {
                scanStatus.text = "Permiso de cámara denegado. Ingresá el código manualmente."
                Toast.makeText(this, "Se necesita permiso de cámara para escanear", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_product)
        ApiClient.restore(this)

        codeInput = findViewById(R.id.edtBarcode)
        resultText = findViewById(R.id.txtBarcodeResult)
        scannerContainer = findViewById(R.id.scannerContainer)
        previewView = findViewById(R.id.previewView)
        scanStatus = findViewById(R.id.txtScanStatus)
        toggleScanButton = findViewById(R.id.btnToggleScan)
        findButton = findViewById(R.id.btnFindBarcode)

        toggleScanButton.setOnClickListener {
            if (scanning) stopScanner() else requestCamera()
        }
        findButton.setOnClickListener {
            searchProduct(codeInput.text.toString().trim())
        }
        findViewById<ImageButton>(R.id.btnBarcodeBack).setOnClickListener { finish() }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            toggleScanButton.isEnabled = false
            scanStatus.text = "Este dispositivo no tiene cámara. Ingresá el código manualmente."
        }
    }

    private fun requestCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> startScanner()

            else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanner() {
        scanStatus.text = "Iniciando cámara…"
        val executor = cameraExecutor ?: Executors.newSingleThreadExecutor().also { cameraExecutor = it }
        val cameraFuture = ProcessCameraProvider.getInstance(this)
        cameraFuture.addListener({
            if (isDestroyed) return@addListener
            try {
                bindScanner(cameraFuture.get(), executor)
            } catch (e: Exception) {
                scanning = false
                toggleScanButton.text = "Activar cámara"
                scanStatus.text = "No se pudo abrir la cámara: ${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindScanner(provider: ProcessCameraProvider, executor: ExecutorService) {
        cameraProvider = provider
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor, BarcodeAnalyzer(barcodeScanner) { value ->
            runOnUiThread { onBarcodeDetected(value) }
        })

        val cameraSelector = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        provider.unbindAll()
        provider.bindToLifecycle(this, cameraSelector, preview, analysis)

        codeDetected.set(false)
        scanning = true
        scannerContainer.visibility = View.VISIBLE
        toggleScanButton.text = "Detener escaneo"
        scanStatus.text = "Escaneando… apuntá al código de barras"
    }

    private fun stopScanner() {
        cameraProvider?.unbindAll()
        codeDetected.set(true)
        scanning = false
        scannerContainer.visibility = View.GONE
        toggleScanButton.text = "Activar cámara"
    }

    private fun onBarcodeDetected(value: String) {
        stopScanner()
        codeInput.setText(value)
        resultText.text = "Código detectado: $value\nBuscando producto…"
        Toast.makeText(this, "Código detectado: $value", Toast.LENGTH_SHORT).show()
        searchProduct(value)
    }

    private fun searchProduct(value: String) {
        if (value.isBlank()) {
            codeInput.error = "Ingresá o escaneá un código"
            return
        }
        if (lookingUp) return

        lookingUp = true
        findButton.isEnabled = false
        resultText.text = "Buscando producto…"

        ApiClient.api.productByBarcode(value).enqueue(object : Callback<ApiProduct> {
            override fun onResponse(call: Call<ApiProduct>, response: Response<ApiProduct>) {
                lookingUp = false
                findButton.isEnabled = true
                val product = response.body()
                if (response.isSuccessful && product != null) {
                    resultText.text = "${product.nombre}\nPrecio: $ ${product.precio}\nStock: ${product.stock}\nID: ${product.id}"
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("id", product.id.toString())
                        putExtra("name", product.nombre)
                        putExtra("description", product.descripcion)
                        putExtra("price", product.precio)
                        putExtra("stock", product.stock)
                    })
                    Toast.makeText(this@BarcodeProductActivity, "Producto cargado para la venta", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    resultText.text = "No se encontró un producto con ese código."
                    scanStatus.text = "No se encontró el producto. Escaneá otro código o probá manualmente."
                }
            }

            override fun onFailure(call: Call<ApiProduct>, t: Throwable) {
                lookingUp = false
                findButton.isEnabled = true
                resultText.text = "No se pudo conectar: ${t.localizedMessage}"
            }
        })
    }

    override fun onDestroy() {
        stopScanner()
        cameraExecutor?.shutdown()
        barcodeScanner.close()
        super.onDestroy()
    }

    private inner class BarcodeAnalyzer(
        private val scanner: BarcodeScanner,
        private val onDetected: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage == null || codeDetected.get()) {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val value = barcodes.mapNotNull { it.rawValue?.takeIf { code -> code.isNotBlank() } }.firstOrNull()
                    if (value != null && codeDetected.compareAndSet(false, true)) {
                        onDetected(value)
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        }
    }
}
