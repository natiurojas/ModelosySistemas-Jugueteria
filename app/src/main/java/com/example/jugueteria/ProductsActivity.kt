package com.example.jugueteria

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProductsActivity : AppCompatActivity() {

    private lateinit var recyclerProducts: RecyclerView
    private lateinit var firebase: FirebaseConnect

    // =================================
    // NAVEGACIÓN
    // =================================

    private lateinit var navHome: LinearLayout
    private lateinit var navSales: LinearLayout
    private lateinit var navProducts: LinearLayout
    private lateinit var navInvoice: LinearLayout

    private lateinit var btnAdd: ImageButton
    private lateinit var btnNewProduct: Button

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
        // FIREBASE
        // =================================

        firebase =
            FirebaseConnect(this)

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

        btnAdd =
            findViewById(R.id.btnAdd)
        btnNewProduct = findViewById(R.id.btnNewProduct)

        // =================================
        // RECYCLER
        // =================================

        recyclerProducts.layoutManager =
            LinearLayoutManager(this)

        // =================================
        // NAVEGACIÓN
        // =================================

        setupNavigation()

        // =================================
        // CARGAR PRODUCTOS
        // =================================

        cargarProductos()

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

    private fun cargarProductos() {

        firebase.getProducts { products, error ->

            if (error != null) {

                Toast.makeText(
                    this,
                    "Error cargando productos",
                    Toast.LENGTH_LONG
                ).show()

                return@getProducts
            }

            val adapter =
                ProductAdapter(

                    products = products,

                    // =========================
                    // VER
                    // =========================

                    onView = { product ->

                        // Acá después podemos abrir
                        // ProductDetailActivity
                    },

                    // =========================
                    // EDITAR
                    // =========================

                    onEdit = { product ->

                        val intent =
                            Intent(
                                this,
                                AddEditProductActivity::class.java
                            )

                        intent.putExtra(
                            "PRODUCT_ID",
                            product.id
                        )

                        startActivity(intent)
                    },

                    // =========================
                    // ELIMINAR
                    // =========================

                    onDelete = { product ->

                        eliminarProducto(product)
                    }
                )

            recyclerProducts.adapter =
                adapter
        }
    }

    // =====================================
    // ELIMINAR PRODUCTO
    // =====================================

    private fun eliminarProducto(
        product: Product
    ) {

        firebase.deleteProduct(
            product.id
        ) { success, error ->

            if (success) {

                Toast.makeText(
                    this,
                    "Producto eliminado",
                    Toast.LENGTH_SHORT
                ).show()

                cargarProductos()

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

        if (::firebase.isInitialized) {

            cargarProductos()
        }
    }
}