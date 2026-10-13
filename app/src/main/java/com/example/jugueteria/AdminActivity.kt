package com.example.jugueteria

import android.os.Bundle
import android.telecom.Call
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.tracing.perfetto.handshake.protocol.Response
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminActivity : AppCompatActivity() {
    private lateinit var token: EditText
    private fun <T> handle(call: Call<T>, done: (Boolean, T?, String?) -> Unit) = call.enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) = done(response.isSuccessful, response.body(), if (response.isSuccessful) null else "HTTP ${response.code()}: ${response.errorBody()?.string()}")
        override fun onFailure(call: Call<T>, t: Throwable) = done(false, null, t.localizedMessage)
    })
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_admin); ApiClient.restore(this)
        token = findViewById(R.id.edtAdminToken)
        token.setText(getSharedPreferences("jugueteria", MODE_PRIVATE).getString("token", ""))
        token.visibility = android.view.View.GONE
        val username=findViewById<EditText>(R.id.edtAdminUsername); val email=findViewById<EditText>(R.id.edtAdminEmail); val password=findViewById<EditText>(R.id.edtAdminPassword)
        val role=findViewById<Spinner>(R.id.spnAdminRole)
        role.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("gerente","deposito","cajero","administrador"))
        val branch=findViewById<EditText>(R.id.edtBranchName); val address=findViewById<EditText>(R.id.edtBranchAddress); val company=findViewById<EditText>(R.id.edtCompanyName)
        val cashBranch=findViewById<EditText>(R.id.edtCashBranchId); val cashName=findViewById<EditText>(R.id.edtCashName); val cashier=findViewById<EditText>(R.id.edtCashierId); val opening=findViewById<EditText>(R.id.edtCashOpen); val closing=findViewById<EditText>(R.id.edtCashClose)
        val status=findViewById<TextView>(R.id.txtAdminStatus)
        fun authToken() = "Token ${token.text.toString().trim().removePrefix("Token ")}"
        findViewById<Button>(R.id.btnCreateAdminUser).setOnClickListener {
            if (username.text.isBlank() || password.text.isBlank()) { status.text="Completá usuario y contraseña"; return@setOnClickListener }
            handle(ApiClient.api.createUser(authToken(), AdminUserRequest(username.text.toString(), email.text.toString(), password.text.toString(), role.selectedItem.toString()))) { ok, _, err -> status.text=if(ok) "Usuario creado" else err ?: "No se pudo crear el usuario" }
        }
        findViewById<Button>(R.id.btnCreateBranch).setOnClickListener {
            handle(ApiClient.api.createBranch(authToken(), BranchRequest(branch.text.toString(), address.text.toString(), company.text.toString()))) { ok, _, err -> status.text=if(ok) "Sucursal creada" else err ?: "No se pudo crear la sucursal" }
        }
        findViewById<Button>(R.id.btnCreateCash).setOnClickListener {
            val bid=cashBranch.text.toString().toIntOrNull(); if(bid==null){cashBranch.error="ID numérico de sucursal";return@setOnClickListener}
            handle(ApiClient.api.createCashRegister(authToken(), CashRegisterRequest(bid,cashName.text.toString(),cashier.text.toString().toIntOrNull(),opening.text.toString(),closing.text.toString()))) { ok, _, err -> status.text=if(ok) "Caja creada" else err ?: "No se pudo crear la caja" }
        }
        findViewById<Button>(R.id.btnAdminBack).setOnClickListener { finish() }
    }
}
