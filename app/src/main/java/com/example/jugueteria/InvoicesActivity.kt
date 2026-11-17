package com.example.jugueteria

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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

    private lateinit var txtSelectedPeriod: TextView
    private lateinit var btnPickDate: Button
    private var salesPeriod: String? = null
    private var salesFilterValue: String? = null
    private var salesRequestId = 0

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
        // FILTROS Y CARGA INICIAL
        // =================================

        setupFilters()
        // La carga la hace onResume (se ejecuta justo después de onCreate).

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

    private fun setupFilters() {

        txtSelectedPeriod = findViewById(R.id.txtSelectedPeriod)
        btnPickDate = findViewById(R.id.btnPickDate)

        findViewById<Button>(R.id.btnFilterDay).setOnClickListener { setPeriod("dia") }
        findViewById<Button>(R.id.btnFilterMonth).setOnClickListener { setPeriod("mes") }
        findViewById<Button>(R.id.btnFilterYear).setOnClickListener { setPeriod("anio") }
        findViewById<Button>(R.id.btnFilterAll).setOnClickListener { setPeriod(null) }
        btnPickDate.setOnClickListener { showDatePicker() }
    }

    private fun setPeriod(period: String?) {

        salesPeriod = period
        if (period == null) {
            salesFilterValue = null
            txtSelectedPeriod.text = "Sin filtro"
            btnPickDate.visibility = View.GONE
        } else {
            val today = LocalDate.now()
            salesFilterValue = when (period) {
                "dia" -> today.toString()
                "mes" -> YearMonth.from(today).toString()
                else -> today.year.toString()
            }
            updatePeriodLabel()
            btnPickDate.visibility = View.VISIBLE
        }
        cargarFacturas()
    }

    private fun updatePeriodLabel() {
        val value = salesFilterValue ?: return
        txtSelectedPeriod.text = when (salesPeriod) {
            "dia" -> "Día: " + LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            "mes" -> "Mes: " + YearMonth.parse(value).format(DateTimeFormatter.ofPattern("MM/yyyy"))
            "anio" -> "Año: $value"
            else -> "Sin filtro"
        }
    }

    private fun showDatePicker() {
        val initial = when (salesPeriod) {
            "dia" -> salesFilterValue?.let { LocalDate.parse(it) } ?: LocalDate.now()
            "mes" -> salesFilterValue?.let { YearMonth.parse(it).atDay(1) } ?: LocalDate.now().withDayOfMonth(1)
            "anio" -> salesFilterValue?.let { LocalDate.of(it.toInt(), 1, 1) } ?: LocalDate.now().withDayOfYear(1)
            else -> LocalDate.now()
        }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = LocalDate.of(year, month + 1, dayOfMonth)
                salesFilterValue = when (salesPeriod) {
                    "dia" -> date.toString()
                    "mes" -> YearMonth.from(date).toString()
                    "anio" -> date.year.toString()
                    else -> null
                }
                updatePeriodLabel()
                cargarFacturas()
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    private fun cargarFacturas() {

        val requestId = ++salesRequestId

        backend.getSalesFiltered(salesPeriod, salesFilterValue) { sales, error ->

            if (requestId != salesRequestId) return@getSalesFiltered

            if (error != null) {

                Toast.makeText(
                    this,
                    "Error cargando facturas",
                    Toast.LENGTH_LONG
                ).show()

                return@getSalesFiltered
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