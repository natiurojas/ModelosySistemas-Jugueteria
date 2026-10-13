package com.example.jugueteria

import android.content.Intent
import android.os.Bundle
import android.view.View
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
        // DJANGO
        // =================================

        backend =
            DjangoRepository(this)

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
        navAdmin =
            findViewById(R.id.navAdmin)

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
    // CARGAR FACTURAS DESDE FIRESTORE
    // =====================================

    private fun cargarFacturas() {

        backend.getSales { sales, error ->

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

        if (::backend.isInitialized) {

            cargarFacturas()
        }
    }
}