package com.example.jugueteria

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ManagerSettingsActivity : AppCompatActivity() {
    private val token get() = "Token " + (getSharedPreferences("jugueteria", MODE_PRIVATE).getString("token", "") ?: "")
    private var branches: List<ManagerBranchConfig> = emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.restore(this)
        setContentView(R.layout.activity_manager_settings)
        findViewById<ImageButton>(R.id.btnManagerSettingsBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnLoadManagerSettings).setOnClickListener { loadConfig() }
        loadConfig()
    }
    private fun loadConfig() {
        ApiClient.api.managerConfig(token).enqueue(object : retrofit2.Callback<List<ManagerBranchConfig>> {
            override fun onResponse(call: retrofit2.Call<List<ManagerBranchConfig>>, response: retrofit2.Response<List<ManagerBranchConfig>>) {
                if (!response.isSuccessful) { toast("No se pudo cargar la configuración (${response.code()})"); return }
                branches = response.body().orEmpty()
                if (branches.isEmpty()) { toast("No hay sucursales registradas"); return }
                AlertDialog.Builder(this@ManagerSettingsActivity).setTitle("Seleccioná una sucursal")
                    .setItems(branches.map { it.sucursal }.toTypedArray()) { _, i -> showBranch(branches[i]) }.show()
            }
            override fun onFailure(call: retrofit2.Call<List<ManagerBranchConfig>>, t: Throwable) { toast("Error: ${t.message}") }
        })
    }
    private fun showBranch(b: ManagerBranchConfig) {
        AlertDialog.Builder(this).setTitle(b.sucursal)
            .setItems(arrayOf("Cambiar hora límite de ventas", "Configurar apertura y cierre de caja")) { _, choice ->
                if (choice == 0) pickTime(b.hora_limite_ventas, "Hora límite de ventas") {
                    save(mapOf("sucursal_id" to b.sucursal_id, "hora_limite_ventas" to it))
                } else chooseCashbox(b)
            }.show()
    }
    private fun chooseCashbox(b: ManagerBranchConfig) {
        if (b.cajas.isEmpty()) { toast("La sucursal no tiene cajas"); return }
        AlertDialog.Builder(this).setTitle("Seleccioná una caja")
            .setItems(b.cajas.map { it.nombre }.toTypedArray()) { _, i ->
                val box = b.cajas[i]
                pickTime(box.hora_apertura, "Apertura de ${box.nombre}") { opening ->
                    pickTime(box.hora_cierre, "Cierre de ${box.nombre}") { closing ->
                        if (opening >= closing) toast("La apertura debe ser anterior al cierre")
                        else save(mapOf("sucursal_id" to b.sucursal_id, "caja_id" to box.id, "hora_apertura" to opening, "hora_cierre" to closing))
                    }
                }
            }.show()
    }
    private fun pickTime(value: String, title: String, done: (String) -> Unit) {
        val p = value.split(":")
        TimePickerDialog(this, { _, h, m -> done(String.format(Locale.US, "%02d:%02d", h, m)) },
            p.getOrNull(0)?.toIntOrNull() ?: 9, p.getOrNull(1)?.toIntOrNull() ?: 0, true).apply { setTitle(title); show() }
    }
    private fun save(body: Map<String, Any>) {
        ApiClient.api.updateManagerConfig(token, body).enqueue(object : retrofit2.Callback<ApiMessage> {
            override fun onResponse(call: retrofit2.Call<ApiMessage>, response: retrofit2.Response<ApiMessage>) {
                toast(if (response.isSuccessful) "Configuración guardada" else "No se pudo guardar (${response.code()})")
            }
            override fun onFailure(call: retrofit2.Call<ApiMessage>, t: Throwable) { toast("Error: ${t.message}") }
        })
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}

class ManagerStockActivity : AppCompatActivity() {
    private val token get() = "Token " + (getSharedPreferences("jugueteria", MODE_PRIVATE).getString("token", "") ?: "")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.restore(this)
        setContentView(R.layout.activity_manager_list)
        findViewById<TextView>(R.id.txtManagerListTitle).text = "Stock de productos"
        findViewById<ImageButton>(R.id.btnManagerListBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnManagerDate).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.txtManagerSummary).text = "Consultando productos..."
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerManagerRecords)
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        ApiClient.api.products().enqueue(object : retrofit2.Callback<List<ApiProduct>> {
            override fun onResponse(call: retrofit2.Call<List<ApiProduct>>, response: retrofit2.Response<List<ApiProduct>>) {
                val products = response.body().orEmpty()
                findViewById<TextView>(R.id.txtManagerSummary).text = if (response.isSuccessful) "Productos registrados: ${products.size}" else "No se pudo consultar el stock (${response.code()})"
                recycler.adapter = ManagerRecordAdapter(products.map { ManagerRecord(it.nombre, "Stock: ${it.stock} unidades", java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es","AR")).format(it.precio)) })
            }
            override fun onFailure(call: retrofit2.Call<List<ApiProduct>>, t: Throwable) {
                findViewById<TextView>(R.id.txtManagerSummary).text = "Error de conexión: ${t.message}"
            }
        })
    }
}
