package com.example.jugueteria

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

class InvoicesActivity : AppCompatActivity() {

    private lateinit var recyclerInvoices: RecyclerView
    private lateinit var txtInvoiceCount: TextView
    private lateinit var adapter: InvoiceAdapter

    private lateinit var firebase: FirebaseConnect

    // =================================
    // NAVEGACIÓN
    // =================================

    private lateinit var navHome: LinearLayout
    private lateinit var navSales: LinearLayout
    private lateinit var navProducts: LinearLayout
    private lateinit var navInvoice: LinearLayout

    private lateinit var btnAdd: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_invoices
        )

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

        recyclerInvoices =
            findViewById(R.id.recyclerInvoices)

        txtInvoiceCount =
            findViewById(R.id.txtInvoiceCount)

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

        // =================================
        // RECYCLER
        // =================================

        recyclerInvoices.layoutManager =
            LinearLayoutManager(this)

        // =================================
        // ADAPTER
        // =================================

        adapter =
            InvoiceAdapter(
                emptyList()
            ) { sale ->

                openInvoice(sale)
            }

        recyclerInvoices.adapter =
            adapter

        // =================================
        // NAVEGACIÓN
        // =================================

        setupNavigation()

        // =================================
        // CARGAR FACTURAS
        // =================================

        cargarFacturas()

        // =================================
        // NUEVA VENTA
        // =================================

        findViewById<Button>(
            R.id.btnNewSale
        ).setOnClickListener {

            openActivity(
                SalesActivity::class.java
            )
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

            openActivity(
                ProductsActivity::class.java
            )
            finish()
        }

        // ---------------------------------
        // FACTURAS
        // ---------------------------------

        navInvoice.setOnClickListener {

            // Ya estamos en Facturas
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
    // CARGAR FACTURAS DESDE FIRESTORE
    // =====================================

    private fun cargarFacturas() {

        firebase.getSales { sales, error ->

            if (error != null) {

                Toast.makeText(
                    this,
                    "Error cargando facturas",
                    Toast.LENGTH_LONG
                ).show()

                return@getSales
            }

            adapter.updateList(
                sales
            )

            updateInvoiceCount(
                sales.size
            )
        }
    }

    // =====================================
    // CONTADOR
    // =====================================

    private fun updateInvoiceCount(
        count: Int
    ) {

        txtInvoiceCount.text =
            "$count facturas"
    }

    // =====================================
    // ABRIR DETALLE DE FACTURA
    // =====================================

    private fun openInvoice(
        sale: Sale
    ) {

        val intent =
            Intent(
                this,
                InvoiceDetailActivity::class.java
            )

        intent.putExtra(
            "SALE_ID",
            sale.id
        )

        startActivity(intent)
    }

    // =====================================
    // ACTUALIZAR AL VOLVER
    // =====================================

    override fun onResume() {

        super.onResume()

        if (::firebase.isInitialized) {

            cargarFacturas()
        }
    }
}