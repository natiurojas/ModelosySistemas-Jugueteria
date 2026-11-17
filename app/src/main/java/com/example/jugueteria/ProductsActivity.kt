package com.example.jugueteria

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProductsActivity : AppCompatActivity() {

    private lateinit var recyclerProducts: RecyclerView
    private lateinit var backend: DjangoRepository

    // =================================
    // NAVEGACIÓN
    // =================================

    private lateinit var navHome: LinearLayout
    private lateinit var navSales: LinearLayout
    private lateinit var navProducts: LinearLayout
    private lateinit var navInvoice: LinearLayout
    private lateinit var navAdmin: LinearLayout

    private lateinit var btnAdd: ImageButton
    private lateinit var btnNewProduct: Button
    private lateinit var txtProductsPage: TextView
    private lateinit var btnLoadMoreProducts: Button

    // Listado por páginas: en memoria queda como máximo lo que se fue mostrando.
    private val loadedProducts = mutableListOf<Product>()
    private lateinit var productAdapter: ProductAdapter
    private var currentSearch = ""
    private var totalProducts = 0
    private var productRequestId = 0
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_products)

        // =================================
        // INSETS
        // =================================

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

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

        backend =
            DjangoRepository(this)

        // =================================
        // VIEWS
        // =================================

        recyclerProducts =
            findViewById(R.id.recyclerProducts)

        navHome =
            findViewById(R.id.navHome)

        navSales =
            findViewById(R.id.navSales)

        navProducts =
            findViewById(R.id.navProducts)

        navInvoice =
            findViewById(R.id.navInvoice)

        navAdmin =
            findViewById(R.id.navAdmin)

        btnAdd =
            findViewById(R.id.btnAdd)
        btnNewProduct = findViewById(R.id.btnNewProduct)
        txtProductsPage = findViewById(R.id.txtProductsPage)
        btnLoadMoreProducts = findViewById(R.id.btnLoadMoreProducts)

        // =================================
        // RECYCLER
        // =================================

        recyclerProducts.layoutManager =
            LinearLayoutManager(this)
        setupProductAdapter()

        btnLoadMoreProducts.setOnClickListener {
            cargarProductos(reset = false)
        }

        findViewById<EditText>(R.id.edtSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s?.toString().orEmpty()
                searchRunnable = Runnable {
                    currentSearch = query
                    cargarProductos(reset = true)
                }.also { searchHandler.postDelayed(it, 300L) }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // =================================
        // NAVEGACIÓN
        // =================================

        setupNavigation()

        // La carga inicial la hace onResume (que se ejecuta justo después de onCreate)
        // para no pedir dos veces la misma página al abrir la pantalla.

        btnNewProduct.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AddEditProductActivity::class.java
                )

            startActivity(intent)
        }
    }

    // =====================================
    // NAVEGACIÓN
    // =====================================

    private fun setupNavigation() {

        // ---------------------------------
        // INICIO
        // ---------------------------------

        navHome.setOnClickListener {

            openActivity(
                HomeActivity::class.java
            )
            finish()
        }

        // ---------------------------------
        // VENTAS
        // ---------------------------------

        navSales.setOnClickListener {

            openActivity(
                SalesActivity::class.java
            )
        }

        // ---------------------------------
        // BOTÓN +
        // ---------------------------------

        btnAdd.setOnClickListener {

            openActivity(
                SalesActivity::class.java
            )
        }

        // ---------------------------------
        // PRODUCTOS
        // ---------------------------------

        navProducts.setOnClickListener {

            // Ya estamos en Productos
        }

        // ---------------------------------
        // FACTURAS
        // ---------------------------------

        navInvoice.setOnClickListener {

            openActivity(
                InvoicesActivity::class.java
            )
            finish()
        }

        navAdmin.setOnClickListener {
            openActivity(
                AdminActivity::class.java
            )
            finish()
        }
        if(!isAdmin()) {
            navAdmin.visibility = View.GONE
        }
    }

    private fun isAdmin(): Boolean {
        val prefs = getSharedPreferences("jugueteria", MODE_PRIVATE)
        val role = prefs.getString("role", "cajero")
        return role == "administrador"
    }

    // =====================================
    // ABRIR ACTIVITY
    // =====================================

    private fun openActivity(
        activityClass: Class<*>
    ) {

        val intent =
            Intent(
                this,
                activityClass
            )

        startActivity(intent)
    }

    // =====================================
    // CARGAR PRODUCTOS
    // =====================================

    private fun setupProductAdapter() {
        productAdapter = ProductAdapter(
            products = loadedProducts,
            onView = { /* Acá después podemos abrir ProductDetailActivity */ },
            onEdit = { product ->
                val intent = Intent(this, AddEditProductActivity::class.java)
                intent.putExtra("PRODUCT_ID", product.id)
                startActivity(intent)
            },
            onDelete = { product -> eliminarProducto(product) }
        )
        recyclerProducts.adapter = productAdapter
    }

    private fun cargarProductos(reset: Boolean = true) {

        val requestId = ++productRequestId
        val offset = if (reset) 0 else loadedProducts.size

        backend.getProductsPage(currentSearch, offset) { products, total, error ->

            if (requestId != productRequestId) return@getProductsPage

            if (error != null) {

                Toast.makeText(
                    this,
                    "Error cargando productos",
                    Toast.LENGTH_LONG
                ).show()

                return@getProductsPage
            }

            if (reset) loadedProducts.clear()
            loadedProducts.addAll(products)
            totalProducts = total
            productAdapter.notifyDataSetChanged()
            updatePaginationBar()
        }
    }

    private fun updatePaginationBar() {
        txtProductsPage.text = "Mostrando ${loadedProducts.size} de $totalProducts"
        btnLoadMoreProducts.visibility =
            if (loadedProducts.size < totalProducts) View.VISIBLE else View.GONE
    }

    // =====================================
    // ELIMINAR PRODUCTO
    // =====================================

    private fun eliminarProducto(
        product: Product
    ) {

        backend.deleteProduct(
            product.id
        ) { success, error ->

            if (success) {

                Toast.makeText(
                    this,
                    "Producto eliminado",
                    Toast.LENGTH_SHORT
                ).show()

                cargarProductos(reset = true)

            } else {

                Toast.makeText(
                    this,
                    "Error eliminando producto",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =====================================
    // ACTUALIZAR AL VOLVER
    // =====================================

    override fun onResume() {

        super.onResume()

        if (::backend.isInitialized) {

            cargarProductos()
        }
    }
}