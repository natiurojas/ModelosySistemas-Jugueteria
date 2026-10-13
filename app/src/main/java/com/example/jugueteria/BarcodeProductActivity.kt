package com.example.jugueteria

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BarcodeProductActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_product)
        ApiClient.restore(this)
        val code = findViewById<EditText>(R.id.edtBarcode)
        val result = findViewById<TextView>(R.id.txtBarcodeResult)
        findViewById<Button>(R.id.btnFindBarcode).setOnClickListener {
            val value = code.text.toString().trim()
            if (value.isBlank()) { code.error = "Ingresá o escaneá un código"; return@setOnClickListener }
            findViewById<Button>(R.id.btnFindBarcode).isEnabled = false
            ApiClient.api.productByBarcode(value).enqueue(object : Callback<ApiProduct> {
                override fun onResponse(call: Call<ApiProduct>, response: Response<ApiProduct>) {
                    findViewById<Button>(R.id.btnFindBarcode).isEnabled = true
                    val p = response.body()
                    if (response.isSuccessful && p != null) {
                        result.text = "${p.nombre}\nPrecio: $ ${p.precio}\nStock: ${p.stock}\nID: ${p.id}"
                        setResult(RESULT_OK, android.content.Intent().apply {
                            putExtra("id", p.id.toString()); putExtra("name", p.nombre)
                            putExtra("description", p.descripcion); putExtra("price", p.precio); putExtra("stock", p.stock)
                        })
                        Toast.makeText(this@BarcodeProductActivity, "Producto cargado para la venta", Toast.LENGTH_SHORT).show()
                        finish()
                    } else result.text = "No se encontró un producto con ese código."
                }
                override fun onFailure(call: Call<ApiProduct>, t: Throwable) {
                    findViewById<Button>(R.id.btnFindBarcode).isEnabled = true
                    result.text = "No se pudo conectar: ${t.localizedMessage}"
                }
            })
        }
        findViewById<Button>(R.id.btnBarcodeBack).setOnClickListener { finish() }
    }
}
