package com.example.jugueteria

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InvoiceDetailActivity : AppCompatActivity() {

    private lateinit var txtInvoiceNumber: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtClient: TextView
    private lateinit var txtTotal: TextView
    private lateinit var recyclerItems: RecyclerView

    private lateinit var firebase: FirebaseConnect

    private var sale: Sale? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_invoice_detail
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

        txtInvoiceNumber =
            findViewById(
                R.id.txtInvoiceNumber
            )

        txtDate =
            findViewById(
                R.id.txtDate
            )

        txtClient =
            findViewById(
                R.id.txtClient
            )

        txtTotal =
            findViewById(
                R.id.txtTotal
            )

        recyclerItems =
            findViewById(
                R.id.recyclerInvoiceItems
            )

        // =================================
        // OBTENER ID DE FIRESTORE
        // =================================

        val saleId =
            intent.getStringExtra(
                "SALE_ID"
            )

        if (saleId.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Factura no encontrada",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        // =================================
        // CONFIGURAR RECYCLER
        // =================================

        recyclerItems.layoutManager =
            LinearLayoutManager(this)

        // =================================
        // BUSCAR VENTA
        // =================================

        cargarFactura(saleId)

        // =================================
        // VOLVER
        // =================================

        findViewById<ImageButton>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }
    }

    // =====================================
    // CARGAR FACTURA
    // =====================================

    private fun cargarFactura(
        saleId: String
    ) {

        firebase.getSale(
            saleId
        ) { result, error ->

            if (error != null) {

                Toast.makeText(
                    this,
                    "Error cargando factura",
                    Toast.LENGTH_LONG
                ).show()

                finish()

                return@getSale
            }

            if (result == null) {

                Toast.makeText(
                    this,
                    "Factura no encontrada",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

                return@getSale
            }

            // Guardamos la venta
            sale = result

            // Mostramos la información
            showSale(result)
        }
    }

    // =====================================
    // MOSTRAR VENTA
    // =====================================

    private fun showSale(
        sale: Sale
    ) {

        // =================================
        // NÚMERO DE FACTURA
        // =================================

        txtInvoiceNumber.text =
            "Factura #${sale.id.takeLast(6)}"

        // =================================
        // FECHA
        // =================================

        val dateFormat =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            )

        txtDate.text =
            dateFormat.format(
                Date(sale.date)
            )

        // =================================
        // CLIENTE
        // =================================

        txtClient.text =
            sale.clientName

        // =================================
        // TOTAL
        // =================================

        txtTotal.text =
            "$ %.0f".format(
                sale.total
            )

        // =================================
        // RECYCLERVIEW
        // =================================

        recyclerItems.adapter =
            InvoiceDetailAdapter(
                sale.items
            )
    }

    // =====================================
    // GENERAR TEXTO DE FACTURA
    // =====================================

    private fun buildInvoiceText(
        sale: Sale
    ): String {

        val builder =
            StringBuilder()

        builder.append(
            "FACTURA #${sale.id.takeLast(6)}\n"
        )

        val dateFormat =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            )

        builder.append(
            "Fecha: ${
                dateFormat.format(
                    Date(sale.date)
                )
            }\n"
        )

        builder.append(
            "Cliente: ${sale.clientName}\n\n"
        )

        builder.append(
            "PRODUCTOS\n"
        )

        builder.append(
            "--------------------------\n"
        )

        for (item in sale.items) {

            builder.append(
                "${item.product.name}\n"
            )

            builder.append(
                "${item.quantity} x " +
                        "$ %.0f = $ %.0f\n"
                            .format(
                                item.product.price,
                                item.subtotal
                            )
            )

            builder.append("\n")
        }

        builder.append(
            "--------------------------\n"
        )

        builder.append(
            "TOTAL: $ %.0f\n"
                .format(
                    sale.total
                )
        )

        return builder.toString()
    }
}