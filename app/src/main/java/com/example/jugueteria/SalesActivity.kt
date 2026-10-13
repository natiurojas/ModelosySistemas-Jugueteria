package com.example.jugueteria

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SalesActivity : AppCompatActivity() {

    private lateinit var recyclerProducts: RecyclerView
    private lateinit var recyclerDetail: RecyclerView

    private lateinit var txtTotal: TextView
    private lateinit var txtDetail: TextView

    private val saleItems = mutableListOf<SaleItem>()
    private var availableProducts: List<Product> = emptyList()

    private lateinit var backend: DjangoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_sales)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // =================================
        // DJANGO
        // =================================

        backend = DjangoRepository(this)

        // =================================
        // RECYCLER PRODUCTOS
        // =================================

        recyclerProducts =
            findViewById(R.id.recyclerAvailableProducts)

        recyclerProducts.layoutManager =
            LinearLayoutManager(this)

        // =================================
        // RECYCLER DETALLE
        // =================================

        recyclerDetail =
            findViewById(R.id.recyclerSaleDetail)

        recyclerDetail.layoutManager =
            LinearLayoutManager(this)

        // =================================
        // TEXTOS
        // =================================

        txtTotal =
            findViewById(R.id.txtTotal)

        txtDetail =
            findViewById(R.id.txtDetail)

        // =================================
        // CARGAR PRODUCTOS DESDE DJANGO
        // =================================

        cargarProductos()

        // =================================
        // ADAPTER DETALLE
        // =================================

        recyclerDetail.adapter =
            SaleDetailAdapter(
                saleItems
            ) { item ->

                removeProduct(item)
            }

        // =================================
        // VOLVER
        // =================================

        findViewById<ImageButton>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }

        findViewById<Button>(R.id.btnBarcodeProduct).setOnClickListener {
            startActivityForResult(Intent(this, BarcodeProductActivity::class.java), 241)
        }

        // =================================
        // CONFIRMAR VENTA
        // =================================

        findViewById<Button>(
            R.id.btnConfirmSale
        ).setOnClickListener {

            confirmSale()
        }

        updateTotal()
    }

    // =====================================
    // CARGAR PRODUCTOS
    // =====================================

    private fun cargarProductos() {

        backend.getProducts { products, error ->

            if (error != null) {

                Toast.makeText(
                    this,
                    "Error cargando productos",
                    Toast.LENGTH_SHORT
                ).show()

                return@getProducts
            }

            availableProducts = products
            showFilteredProducts("")
            findViewById<EditText>(R.id.edtSearchProduct).addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { showFilteredProducts(s?.toString().orEmpty()) }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    @Deprecated("Deprecated in Android API; kept for compatibility with this project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 241 && resultCode == RESULT_OK && data != null) {
            val product = Product(
                id = data.getStringExtra("id") ?: return,
                imageRes = "", name = data.getStringExtra("name") ?: "Producto",
                description = data.getStringExtra("description") ?: "", category = "",
                price = data.getDoubleExtra("price", 0.0), stock = data.getIntExtra("stock", 0)
            )
            addProduct(product)
        }
    }

    private fun showFilteredProducts(query: String) {
        val filtered = availableProducts.filter { product ->
            product.name.contains(query, ignoreCase = true) ||
                product.description.contains(query, ignoreCase = true) ||
                product.id.contains(query, ignoreCase = true)
        }
        recyclerProducts.adapter = SaleProductAdapter(filtered) { product -> addProduct(product) }
    }

    // =====================================
    // AGREGAR PRODUCTO
    // =====================================

    private fun addProduct(product: Product) {

        val existingItem =
            saleItems.find {

                it.product.id == product.id
            }

        if (existingItem != null) {

            // Evitar superar el stock
            if (existingItem.quantity < product.stock) {

                existingItem.quantity++

            } else {

                Toast.makeText(
                    this,
                    "No hay más stock disponible",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else {

            if (product.stock <= 0) {

                Toast.makeText(
                    this,
                    "Este producto no tiene stock",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            saleItems.add(
                SaleItem(
                    product = product,
                    quantity = 1
                )
            )
        }

        recyclerDetail.adapter?.notifyDataSetChanged()

        updateTotal()
    }

    // =====================================
    // ELIMINAR PRODUCTO
    // =====================================

    private fun removeProduct(item: SaleItem) {

        saleItems.remove(item)

        recyclerDetail.adapter?.notifyDataSetChanged()

        updateTotal()
    }

    // =====================================
    // CALCULAR TOTAL
    // =====================================

    private fun updateTotal() {

        val total =
            saleItems.sumOf {

                it.subtotal
            }

        txtTotal.text =
            "$ %.0f".format(total)

        txtDetail.text =
            "Detalle (${saleItems.size})"
    }

    // =====================================
    // CONFIRMAR VENTA
    // =====================================

    private fun confirmSale() {

        // No hay productos
        if (saleItems.isEmpty()) {

            Toast.makeText(
                this,
                "Agregá al menos un producto",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // =================================
        // CALCULAR TOTAL
        // =================================

        val total =
            saleItems.sumOf {

                it.subtotal
            }

        // =================================
        // CREAR VENTA
        // =================================

        val sale = Sale(

            // Django devuelve el ID de la factura creada
            id = "",

            // Usuario actualmente logueado
            userId = backend.getCurrentUserId() ?: "",

            clientName = "",

            // Fecha como timestamp
            date = System.currentTimeMillis(),

            // Copiamos los productos vendidos
            items = saleItems.map {

                it.copy()
            },

            total = total
        )

        // =================================
        // GUARDAR EN FIRESTORE
        // =================================

        backend.addSale(sale) { success, saleId, error ->

            if (success) {

                Toast.makeText(
                    this,
                    "Venta registrada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                // Ir a facturas
                val intent =
                    Intent(
                        this,
                        InvoicesActivity::class.java
                    )

                startActivity(intent)

                finish()

            } else {

                Toast.makeText(
                    this,
                    "Error guardando la venta: ${error?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}