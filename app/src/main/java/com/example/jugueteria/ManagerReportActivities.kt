package com.example.jugueteria

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

abstract class ManagerReportBaseActivity : AppCompatActivity() {
    protected abstract val reportType: String
    protected abstract val screenTitle: String
    private val token get() = "Token " + (getSharedPreferences("jugueteria", MODE_PRIVATE).getString("token", "") ?: "")
    private lateinit var summary: TextView
    private lateinit var list: RecyclerView
    private val money = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.restore(this)
        setContentView(R.layout.activity_manager_list)
        findViewById<TextView>(R.id.txtManagerListTitle).text = screenTitle
        findViewById<ImageButton>(R.id.btnManagerListBack).setOnClickListener { finish() }
        summary = findViewById(R.id.txtManagerSummary)
        list = findViewById(R.id.recyclerManagerRecords)
        list.layoutManager = LinearLayoutManager(this)
        if (reportType == "recaudacion" || reportType == "ganancia") {
            findViewById<TextView>(R.id.btnManagerDate).visibility = View.VISIBLE
            findViewById<TextView>(R.id.btnManagerDate).setOnClickListener { chooseDate() }
            chooseDate()
        } else {
            findViewById<TextView>(R.id.btnManagerDate).visibility = View.GONE
            load(null)
        }
    }
    private fun chooseDate() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d -> load(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)) },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
    private fun load(date: String?) {
        summary.text = "Generando informe..."
        ApiClient.api.managerReport(token, reportType, date).enqueue(object : retrofit2.Callback<ManagerReport> {
            override fun onResponse(call: retrofit2.Call<ManagerReport>, response: retrofit2.Response<ManagerReport>) {
                val r = response.body()
                if (!response.isSuccessful || r == null) {
                    summary.text = "No se pudo generar el informe (${response.code()}). ${r?.detail.orEmpty()}"
                    list.adapter = ManagerRecordAdapter(emptyList()); return
                }
                val rows = r.resultados.map { item ->
                    when (reportType) {
                        "cajas" -> ManagerRecord("Venta #${item["venta_id"] ?: "—"}",
                            "${item["fecha"] ?: ""} · ${item["caja"] ?: "Caja"} · ${item["usuario"] ?: ""}",
                            money.format((item["total"] as? Number)?.toDouble() ?: 0.0))
                        else -> ManagerRecord(item["producto"]?.toString() ?: "Producto",
                            "Cantidad: ${item["cantidad"] ?: 0}",
                            money.format((item["total_vendido"] as? Number)?.toDouble() ?: 0.0))
                    }
                }
                summary.text = when (reportType) {
                    "cajas" -> "Ventas encontradas: ${r.cantidad ?: rows.size}"
                    "productos" -> "Productos en el histórico: ${r.cantidad ?: rows.size}"
                    "recaudacion" -> "Fecha: ${r.fecha ?: date}\nVentas: ${r.ventas ?: 0}\nTotal recaudado: ${money.format(r.recaudacion ?: 0.0)}"
                    else -> "Fecha: ${r.fecha ?: date}\nVentas: ${r.ventas ?: 0}\nGanancia estimada: ${money.format(r.ganancia_estimada ?: 0.0)}"
                }
                list.adapter = ManagerRecordAdapter(rows)
                if (rows.isEmpty()) Toast.makeText(this@ManagerReportBaseActivity, "No hay registros para mostrar", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: retrofit2.Call<ManagerReport>, t: Throwable) {
                summary.text = "Error de conexión: ${t.message}"
            }
        })
    }
}
class ManagerCashHistoryActivity : ManagerReportBaseActivity() {
    override val reportType = "cajas"
    override val screenTitle = "Histórico de ventas de caja"
}
class ManagerProductHistoryActivity : ManagerReportBaseActivity() {
    override val reportType = "productos"
    override val screenTitle = "Histórico de venta de productos"
}
class ManagerRevenueActivity : ManagerReportBaseActivity() {
    override val reportType = "recaudacion"
    override val screenTitle = "Recaudación del día"
}
class ManagerProfitActivity : ManagerReportBaseActivity() {
    override val reportType = "ganancia"
    override val screenTitle = "Ganancia del día"
}
