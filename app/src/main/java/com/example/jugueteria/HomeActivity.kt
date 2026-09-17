package com.example.jugueteria

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var firebase: FirebaseConnect

    private lateinit var txtWelcome: TextView
    private lateinit var txtDate: TextView

    private lateinit var txtSales: TextView
    private lateinit var txtInvoices: TextView
    private lateinit var txtProducts: TextView
    private lateinit var txtClients: TextView

    private lateinit var navHome: LinearLayout
    private lateinit var navSales: LinearLayout
    private lateinit var navProducts: LinearLayout
    private lateinit var navInvoice: LinearLayout

    private lateinit var btnAdd: ImageButton
    private lateinit var btnMenu: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_home
        )

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

        txtWelcome =
            findViewById(R.id.txtWelcome)

        txtDate =
            findViewById(R.id.txtDate)

        txtSales =
            findViewById(R.id.txtSales)

        txtInvoices =
            findViewById(R.id.txtInvoices)

        txtProducts =
            findViewById(R.id.txtProducts)

        txtClients =
            findViewById(R.id.txtClients)

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

        btnMenu =
            findViewById(R.id.btnMenu)

        // =================================
        // FECHA
        // =================================

        showCurrentDate()

        // =================================
        // USUARIO
        // =================================

        showCurrentUser()

        // =================================
        // NAVEGACIÓN
        // =================================

        setupNavigation()

        // =================================
        // CARGAR ESTADÍSTICAS
        // =================================

        loadDashboard()
    }

    // =====================================
    // FECHA ACTUAL
    // =====================================

    private fun showCurrentDate() {

        val formatter =
            SimpleDateFormat(
                "dd 'de' MMMM 'de' yyyy",
                Locale("es", "ES")
            )

        txtDate.text =
            formatter.format(
                Calendar.getInstance().time
            )
    }

    // =====================================
    // USUARIO ACTUAL
    // =====================================

    private fun showCurrentUser() {

        val user =
            firebase.getCurrentUser()

        val name =
            user?.displayName
                ?: user?.email
                    ?.substringBefore("@")
                ?: "Usuario"

        txtWelcome.text =
            "¡Bienvenido, $name!"
    }

    // =====================================
    // NAVEGACIÓN
    // =====================================

    private fun setupNavigation() {

        // Inicio
        navHome.setOnClickListener {

            // Ya estamos en Home
        }

        // Ventas
        navSales.setOnClickListener {

            openActivity(
                SalesActivity::class.java
            )
        }

        // Productos
        navProducts.setOnClickListener {

            openActivity(
                ProductsActivity::class.java
            )
            finish()
        }

        // Facturas
        navInvoice.setOnClickListener {

            openActivity(
                InvoicesActivity::class.java
            )
            finish()
        }

        // Botón +
        btnAdd.setOnClickListener {

            openActivity(
                SalesActivity::class.java
            )
        }

        // Menú
        btnMenu.setOnClickListener {

            showMenu()
        }

        // Cards
        findViewById<androidx.cardview.widget.CardView>(
            R.id.cardSales
        ).setOnClickListener {

            openActivity(
                SalesActivity::class.java
            )
        }

        findViewById<androidx.cardview.widget.CardView>(
            R.id.cardInvoices
        ).setOnClickListener {

            openActivity(
                InvoicesActivity::class.java
            )
            finish()
        }

        findViewById<androidx.cardview.widget.CardView>(
            R.id.cardProducts
        ).setOnClickListener {

            openActivity(
                ProductsActivity::class.java
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
    // CARGAR DASHBOARD
    // =====================================

    private fun loadDashboard() {

        loadSalesToday()

        loadProductsCount()

        loadClientsCount()
    }

    // =====================================
    // VENTAS DEL DÍA
    // =====================================

    private fun loadSalesToday() {

        firebase.getSales { sales, error ->

            if (error != null) {

                txtSales.text = "$ 0"
                txtInvoices.text = "0"

                return@getSales
            }

            // =================================
            // INICIO DEL DÍA
            // =================================

            val startOfDay =
                Calendar.getInstance().apply {

                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                }.timeInMillis

            // =================================
            // FIN DEL DÍA
            // =================================

            val endOfDay =
                Calendar.getInstance().apply {

                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)

                }.timeInMillis

            // =================================
            // FILTRAR VENTAS DE HOY
            // =================================

            val todaySales =
                sales.filter { sale ->

                    sale.date in startOfDay..endOfDay
                }

            // =================================
            // TOTAL VENDIDO
            // =================================

            val total =
                todaySales.sumOf {
                    it.total
                }

            // =================================
            // CANTIDAD DE FACTURAS
            // =================================

            val invoiceCount =
                todaySales.size

            // =================================
            // MOSTRAR
            // =================================

            txtSales.text =
                "$ %.0f".format(total)

            txtInvoices.text =
                invoiceCount.toString()
        }
    }

    // =====================================
    // CANTIDAD DE PRODUCTOS
    // =====================================

    private fun loadProductsCount() {

        firebase.getProducts { products, error ->

            if (error != null) {

                txtProducts.text =
                    "0"

                return@getProducts
            }

            txtProducts.text =
                products.size.toString()
        }
    }

    // =====================================
    // CANTIDAD DE CLIENTES
    // =====================================

    private fun loadClientsCount() {

        firebase.getClients { clients, error ->

            if (error != null) {

                txtClients.text =
                    "0"

                return@getClients
            }

            txtClients.text =
                clients.size.toString()
        }
    }

    // =====================================
    // MENÚ
    // =====================================

    private fun showMenu() {

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Menú")
            .setItems(
                arrayOf(
                    "Cerrar sesión"
                )
            ) { _, which ->

                when (which) {

                    0 -> logout()
                }
            }
            .show()
    }

    // =====================================
    // CERRAR SESIÓN
    // =====================================

    private fun logout() {

        firebase.logout()

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }

    // =====================================
    // ACTUALIZAR AL VOLVER
    // =====================================

    override fun onResume() {

        super.onResume()

        if (::firebase.isInitialized) {

            showCurrentUser()

            showCurrentDate()

            loadDashboard()
        }
    }
}